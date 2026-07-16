package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchResult;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;

import java.util.List;

public record PolicyEligibilityEvaluation(
        int policyId,
        boolean passed,
        RegionMatchResult regionMatch,
        ConditionMatchResult ageMatch,
        ConditionMatchResult employmentMatch,
        ConditionMatchResult studentMatch,
        TargetStageMatchResult educationStageMatch,
        EmploymentAudienceMatch employmentAudienceMatch,
        RecommendationTier preliminaryTier,
        List<String> matchedReasons,
        List<String> confirmationReasons,
        String excludedReason
) {
    public PolicyEligibilityEvaluation {
        matchedReasons = matchedReasons == null ? List.of() : List.copyOf(matchedReasons);
        confirmationReasons = confirmationReasons == null ? List.of() : List.copyOf(confirmationReasons);
    }
}
