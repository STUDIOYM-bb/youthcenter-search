package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.CandidateSource;

/**
 * 후보 정책이 어떤 검색 경로에서 발견됐는지 보존하는 원천별 증거다.
 *
 * <p>최종 랭킹 순위와 벡터/BM25 원천 순위는 서로 다른 값이다. Explain에서
 * 최종 순위를 벡터 순위처럼 재사용하지 않도록 후보 수집 시점의 rank와 score를 별도 보관한다.</p>
 */
public record CandidateSourceEvidence(
        CandidateSource source,
        Integer sourceRank,
        Double rawScore,
        Double normalizedScore,
        String queryVariant
) {
}
