package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record PolicyRegionClassificationResult(
        RegionScope scope,
        Set<RegionCode> regions,
        double confidence,
        List<PolicyRegionClassificationEvidence> evidence,
        List<PolicyRegionClassificationEvidence> conflictingEvidence,
        boolean needsReview,
        String classifierVersion
) {
    public static final String VERSION = "policy-region-v3";

    public PolicyRegionResolution toResolution() {
        Set<RegionCode> safeRegions = regions == null ? Set.of() : regions;
        return new PolicyRegionResolution(scope,
                safeRegions.stream().map(RegionCode::getId).collect(Collectors.toCollection(LinkedHashSet::new)),
                safeRegions.stream().map(RegionCode::getRegionCode).collect(Collectors.toCollection(LinkedHashSet::new)),
                safeRegions.stream().map(RegionCode::displayName).collect(Collectors.toCollection(LinkedHashSet::new)),
                evidence.stream()
                        .map(item -> new RegionEvidence(item.source(), item.rawValue(), item.matchedRegion(), item.confidence()))
                        .toList(),
                scope == RegionScope.NATIONWIDE,
                needsReview);
    }
}
