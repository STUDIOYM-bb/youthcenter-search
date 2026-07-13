package com.themoa.youthcentersearch.region.sgis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SgisRegionItem(
        String cd,
        @JsonProperty("addr_name") String addrName,
        @JsonProperty("full_addr") String fullAddr
) {
}
