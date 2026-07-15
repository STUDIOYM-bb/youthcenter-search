package com.themoa.youthcentersearch.rag.dto;

/**
 * 최종 추천에서 자격 확실성을 표현하는 단계다.
 * 점수 공식은 그대로 두고, 같은 검색 결과 내에서 PRIMARY를 NEEDS_CONFIRMATION보다 먼저 배치한다.
 */
public enum RecommendationTier {
    PRIMARY,
    NEEDS_CONFIRMATION,
    MISMATCH
}
