package com.themoa.youthcentersearch.youthcenter.util;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class RawResponseStorage {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String SAFE_FILENAME_PATTERN = "[^\\p{IsAlphabetic}\\p{IsDigit}._-]";

    private final YouthCenterApiProperties properties;

    public RawResponseStorage(YouthCenterApiProperties properties) {
        this.properties = properties;
    }

    public String save(String prefix, ResponseType type, String body) {
        if (!properties.getRawResponse().isSaveEnabled()) {
            return null;
        }
        try {
            Path base = Path.of(properties.getRawResponse().getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(base);

            String extension = switch (type) {
                case JSON -> "json";
                case XML -> "xml";
                case HTML -> "html";
                default -> "txt";
            };
            String safePrefix = sanitizePrefix(prefix);
            Path target = base.resolve(safePrefix + "-" + LocalDateTime.now().format(FORMATTER) + "." + extension)
                    .normalize();
            if (!target.startsWith(base)) {
                throw new YouthCenterApiException("원본 응답 파일은 지정된 디렉터리 내부에만 저장할 수 있습니다.");
            }

            Files.writeString(target, body == null ? "" : body, StandardCharsets.UTF_8);
            return Path.of("").toAbsolutePath().normalize().relativize(target).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new YouthCenterApiException("원본 응답 파일 저장에 실패했습니다.", ex);
        }
    }

    private String sanitizePrefix(String prefix) {
        String cleaned = (prefix == null || prefix.isBlank() ? "response" : prefix)
                .replaceAll(SAFE_FILENAME_PATTERN, "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (cleaned.isBlank()) {
            return "response";
        }
        return cleaned.length() > 80 ? cleaned.substring(0, 80) : cleaned;
    }
}
