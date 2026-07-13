package com.themoa.youthcentersearch.region.sgis.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SgisAuthenticationResponse(
        String id,
        String errCd,
        String errMsg,
        Result result
) {
    @JsonIgnore
    public boolean success() {
        return errCd == null || "0".equals(errCd);
    }

    @JsonIgnore
    public String token() {
        return result == null ? null : result.accessToken();
    }

    @JsonIgnore
    public String timeout() {
        return result == null ? null : result.accessTimeout();
    }

    public record Result(
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("accessTimeout") String accessTimeout
    ) {
    }
}
