package com.themoa.youthcentersearch.policy.region;

public record PolicyRegionClassificationEvidence(
        RegionEvidenceSource source,
        String rawValue,
        String matchedRegion,
        int confidence,
        String reason,
        PolicyRegionMentionRole role
) {
    public PolicyRegionClassificationEvidence(RegionEvidenceSource source, String rawValue, String matchedRegion,
                                              int confidence, String reason) {
        this(source, rawValue, matchedRegion, confidence, reason, PolicyRegionMentionRole.UNKNOWN);
    }
}
