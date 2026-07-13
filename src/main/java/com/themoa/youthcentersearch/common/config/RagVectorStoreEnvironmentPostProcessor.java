package com.themoa.youthcentersearch.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.Map;

public class RagVectorStoreEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String VECTOR_STORE_TYPE = "spring.ai.vectorstore.type";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.hasText(environment.getProperty(VECTOR_STORE_TYPE))) {
            return;
        }
        boolean ragEnabled = Boolean.parseBoolean(environment.getProperty("RAG_ENABLED",
                environment.getProperty("app.rag.enabled", "false")));
        environment.getPropertySources().addFirst(new MapPropertySource("ragVectorStoreType",
                Map.of(VECTOR_STORE_TYPE, ragEnabled ? "qdrant" : "none")));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
