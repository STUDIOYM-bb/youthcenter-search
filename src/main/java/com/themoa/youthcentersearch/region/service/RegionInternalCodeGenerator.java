package com.themoa.youthcentersearch.region.service;

import org.springframework.stereotype.Component;

@Component
public class RegionInternalCodeGenerator {
    public String nationwide() {
        return "KR";
    }

    public String province(String provinceName) {
        return "P:" + normalize(provinceName);
    }

    public String municipality(String provinceName, String municipalityName) {
        return "M:" + normalize(provinceName) + ":" + normalize(municipalityName);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }
}
