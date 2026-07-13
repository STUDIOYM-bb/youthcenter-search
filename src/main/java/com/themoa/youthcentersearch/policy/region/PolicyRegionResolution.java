package com.themoa.youthcentersearch.policy.region;

import java.util.List;
import java.util.Set;

public record PolicyRegionResolution(
        RegionScope scope,
        Set<Integer> regionIds,
        Set<String> regionCodes,
        Set<String> regionNames,
        List<RegionEvidence> evidence,
        boolean nationwideExplicitlyConfirmed,
        boolean needsReview
) {
    public static PolicyRegionResolution unknown(List<RegionEvidence> evidence) {
        return new PolicyRegionResolution(RegionScope.UNKNOWN, Set.of(), Set.of(), Set.of(), evidence, false, true);
    }
}
