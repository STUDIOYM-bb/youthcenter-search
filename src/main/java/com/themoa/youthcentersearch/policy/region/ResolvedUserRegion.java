package com.themoa.youthcentersearch.policy.region;

public record ResolvedUserRegion(
        String province,
        String city,
        String district
) {
    public boolean hasRegion() {
        return (province != null && !province.isBlank()) || (city != null && !city.isBlank())
                || (district != null && !district.isBlank());
    }
}
