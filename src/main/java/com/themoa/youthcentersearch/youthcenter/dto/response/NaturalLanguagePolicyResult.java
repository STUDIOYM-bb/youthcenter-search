package com.themoa.youthcentersearch.youthcenter.dto.response;

import java.util.List;

public record NaturalLanguagePolicyResult(
        YouthPolicyView policy,
        double relevanceScore,
        List<String> matchedReasons,
        List<String> missingConditions,
        RegionMatchStatus regionMatchStatus,
        AgeMatchStatus ageMatchStatus
) {
}
