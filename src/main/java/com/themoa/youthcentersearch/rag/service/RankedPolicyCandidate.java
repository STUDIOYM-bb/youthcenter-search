package com.themoa.youthcentersearch.rag.service;

public record RankedPolicyCandidate(
        EvaluatedPolicyCandidate candidate,
        PolicyRankingEvaluation ranking
) {
}
