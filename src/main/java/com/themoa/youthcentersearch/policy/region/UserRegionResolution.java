package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

import java.util.List;

public record UserRegionResolution(
        UserRegionResolutionStatus status,
        String province,
        String city,
        String district,
        String regionCode,
        String regionName,
        List<String> candidates
) {
    public static UserRegionResolution notFound() {
        return new UserRegionResolution(UserRegionResolutionStatus.NOT_FOUND, null, null, null, null, null, List.of());
    }

    public static UserRegionResolution ambiguous(List<RegionCode> candidates) {
        return new UserRegionResolution(UserRegionResolutionStatus.AMBIGUOUS, null, null, null, null, null,
                candidates.stream().map(RegionCode::displayName).distinct().sorted().toList());
    }

    public static UserRegionResolution of(UserRegionResolutionStatus status, RegionCode region) {
        String district = "DISTRICT".equals(region.getRegionLevel()) ? region.getCity() : null;
        String city = "PROVINCE".equals(region.getRegionLevel()) ? null : region.getCity();
        return new UserRegionResolution(status, region.getProvince(), city, district,
                region.getRegionCode(), region.displayName(), List.of(region.displayName()));
    }

    public boolean resolved() {
        return status == UserRegionResolutionStatus.EXACT || status == UserRegionResolutionStatus.UNIQUE_ALIAS;
    }
}
