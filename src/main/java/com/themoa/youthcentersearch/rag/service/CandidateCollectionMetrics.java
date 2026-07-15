package com.themoa.youthcentersearch.rag.service;

/**
 * 후보 수집 단계에서만 알 수 있는 검색량/중복/지역 풀 지표다.
 *
 * <p>Diagnostics 조립 시 서비스 본문에 90개가 넘는 생성자 인자를 직접 나열하지 않도록
 * 후보 수집 관련 값을 한 객체로 묶는다.</p>
 */
public record CandidateCollectionMetrics(
        int vectorCandidateCount,
        int vectorOriginalCandidateCount,
        int vectorIntentCandidateCount,
        int vectorExpandedCandidateCount,
        int vectorCategoryCandidateCount,
        int vectorNormalizedCandidateCount,
        int lexicalCandidateCount,
        int mysqlTitleCandidateCount,
        int mysqlKeywordCandidateCount,
        int mysqlSummaryCandidateCount,
        int mysqlCategoryCandidateCount,
        int loadedPolicyCount,
        int duplicateCandidateCount,
        int fallbackAddedCount,
        int regionPoolTotal,
        int regionPoolExactSigungu,
        int regionPoolParentSido,
        int regionPoolNationwide,
        int regionPoolMultiple
) {
}
