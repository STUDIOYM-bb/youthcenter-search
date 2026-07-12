package com.themoa.youthcentersearch.youthcenter.client;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.common.util.SecretMasker;
import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyDetailRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyListRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class YouthCenterApiClient {
    private final HttpClient httpClient;
    private final YouthCenterApiProperties properties;

    public YouthCenterApiClient(HttpClient youthCenterHttpClient, YouthCenterApiProperties properties) {
        this.httpClient = youthCenterHttpClient;
        this.properties = properties;
    }

    public ExternalApiResponse fetchCurrentList(YouthPolicyListRequest request) {
        ensureApiKey();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apiKeyNm", properties.getCurrent().getApiKey());
        params.put("pageNum", String.valueOf(request.effectivePageNum(properties)));
        params.put("pageSize", String.valueOf(request.effectivePageSize(properties)));
        params.put("pageType", "1");
        params.put("rtnType", request.effectiveReturnType(properties));
        putIfPresent(params, "plcyKywdNm", request.policyKeywordName());
        putIfPresent(params, "plcyExplnCn", request.policyDescription());
        return get(buildUri(properties.getCurrent().getBaseUrl(), properties.getCurrent().getPath(), params));
    }

    public ExternalApiResponse fetchCurrentDetail(YouthPolicyDetailRequest request) {
        ensureApiKey();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apiKeyNm", properties.getCurrent().getApiKey());
        params.put("pageType", "2");
        params.put("plcyNo", request.policyNumber());
        params.put("rtnType", request.effectiveReturnType(properties));
        return get(buildUri(properties.getCurrent().getBaseUrl(), properties.getCurrent().getPath(), params));
    }

    public ExternalApiResponse fetchLegacyProbe() {
        ensureApiKey();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("openApiVlak", properties.getCurrent().getApiKey());
        params.put("pageIndex", "1");
        params.put("display", "10");
        return get(buildUri(properties.getLegacy().getBaseUrl(), properties.getLegacy().getPath(), params));
    }

    URI buildCurrentListUriForTest(YouthPolicyListRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apiKeyNm", properties.getCurrent().getApiKey());
        params.put("pageNum", String.valueOf(request.effectivePageNum(properties)));
        params.put("pageSize", String.valueOf(request.effectivePageSize(properties)));
        params.put("pageType", "1");
        params.put("rtnType", request.effectiveReturnType(properties));
        putIfPresent(params, "plcyKywdNm", request.policyKeywordName());
        putIfPresent(params, "plcyExplnCn", request.policyDescription());
        return buildUri(properties.getCurrent().getBaseUrl(), properties.getCurrent().getPath(), params);
    }

    URI buildCurrentDetailUriForTest(YouthPolicyDetailRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("apiKeyNm", properties.getCurrent().getApiKey());
        params.put("pageType", "2");
        params.put("plcyNo", request.policyNumber());
        params.put("rtnType", request.effectiveReturnType(properties));
        return buildUri(properties.getCurrent().getBaseUrl(), properties.getCurrent().getPath(), params);
    }

    private ExternalApiResponse get(URI uri) {
        Instant start = Instant.now();
        List<String> redirectHistory = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        URI current = uri;
        HttpResponse<String> response;
        try {
            for (int redirects = 0; ; redirects++) {
                HttpRequest request = HttpRequest.newBuilder(current)
                        .GET()
                        .timeout(properties.getReadTimeout())
                        .header("Accept", "application/json, application/xml, text/xml, */*")
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                Optional<String> location = response.headers().firstValue("location");
                if (!isRedirect(response.statusCode()) || !properties.isFollowRedirects()) {
                    break;
                }
                if (redirects >= properties.getMaximumRedirects()) {
                    warnings.add("理쒕? Redirect ?잛닔瑜?珥덇낵?덉뒿?덈떎.");
                    break;
                }
                URI next = current.resolve(location.orElse(""));
                addRedirectWarnings(current, next, warnings);
                redirectHistory.add(SecretMasker.maskUrl(next.toString()));
                current = next;
            }
        } catch (IOException ex) {
            throw new YouthCenterApiException("?몃? API ?곌껐 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new YouthCenterApiException("?몃? API ?붿껌??以묐떒?섏뿀?듬땲??", ex);
        }

        long elapsed = Duration.between(start, Instant.now()).toMillis();
        String contentType = response.headers().firstValue("content-type").orElse("");
        String location = response.headers().firstValue("location").orElse(null);
        if (isRedirect(response.statusCode()) && location != null) {
            redirectHistory.add(SecretMasker.maskUrl(current.resolve(location).toString()));
        }
        return new ExternalApiResponse(
                response.statusCode(),
                reasonPhrase(response.statusCode()),
                contentType,
                response.headers().map(),
                response.body() == null ? "" : response.body(),
                SecretMasker.maskUrl(uri.toString()),
                SecretMasker.maskUrl(location),
                redirectHistory,
                SecretMasker.maskUrl(current.toString()),
                warnings,
                elapsed
        );
    }

    private void ensureApiKey() {
        if (!StringUtils.hasText(properties.getCurrent().getApiKey())) {
            throw new YouthCenterApiException("?⑦넻泥?뀈 API ?ㅺ? ?ㅼ젙?섏? ?딆븯?듬땲?? application-secret.yml??YOUTH_CENTER_API_KEY瑜??낅젰?섏꽭??");
        }
    }

    private URI buildUri(String baseUrl, String path, Map<String, String> params) {
        String base = trimTrailingSlash(baseUrl) + (path.startsWith("/") ? path : "/" + path);
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> {
            if (StringUtils.hasText(value)) {
                if (!query.isEmpty()) {
                    query.append('&');
                }
                query.append(encodePreservingPercent(key)).append('=').append(encodePreservingPercent(value));
            }
        });
        return URI.create(query.isEmpty() ? base : base + "?" + query);
    }

    private static void putIfPresent(Map<String, String> params, String key, String value) {
        if (StringUtils.hasText(value)) {
            params.put(key, value);
        }
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String encodePreservingPercent(String value) {
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
        return encoded.replaceAll("%25([0-9a-fA-F]{2})", "%$1");
    }

    private static boolean isRedirect(int status) {
        return status >= 300 && status < 400;
    }

    private static void addRedirectWarnings(URI from, URI to, List<String> warnings) {
        if ("https".equalsIgnoreCase(from.getScheme()) && "http".equalsIgnoreCase(to.getScheme())) {
            warnings.add("HTTPS?먯꽌 HTTP濡??대젮媛??Redirect?낅땲??");
        }
        if (!from.getHost().equalsIgnoreCase(to.getHost())) {
            warnings.add("?ㅻⅨ ?몄뒪?몃줈 ?대룞?섎뒗 Redirect?낅땲??");
        }
        if (to.getPort() > 0 && to.getPort() != 443 && to.getPort() != 80) {
            warnings.add("?쇰컲?곸씠吏 ?딆? ?ы듃濡??대룞?섎뒗 Redirect?낅땲?? " + to.getPort());
        }
    }

    private static String reasonPhrase(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }
}
