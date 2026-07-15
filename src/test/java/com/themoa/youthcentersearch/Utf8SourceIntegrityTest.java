package com.themoa.youthcentersearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소스와 리소스에 깨진 한글이 섞이는 일을 막는 회귀 테스트다.
 * 정상적인 영문 물음표나 정규식은 검사하지 않고, UTF-8 디코딩 실패 흔적과 흔한 mojibake 조각만 확인한다.
 */
class Utf8SourceIntegrityTest {
    private static final List<String> BROKEN_FRAGMENTS = List.of(
            "\uFFFD",
            "\u00EC\u201E", "\u00EC\u009D", "\u00ED\u2022",
            "\u00EA\u00B0", "\u00EB\u008F", "\u00EC\u00A7",
            "\u9858", "\uF9E3?", "?\u2466", "\u7531\u044A", "\u5A9B\u0080"
    );

    @Test
    void sourceFilesDoNotContainBrokenKoreanFragments() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Stream.of("src/main/java", "src/main/resources", "src/test/java")
                .map(Path::of)
                .filter(Files::exists)
                .flatMap(this::walk)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(this::isTextFile)
                    .toList();
        }

        List<String> brokenFiles = files.stream()
                .filter(this::containsBrokenFragment)
                .map(Path::toString)
                .toList();

        assertThat(brokenFiles).isEmpty();
    }

    private Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException("소스 경로를 읽을 수 없습니다: " + root, e);
        }
    }

    private boolean isTextFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".java")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".properties")
                || name.endsWith(".html")
                || name.endsWith(".js")
                || name.endsWith(".css")
                || name.endsWith(".md")
                || name.endsWith(".json");
    }

    private boolean containsBrokenFragment(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return BROKEN_FRAGMENTS.stream().anyMatch(text::contains);
        } catch (IOException e) {
            throw new IllegalStateException("파일을 읽을 수 없습니다: " + path, e);
        }
    }
}
