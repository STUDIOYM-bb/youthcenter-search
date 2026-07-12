package com.themoa.youthcentersearch.youthcenter.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record YouthCenterFilterProbeRequest(
        @NotNull
        FilterProbeType filterType,
        @Size(max = 100)
        String value,
        @Min(1)
        @Max(100)
        Integer pageSize
) {
    public int effectivePageSize() {
        return pageSize == null ? 10 : pageSize;
    }
}
