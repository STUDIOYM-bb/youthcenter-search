package com.themoa.youthcentersearch.common.config;

import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {
    @Bean
    HttpClient youthCenterHttpClient(YouthCenterApiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
