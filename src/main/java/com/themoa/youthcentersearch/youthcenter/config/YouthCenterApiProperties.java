package com.themoa.youthcentersearch.youthcenter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.youth-center")
public class YouthCenterApiProperties {
    private boolean enabled = true;
    @NotNull
    private ApiMode apiMode = ApiMode.CURRENT;
    @Valid
    @NotNull
    private Endpoint current = new Endpoint();
    @Valid
    @NotNull
    private Endpoint legacy = new Endpoint();
    @Min(1)
    private int defaultPageNumber = 1;
    @Min(1)
    private int defaultPageSize = 10;
    @Min(1)
    @Max(100)
    private int maximumPageSize = 100;
    @Min(1)
    @Max(10)
    private int paginationTestMaxPages = 3;
    @NotBlank
    private String defaultReturnType = "json";
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(20);
    private boolean followRedirects = false;
    @Min(0)
    @Max(10)
    private int maximumRedirects = 3;
    @NotNull
    private Duration requestDelay = Duration.ofMillis(300);
    @Min(0)
    @Max(5)
    private int maxRetries = 2;
    @Valid
    @NotNull
    private RawResponse rawResponse = new RawResponse();
    @Valid
    @NotNull
    private Collection collection = new Collection();

    @AssertTrue(message = "defaultReturnType? json ?먮뒗 xml?댁뼱???⑸땲??")
    public boolean isDefaultReturnTypeValid() {
        return "json".equalsIgnoreCase(defaultReturnType) || "xml".equalsIgnoreCase(defaultReturnType);
    }

    @AssertTrue(message = "defaultPageSize??maximumPageSize ?댄븯?댁뼱???⑸땲??")
    public boolean isDefaultPageSizeValid() {
        return defaultPageSize <= maximumPageSize;
    }

    @AssertTrue(message = "timeout? ?묒닔?댁뼱???⑸땲??")
    public boolean isTimeoutValid() {
        return connectTimeout != null && !connectTimeout.isNegative() && !connectTimeout.isZero()
                && readTimeout != null && !readTimeout.isNegative() && !readTimeout.isZero();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ApiMode getApiMode() { return apiMode; }
    public void setApiMode(ApiMode apiMode) { this.apiMode = apiMode; }
    public Endpoint getCurrent() { return current; }
    public void setCurrent(Endpoint current) { this.current = current; }
    public Endpoint getLegacy() { return legacy; }
    public void setLegacy(Endpoint legacy) { this.legacy = legacy; }
    public int getDefaultPageNumber() { return defaultPageNumber; }
    public void setDefaultPageNumber(int defaultPageNumber) { this.defaultPageNumber = defaultPageNumber; }
    public int getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
    public int getMaximumPageSize() { return maximumPageSize; }
    public void setMaximumPageSize(int maximumPageSize) { this.maximumPageSize = maximumPageSize; }
    public int getPaginationTestMaxPages() { return paginationTestMaxPages; }
    public void setPaginationTestMaxPages(int paginationTestMaxPages) { this.paginationTestMaxPages = paginationTestMaxPages; }
    public String getDefaultReturnType() { return defaultReturnType; }
    public void setDefaultReturnType(String defaultReturnType) { this.defaultReturnType = defaultReturnType; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    public boolean isFollowRedirects() { return followRedirects; }
    public void setFollowRedirects(boolean followRedirects) { this.followRedirects = followRedirects; }
    public int getMaximumRedirects() { return maximumRedirects; }
    public void setMaximumRedirects(int maximumRedirects) { this.maximumRedirects = maximumRedirects; }
    public Duration getRequestDelay() { return requestDelay; }
    public void setRequestDelay(Duration requestDelay) { this.requestDelay = requestDelay; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public RawResponse getRawResponse() { return rawResponse; }
    public void setRawResponse(RawResponse rawResponse) { this.rawResponse = rawResponse; }
    public Collection getCollection() { return collection; }
    public void setCollection(Collection collection) { this.collection = collection; }

    public static class Endpoint {
        @NotBlank
        private String baseUrl = "https://www.youthcenter.go.kr";
        @NotBlank
        private String path = "/go/ythip/getPlcy";
        private String apiKey = "";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class RawResponse {
        private boolean saveEnabled = true;
        @NotBlank
        private String directory = "build/api-responses";
        @Min(200)
        @Max(20000)
        private int previewLength = 2000;

        public boolean isSaveEnabled() { return saveEnabled; }
        public void setSaveEnabled(boolean saveEnabled) { this.saveEnabled = saveEnabled; }
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        public int getPreviewLength() { return previewLength; }
        public void setPreviewLength(int previewLength) { this.previewLength = previewLength; }
    }

    public static class Collection {
        private boolean enabled = false;
        @Min(1)
        @Max(100)
        private int pageSize = 100;
        @Min(1)
        private int maxPages = 1000;
        @NotNull
        private Duration requestDelay = Duration.ofMillis(300);
        @NotNull
        private Duration detailRequestDelay = Duration.ofMillis(200);
        @Min(0)
        @Max(10)
        private int maxRetries = 3;
        @NotNull
        private Duration connectTimeout = Duration.ofSeconds(5);
        @NotNull
        private Duration readTimeout = Duration.ofSeconds(20);
        @NotNull
        private DetailFetchMode detailFetchMode = DetailFetchMode.MISSING_ONLY;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getMaxPages() { return maxPages; }
        public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
        public Duration getRequestDelay() { return requestDelay; }
        public void setRequestDelay(Duration requestDelay) { this.requestDelay = requestDelay; }
        public Duration getDetailRequestDelay() { return detailRequestDelay; }
        public void setDetailRequestDelay(Duration detailRequestDelay) { this.detailRequestDelay = detailRequestDelay; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
        public DetailFetchMode getDetailFetchMode() { return detailFetchMode; }
        public void setDetailFetchMode(DetailFetchMode detailFetchMode) { this.detailFetchMode = detailFetchMode; }
    }
}
