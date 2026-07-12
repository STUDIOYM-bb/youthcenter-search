package com.themoa.youthcentersearch.youthcenter.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.natural-language-search")
public class NaturalLanguageSearchProperties {
    private boolean enabled = true;
    private boolean openaiEnabled = true;
    @Min(1)
    @Max(100)
    private int pageSize = 20;
    @Min(1)
    @Max(20)
    private int maxApiRequests = 6;
    @Min(1)
    @Max(10)
    private int maxPagesPerRequest = 2;
    @Min(1)
    @Max(5)
    private int maximumKeywordsPerRequest = 2;
    private boolean descriptionFilterEnabled = false;
    @Min(1)
    @Max(50)
    private int descriptionFilterMaxLength = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOpenaiEnabled() {
        return openaiEnabled;
    }

    public void setOpenaiEnabled(boolean openaiEnabled) {
        this.openaiEnabled = openaiEnabled;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxApiRequests() {
        return maxApiRequests;
    }

    public void setMaxApiRequests(int maxApiRequests) {
        this.maxApiRequests = maxApiRequests;
    }

    public int getMaxPagesPerRequest() {
        return maxPagesPerRequest;
    }

    public void setMaxPagesPerRequest(int maxPagesPerRequest) {
        this.maxPagesPerRequest = maxPagesPerRequest;
    }

    public int getMaximumKeywordsPerRequest() {
        return maximumKeywordsPerRequest;
    }

    public void setMaximumKeywordsPerRequest(int maximumKeywordsPerRequest) {
        this.maximumKeywordsPerRequest = maximumKeywordsPerRequest;
    }

    public boolean isDescriptionFilterEnabled() {
        return descriptionFilterEnabled;
    }

    public void setDescriptionFilterEnabled(boolean descriptionFilterEnabled) {
        this.descriptionFilterEnabled = descriptionFilterEnabled;
    }

    public int getDescriptionFilterMaxLength() {
        return descriptionFilterMaxLength;
    }

    public void setDescriptionFilterMaxLength(int descriptionFilterMaxLength) {
        this.descriptionFilterMaxLength = descriptionFilterMaxLength;
    }
}
