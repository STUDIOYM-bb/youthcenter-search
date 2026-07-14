package com.themoa.youthcentersearch.policy.region;

public record RegionEligiblePolicyCandidate(
        Integer policyId,
        RegionCompatibility compatibility
) {
}
