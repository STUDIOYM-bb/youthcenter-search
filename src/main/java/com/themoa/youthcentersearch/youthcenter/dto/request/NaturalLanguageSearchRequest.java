package com.themoa.youthcentersearch.youthcenter.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalLanguageSearchRequest(
        @NotBlank
        @Size(min = 2, max = 500)
        String query,
        @Min(1)
        @Max(100)
        Integer resultSize
) {
    public int effectiveResultSize() {
        return resultSize == null ? 20 : resultSize;
    }
}
