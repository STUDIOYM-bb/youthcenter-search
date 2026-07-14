package com.themoa.youthcentersearch.policy.region;

public record ResolvedUserRegion(
        String province,
        String city,
        String district,
        SearchRegionLevel level,
        com.themoa.youthcentersearch.policy.domain.RegionCode region
) {
    public ResolvedUserRegion(String province, String city, String district) {
        this(province, city, district, inferLevel(province, city, district), null);
    }

    public boolean hasRegion() {
        return level == SearchRegionLevel.NATIONWIDE
                || (province != null && !province.isBlank())
                || (city != null && !city.isBlank())
                || (district != null && !district.isBlank());
    }

    public boolean nationwide() {
        return level == SearchRegionLevel.NATIONWIDE;
    }

    private static SearchRegionLevel inferLevel(String province, String city, String district) {
        if ("전국".equals(province)) {
            return SearchRegionLevel.NATIONWIDE;
        }
        if ((city != null && !city.isBlank()) || (district != null && !district.isBlank())) {
            return SearchRegionLevel.SIGUNGU;
        }
        if (province != null && !province.isBlank()) {
            return SearchRegionLevel.SIDO;
        }
        return null;
    }
}
