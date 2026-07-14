package com.themoa.youthcentersearch.policy.region;

public enum RegionCompatibility {
    EXACT_SIGUNGU("정확한 시·군·자치구 정책"),
    EXACT_SIDO("시·도 전체 정책"),
    PARENT_SIDO("상위 시·도 전체 정책"),
    NATIONWIDE("전국 정책"),
    MULTIPLE_REGION_MATCH("복수 지역 중 사용자 지역 포함"),
    UNKNOWN("지역 확인 필요"),
    NOT_MATCHED("다른 지역 정책");

    private final String label;

    RegionCompatibility(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
