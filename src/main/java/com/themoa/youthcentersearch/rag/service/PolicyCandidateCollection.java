package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 자연어 검색 후보 수집 결과다.
 *
 * <p>정책 엔티티 목록과 함께 기존 검색 서비스가 사용하던 score/source map을 유지한다.
 * 동시에 Explain 개선을 위해 원천별 rank/score evidence도 제공한다.</p>
 */
public record PolicyCandidateCollection(
        List<Policy> policies,
        Map<Integer, CandidateEvidence> evidenceByPolicyId,
        Map<Integer, Double> semanticScores,
        Map<Integer, Double> lexicalScores,
        Map<Integer, Double> titleExactScores,
        Map<Integer, Set<CandidateSource>> candidateSources,
        CandidateCollectionMetrics metrics,
        boolean retried,
        boolean mysqlFallbackUsed,
        String fallbackReason
) {
}
