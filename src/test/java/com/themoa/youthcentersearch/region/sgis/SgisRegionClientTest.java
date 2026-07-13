package com.themoa.youthcentersearch.region.sgis;

import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SgisRegionClientTest {
    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void authenticatesAndFetchesProvinceAndChildrenWithCachedToken() throws Exception {
        server.enqueue(json("""
                {"errCd":"0","errMsg":"Success","result":{"accessToken":"token-for-test","accessTimeout":"3600"}}
                """));
        server.enqueue(json("""
                {"errCd":"0","errMsg":"Success","result":[{"cd":"47","addr_name":"경상북도","full_addr":"경상북도"}]}
                """));
        server.enqueue(json("""
                {"errCd":"0","errMsg":"Success","result":[{"cd":"47850","addr_name":"칠곡군","full_addr":"경상북도 칠곡군"}]}
                """));
        var properties = properties();
        var retryExecutor = new SgisRetryExecutor(properties);
        var authClient = new SgisAuthenticationClient(properties, retryExecutor);
        var tokenManager = new SgisAccessTokenManager(authClient);
        var regionClient = new SgisRegionClient(properties, tokenManager, retryExecutor);

        var provinces = regionClient.fetchProvinces();
        var children = regionClient.fetchChildren("47");

        assertThat(provinces).extracting("cd").containsExactly("47");
        assertThat(children).extracting("addrName").containsExactly("칠곡군");
        assertThat(server.takeRequest().getPath()).contains("/OpenAPI3/auth/authentication.json");
        assertThat(server.takeRequest().getPath()).contains("/OpenAPI3/addr/stage.json");
        assertThat(server.takeRequest().getPath()).contains("cd=47");
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void throwsOnSgisErrorCode() {
        server.enqueue(json("""
                {"errCd":"0","errMsg":"Success","result":{"accessToken":"token-for-test","accessTimeout":"3600"}}
                """));
        server.enqueue(json("""
                {"errCd":"-1","errMsg":"SGIS error","result":[]}
                """));
        var properties = properties();
        var retryExecutor = new SgisRetryExecutor(properties);
        var regionClient = new SgisRegionClient(properties,
                new SgisAccessTokenManager(new SgisAuthenticationClient(properties, retryExecutor)), retryExecutor);

        assertThatThrownBy(regionClient::fetchProvinces)
                .isInstanceOf(SgisApiException.class)
                .hasMessageContaining("SGIS 지역 조회");
    }

    private RegionSyncProperties properties() {
        return new RegionSyncProperties(true, false, "0 0 4 1 * *",
                Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1), 3,
                new RegionSyncProperties.Sgis(server.url("").toString(), "consumer-key", "consumer-secret"));
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
