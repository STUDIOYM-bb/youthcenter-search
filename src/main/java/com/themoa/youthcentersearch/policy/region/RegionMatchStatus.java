package com.themoa.youthcentersearch.policy.region;

public enum RegionMatchStatus {
    EXACT_CITY("수원시 정책"),
    EXACT_DISTRICT("구 단위 정책"),
    PROVINCE_MATCH("광역 지역 정책"),
    NATIONWIDE("전국 정책"),
    MULTIPLE_REGION_MATCH("여러 지역 중 일치"),
    UNKNOWN("지역 확인 필요"),
    NOT_MATCHED("다른 지역 정책");

    private final String description;

    RegionMatchStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
