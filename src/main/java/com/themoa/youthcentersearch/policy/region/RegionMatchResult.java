package com.themoa.youthcentersearch.policy.region;

public record RegionMatchResult(
        RegionCompatibility compatibility,
        boolean eligible,
        int score,
        String reason
) {
    public boolean hardFiltered(boolean includeUnknown) {
        return compatibility == RegionCompatibility.NOT_MATCHED
                || (!includeUnknown && compatibility == RegionCompatibility.UNKNOWN);
    }

    public String label() {
        return compatibility.label();
    }
}
