package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

public enum SearchRegionLevel {
    NATIONWIDE,
    SIDO,
    SIGUNGU;

    public static SearchRegionLevel from(RegionCode region) {
        if (region == null) {
            return null;
        }
        if ("NATIONWIDE".equals(region.getRegionLevel()) || "KR".equals(region.getRegionCode())) {
            return NATIONWIDE;
        }
        if ("PROVINCE".equals(region.getRegionLevel())) {
            return SIDO;
        }
        if ("CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel())) {
            return SIGUNGU;
        }
        return null;
    }
}
