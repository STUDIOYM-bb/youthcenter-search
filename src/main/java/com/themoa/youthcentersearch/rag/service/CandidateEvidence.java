package com.themoa.youthcentersearch.rag.service;

import java.util.List;

/**
 * policyId 기준으로 병합된 후보 증거다.
 *
 * <p>벡터, lexical, 제목 매칭 등 여러 경로에서 같은 정책이 발견될 수 있으므로
 * 검색 경로별 evidence와 병합 후 대표 score를 함께 들고 다닌다.</p>
 */
public record CandidateEvidence(
        Integer policyId,
        List<CandidateSourceEvidence> sourceEvidence,
        double semanticScore,
        double lexicalScore,
        double titleExactScore
) {
    public CandidateEvidence {
        sourceEvidence = sourceEvidence == null ? List.of() : List.copyOf(sourceEvidence);
    }
}
