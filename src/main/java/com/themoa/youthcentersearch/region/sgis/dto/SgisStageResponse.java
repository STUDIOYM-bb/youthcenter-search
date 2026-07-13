package com.themoa.youthcentersearch.region.sgis.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public record SgisStageResponse(
        String id,
        String errCd,
        String errMsg,
        List<SgisRegionItem> result
) {
    @JsonIgnore
    public boolean success() {
        return errCd == null || "0".equals(errCd);
    }

    @JsonIgnore
    public List<SgisRegionItem> safeResult() {
        return result == null ? List.of() : result;
    }
}
