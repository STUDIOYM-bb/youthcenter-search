package com.themoa.youthcentersearch.policy.region;

public record RegionCandidate(
        Integer regionId,
        String province,
        String city,
        String displayName,
        String matchedAlias,
        RegionTextMatchType matchType
) {
}
