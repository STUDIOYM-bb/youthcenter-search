package com.themoa.youthcentersearch.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSecretConfigurationStatusTest {
    @Test
    void reportsDisabledDefaultsWithoutKeys() {
        LocalSecretConfigurationStatus status = new LocalSecretConfigurationStatus("", "", "none", "none", false);

        assertThat(status.youthCenterApiKeyConfigured()).isFalse();
        assertThat(status.openAiApiKeyConfigured()).isFalse();
        assertThat(status.springAiChatModel()).isEqualTo("none");
        assertThat(status.springAiEmbeddingModel()).isEqualTo("none");
        assertThat(status.chatModelAvailable()).isFalse();
        assertThat(status.embeddingModelAvailable()).isFalse();
        assertThat(status.ragEnabled()).isFalse();
    }

    @Test
    void reportsConfiguredWhenKeysAndOpenAiModesAreProvided() {
        LocalSecretConfigurationStatus status = new LocalSecretConfigurationStatus(
                "youth-test-key", "openai-test-key", "openai", "openai", true);

        assertThat(status.youthCenterApiKeyConfigured()).isTrue();
        assertThat(status.openAiApiKeyConfigured()).isTrue();
        assertThat(status.chatModelAvailable()).isTrue();
        assertThat(status.embeddingModelAvailable()).isTrue();
        assertThat(status.ragEnabled()).isTrue();
    }

    @Test
    void usesProjectRootRelativeSecretPath() {
        assertThat(LocalSecretConfigurationStatus.SECRET_CONFIG_PATH).isEqualTo("./config/application-secret.yml");
    }
}
