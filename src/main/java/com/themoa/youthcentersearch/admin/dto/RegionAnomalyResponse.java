package com.themoa.youthcentersearch.admin.dto;

import com.themoa.youthcentersearch.policy.region.RegionEvidence;

import java.util.List;

public record RegionAnomalyResponse(
        Integer policyId,
        String title,
        List<String> currentRegions,
        List<String> resolvedRegions,
        List<RegionEvidence> evidence
) {
}
