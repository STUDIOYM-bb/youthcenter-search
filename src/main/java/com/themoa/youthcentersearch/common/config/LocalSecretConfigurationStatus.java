package com.themoa.youthcentersearch.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalSecretConfigurationStatus implements ApplicationRunner {
    public static final String SECRET_CONFIG_PATH = "./config/application-secret.yml";

    private static final Logger log = LoggerFactory.getLogger(LocalSecretConfigurationStatus.class);

    private final String youthCenterApiKey;
    private final String openAiApiKey;
    private final String springAiChatModel;
    private final String springAiEmbeddingModel;
    private final boolean ragEnabled;

    public LocalSecretConfigurationStatus(@Value("${app.youth-center.current.api-key:}") String youthCenterApiKey,
                                          @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
                                          @Value("${spring.ai.model.chat:none}") String springAiChatModel,
                                          @Value("${spring.ai.model.embedding:none}") String springAiEmbeddingModel,
                                          @Value("${app.rag.enabled:false}") boolean ragEnabled) {
        this.youthCenterApiKey = youthCenterApiKey;
        this.openAiApiKey = openAiApiKey;
        this.springAiChatModel = springAiChatModel;
        this.springAiEmbeddingModel = springAiEmbeddingModel;
        this.ragEnabled = ragEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (secretConfigFileFound()) {
            log.info("External secret configuration: FOUND");
            log.info("External secret configuration path: {}", SECRET_CONFIG_PATH);
        } else {
            log.info("External secret configuration: NOT FOUND");
            log.info("Run scripts/setup-local.ps1 or copy the example file.");
        }
    }

    public boolean secretConfigFileFound() {
        return Files.isRegularFile(Path.of("config", "application-secret.yml"));
    }

    public boolean youthCenterApiKeyConfigured() {
        return StringUtils.hasText(youthCenterApiKey);
    }

    public boolean openAiApiKeyConfigured() {
        return StringUtils.hasText(openAiApiKey);
    }

    public String springAiChatModel() {
        return normalizeModel(springAiChatModel);
    }

    public String springAiEmbeddingModel() {
        return normalizeModel(springAiEmbeddingModel);
    }

    public boolean chatModelAvailable() {
        return openAiApiKeyConfigured() && "openai".equals(springAiChatModel());
    }

    public boolean embeddingModelAvailable() {
        return openAiApiKeyConfigured() && "openai".equals(springAiEmbeddingModel());
    }

    public boolean ragEnabled() {
        return ragEnabled;
    }

    private String normalizeModel(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "none";
    }
}
