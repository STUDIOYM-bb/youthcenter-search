package com.themoa.youthcentersearch.region.sgis;

import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.dto.SgisAuthenticationResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SgisAuthenticationClient {
    private final RegionSyncProperties properties;
    private final SgisRetryExecutor retryExecutor;

    public SgisAuthenticationClient(RegionSyncProperties properties, SgisRetryExecutor retryExecutor) {
        this.properties = properties;
        this.retryExecutor = retryExecutor;
    }

    public SgisAuthenticationResponse authenticate() {
        if (!properties.credentialsConfigured()) {
            throw new SgisApiException("SGIS 인증 정보가 설정되지 않았습니다.");
        }
        RestClient client = restClient();
        SgisAuthenticationResponse response = retryExecutor.execute(() -> client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/OpenAPI3/auth/authentication.json")
                            .queryParam("consumer_key", properties.sgis().consumerKey())
                            .queryParam("consumer_secret", properties.sgis().consumerSecret())
                            .build())
                    .retrieve()
                    .body(SgisAuthenticationResponse.class));
        if (response == null || !response.success() || response.token() == null || response.token().isBlank()) {
            throw new SgisApiException("SGIS 인증에 실패했습니다. errCd=" + safeCode(response));
        }
        return response;
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.sgis().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private String safeCode(SgisAuthenticationResponse response) {
        return response == null ? "NO_RESPONSE" : String.valueOf(response.errCd());
    }
}
