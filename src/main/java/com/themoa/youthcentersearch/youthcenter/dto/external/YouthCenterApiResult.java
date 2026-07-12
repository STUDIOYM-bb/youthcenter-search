package com.themoa.youthcentersearch.youthcenter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthCenterApiResult(
        @JsonProperty("pagging")
        YouthCenterPaging pagging,
        YouthCenterPaging paging,
        List<YouthCenterPolicyRaw> youthPolicyList,
        YouthCenterPolicyRaw youthPolicy
) {
    public YouthCenterPaging effectivePaging() {
        return pagging != null ? pagging : paging;
    }
}
