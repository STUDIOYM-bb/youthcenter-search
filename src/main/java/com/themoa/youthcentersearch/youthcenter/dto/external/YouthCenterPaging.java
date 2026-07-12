package com.themoa.youthcentersearch.youthcenter.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthCenterPaging(
        Integer totCount,
        Integer pageNum,
        Integer pageSize
) {
}
