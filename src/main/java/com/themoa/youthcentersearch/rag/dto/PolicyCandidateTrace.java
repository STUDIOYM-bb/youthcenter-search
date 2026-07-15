package com.themoa.youthcentersearch.rag.dto;

import java.util.Set;

/**
 * 검색 후보가 생성되고 필터와 랭킹을 통과하는 동안 남긴 실제 추적 정보다.
 * Explain 화면은 검색 종료 후 점수를 다시 추정하지 않고 이 형태의 값을 반환하는 방향으로 확장한다.
 */
public record PolicyCandidateTrace(
        Integer policyId,
        Set<CandidateSource> sources,
        Integer bm25Rank,
        Double bm25Score,
        Integer vectorNormalizedRank,
        Double vectorNormalizedScore,
        Integer vectorIntentRank,
        Double vectorIntentScore,
        PolicyDomainClassification domain,
        boolean regionPassed,
        boolean agePassed,
        boolean studentPassed,
        boolean employmentPassed,
        boolean preferencePassed,
        String excludedStage,
        String excludedReason,
        Double finalScore,
        Integer finalRank
) {
    public PolicyCandidateTrace {
        sources = sources == null ? Set.of() : Set.copyOf(sources);
    }
}
