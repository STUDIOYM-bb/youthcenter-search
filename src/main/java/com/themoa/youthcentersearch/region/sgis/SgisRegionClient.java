package com.themoa.youthcentersearch.region.sgis;

import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.dto.SgisRegionItem;
import com.themoa.youthcentersearch.region.sgis.dto.SgisStageResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class SgisRegionClient {
    private final RegionSyncProperties properties;
    private final SgisAccessTokenManager tokenManager;
    private final SgisRetryExecutor retryExecutor;

    public SgisRegionClient(RegionSyncProperties properties, SgisAccessTokenManager tokenManager, SgisRetryExecutor retryExecutor) {
        this.properties = properties;
        this.tokenManager = tokenManager;
        this.retryExecutor = retryExecutor;
    }

    public List<SgisRegionItem> fetchProvinces() {
        return fetchStage(null);
    }

    public List<SgisRegionItem> fetchChildren(String provinceCode) {
        return fetchStage(provinceCode);
    }

    private List<SgisRegionItem> fetchStage(String cd) {
        RestClient client = restClient();
        SgisStageResponse response = retryExecutor.execute(() -> request(client, cd, false));
        if (response == null || !response.success()) {
            String code = response == null ? "NO_RESPONSE" : String.valueOf(response.errCd());
            if (isAuthError(code)) {
                tokenManager.invalidate();
                response = retryExecutor.execute(() -> request(client, cd, true));
            }
        }
        if (response == null || !response.success()) {
            throw new SgisApiException("SGIS 지역 조회에 실패했습니다. errCd=" + (response == null ? "NO_RESPONSE" : response.errCd()));
        }
        return response.safeResult();
    }

    private SgisStageResponse request(RestClient client, String cd, boolean refreshed) {
        try {
            String token = tokenManager.accessToken();
            return client.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/OpenAPI3/addr/stage.json")
                                .queryParam("accessToken", token)
                                .queryParam("pg_yn", "0");
                        if (StringUtils.hasText(cd)) {
                            builder.queryParam("cd", cd);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(SgisStageResponse.class);
        } catch (RuntimeException ex) {
            if (!refreshed) {
                tokenManager.invalidate();
            }
            throw new SgisApiException("SGIS 지역 조회 요청에 실패했습니다.", ex);
        }
    }

    private boolean isAuthError(String code) {
        return "100".equals(code) || "401".equals(code) || "-401".equals(code) || "INVALID_TOKEN".equals(code);
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
}
