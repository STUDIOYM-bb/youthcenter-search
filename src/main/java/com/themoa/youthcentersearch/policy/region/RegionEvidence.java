package com.themoa.youthcentersearch.policy.region;

public record RegionEvidence(
        RegionEvidenceSource source,
        String rawValue,
        String matchedRegion,
        int confidence
) {
}
