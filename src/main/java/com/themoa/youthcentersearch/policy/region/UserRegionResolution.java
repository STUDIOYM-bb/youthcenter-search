package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

import java.util.List;

public record UserRegionResolution(
        UserRegionResolutionStatus status,
        String rawRegionText,
        SearchRegionLevel regionLevel,
        RegionCode selectedRegion,
        String province,
        String city,
        String district,
        String regionCode,
        String regionName,
        String matchedText,
        RegionTextMatchType matchType,
        List<RegionCandidate> candidateDetails,
        List<String> candidates
) {
    public static UserRegionResolution notFound() {
        return new UserRegionResolution(UserRegionResolutionStatus.NOT_FOUND, null, null, null,
                null, null, null, null, null, null, null, List.of(), List.of());
    }

    public static UserRegionResolution nationwide(String rawRegionText, RegionCode region, String matchedText) {
        String regionCode = region == null ? "KR" : region.getRegionCode();
        String regionName = region == null ? "전국" : region.displayName();
        return new UserRegionResolution(UserRegionResolutionStatus.EXACT, rawRegionText, SearchRegionLevel.NATIONWIDE, region,
                "전국", null, null, regionCode, regionName, matchedText, RegionTextMatchType.OFFICIAL_SIDO_NAME,
                List.of(), List.of(regionName));
    }

    public static UserRegionResolution ambiguous(String rawRegionText, List<RegionTextMatchCandidate> candidates) {
        List<RegionCandidate> details = candidates.stream()
                .map(UserRegionResolution::candidate)
                .distinct()
                .toList();
        return new UserRegionResolution(UserRegionResolutionStatus.AMBIGUOUS, rawRegionText, null, null,
                null, null, null, null, null, null, null, details,
                details.stream().map(RegionCandidate::displayName).distinct().sorted().toList());
    }

    public static UserRegionResolution ambiguous(List<RegionCode> candidates) {
        List<RegionTextMatchCandidate> converted = candidates.stream()
                .map(region -> new RegionTextMatchCandidate(region, RegionTextMatchType.GENERATED_SIGUNGU_ALIAS, region.displayName(), 80))
                .toList();
        return ambiguous(null, converted);
    }

    public static UserRegionResolution of(UserRegionResolutionStatus status, String rawRegionText, RegionTextMatchCandidate candidate) {
        RegionCode region = candidate.region();
        String city = "PROVINCE".equals(region.getRegionLevel()) ? null : region.getCity();
        String district = null;
        SearchRegionLevel level = SearchRegionLevel.from(region);
        return new UserRegionResolution(status, rawRegionText, level, region,
                region.getProvince(), city, district, region.getRegionCode(), region.displayName(),
                candidate.matchedText(), candidate.matchType(), List.of(candidate(candidate)), List.of(region.displayName()));
    }

    public static UserRegionResolution of(UserRegionResolutionStatus status, RegionCode region) {
        return of(status, null, new RegionTextMatchCandidate(region, RegionTextMatchType.FULL_OFFICIAL_PATH, region.displayName(), 100));
    }

    public boolean resolved() {
        return status == UserRegionResolutionStatus.EXACT || status == UserRegionResolutionStatus.UNIQUE_ALIAS;
    }

    private static RegionCandidate candidate(RegionTextMatchCandidate candidate) {
        RegionCode region = candidate.region();
        return new RegionCandidate(region.getId(), region.getProvince(),
                "PROVINCE".equals(region.getRegionLevel()) ? null : region.getCity(),
                region.displayName(), candidate.matchedText(), candidate.matchType());
    }
}
