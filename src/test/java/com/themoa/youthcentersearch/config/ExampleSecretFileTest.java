package com.themoa.youthcentersearch.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleSecretFileTest {
    @Test
    void exampleFilesDoNotContainRealKeyLikeValues() throws Exception {
        String secretExample = Files.readString(Path.of("config/application-secret.example.yml"));
        String envExample = Files.readString(Path.of(".env.example"));

        assertThat(secretExample).contains("YOUTH_CENTER_API_KEY: \"\"");
        assertThat(secretExample).contains("OPENAI_API_KEY: \"\"");
        assertThat(secretExample).contains("ADMIN_API_KEY: \"change_me\"");
        assertThat(secretExample).doesNotContain("sk-", "sk-proj-", "Bearer ");
        assertThat(envExample).doesNotContain("YOUTH_CENTER_API_KEY", "OPENAI_API_KEY", "ADMIN_API_KEY", "sk-");
    }
}
