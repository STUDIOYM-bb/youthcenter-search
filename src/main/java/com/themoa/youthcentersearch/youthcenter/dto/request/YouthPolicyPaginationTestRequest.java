package com.themoa.youthcentersearch.youthcenter.dto.request;

import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record YouthPolicyPaginationTestRequest(
        @Min(1) Integer startPage,
        @Min(1) @Max(100) Integer pageSize,
        @Min(1) @Max(10) Integer maxPages,
        String returnType,
        String policyKeywordName,
        String policyDescription
) {
    public int effectiveStartPage(YouthCenterApiProperties properties) {
        return startPage == null ? properties.getDefaultPageNumber() : startPage;
    }

    public int effectivePageSize(YouthCenterApiProperties properties) {
        return Math.min(pageSize == null ? properties.getDefaultPageSize() : pageSize, properties.getMaximumPageSize());
    }

    public int effectiveMaxPages(YouthCenterApiProperties properties) {
        return Math.min(maxPages == null ? properties.getPaginationTestMaxPages() : maxPages, properties.getPaginationTestMaxPages());
    }

    public String effectiveReturnType(YouthCenterApiProperties properties) {
        return returnType == null || returnType.isBlank() ? properties.getDefaultReturnType() : returnType.toLowerCase();
    }
}
