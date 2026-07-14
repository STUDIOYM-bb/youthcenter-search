package com.themoa.youthcentersearch.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolicySearchRequest(
        @NotBlank
        @Size(min = 2, max = 500)
        String query,
        @Min(1)
        @Max(100)
        Integer resultSize,
        @Min(0)
        Integer page,
        @Min(1)
        @Max(100)
        Integer size
) {
    public PolicySearchRequest(String query, Integer resultSize) {
        this(query, resultSize, null, null);
    }

    public int resolvedPage() {
        return page == null ? 0 : Math.max(0, page);
    }

    public int resolvedSize(int defaultSize) {
        if (size != null) {
            return Math.max(1, Math.min(100, size));
        }
        if (resultSize != null) {
            return Math.max(1, Math.min(100, resultSize));
        }
        return Math.max(1, Math.min(100, defaultSize));
    }
}
