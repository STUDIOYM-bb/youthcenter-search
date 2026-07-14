package com.themoa.youthcentersearch.policy.region;

public record PolicyRegionClassificationEvidence(
        RegionEvidenceSource source,
        String rawValue,
        String matchedRegion,
        int confidence,
        String reason
) {
}
