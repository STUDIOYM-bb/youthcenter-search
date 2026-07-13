package com.themoa.youthcentersearch.policy.region;

public record RegionMatchResult(
        RegionMatchStatus status,
        int score,
        String description
) {
    public boolean hardFiltered(boolean includeUnknown) {
        return status == RegionMatchStatus.NOT_MATCHED || (!includeUnknown && status == RegionMatchStatus.UNKNOWN);
    }
}
