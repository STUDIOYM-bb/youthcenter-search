package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;

public record EvaluatedPolicyCandidate(
        Policy policy,
        CandidateEvidence candidateEvidence,
        PolicyEligibilityEvaluation eligibility
) {
}
