package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

import java.util.Set;

public record InstitutionRegionResult(
        InstitutionRegionType type,
        Set<RegionCode> regions
) {
}
