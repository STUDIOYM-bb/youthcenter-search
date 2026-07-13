package com.themoa.youthcentersearch.region.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.region-sync")
public record RegionSyncProperties(
        boolean enabled,
        boolean syncOnStartup,
        String cron,
        Duration requestDelay,
        Duration connectTimeout,
        Duration readTimeout,
        int maxRetries,
        Sgis sgis
) {
    public RegionSyncProperties {
        requestDelay = requestDelay == null ? Duration.ofMillis(100) : requestDelay;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
        maxRetries = maxRetries <= 0 ? 3 : maxRetries;
        sgis = sgis == null ? new Sgis("", "", "") : sgis;
    }

    public boolean credentialsConfigured() {
        return sgis != null && hasText(sgis.consumerKey()) && hasText(sgis.consumerSecret());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Sgis(
            String baseUrl,
            String consumerKey,
            String consumerSecret
    ) {
        public Sgis {
            baseUrl = hasText(baseUrl) ? baseUrl : "https://sgisapi.mods.go.kr";
            consumerKey = consumerKey == null ? "" : consumerKey;
            consumerSecret = consumerSecret == null ? "" : consumerSecret;
        }
    }
}
