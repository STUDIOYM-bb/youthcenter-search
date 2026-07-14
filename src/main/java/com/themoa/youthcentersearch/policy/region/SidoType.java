package com.themoa.youthcentersearch.policy.region;

public enum SidoType {
    SPECIAL_CITY,
    METROPOLITAN_CITY,
    SPECIAL_AUTONOMOUS_CITY,
    PROVINCE,
    SPECIAL_AUTONOMOUS_PROVINCE;

    public static SidoType fromOfficialName(String officialName) {
        if (officialName == null) {
            return null;
        }
        if (officialName.endsWith("특별자치시")) {
            return SPECIAL_AUTONOMOUS_CITY;
        }
        if (officialName.endsWith("특별자치도")) {
            return SPECIAL_AUTONOMOUS_PROVINCE;
        }
        if (officialName.endsWith("특별시")) {
            return SPECIAL_CITY;
        }
        if (officialName.endsWith("광역시")) {
            return METROPOLITAN_CITY;
        }
        if (officialName.endsWith("도")) {
            return PROVINCE;
        }
        return null;
    }
}
