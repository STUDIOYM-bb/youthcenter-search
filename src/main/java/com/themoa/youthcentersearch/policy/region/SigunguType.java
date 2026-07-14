package com.themoa.youthcentersearch.policy.region;

public enum SigunguType {
    CITY,
    COUNTY,
    AUTONOMOUS_DISTRICT,
    METROPOLITAN_COUNTY;

    public static SigunguType from(SidoType parentType, String officialName) {
        if (parentType == null || officialName == null) {
            return null;
        }
        if ((parentType == SidoType.PROVINCE || parentType == SidoType.SPECIAL_AUTONOMOUS_PROVINCE)
                && officialName.endsWith("시")) {
            return CITY;
        }
        if ((parentType == SidoType.PROVINCE || parentType == SidoType.SPECIAL_AUTONOMOUS_PROVINCE)
                && officialName.endsWith("군")) {
            return COUNTY;
        }
        if ((parentType == SidoType.SPECIAL_CITY || parentType == SidoType.METROPOLITAN_CITY)
                && officialName.endsWith("구")) {
            return AUTONOMOUS_DISTRICT;
        }
        if (parentType == SidoType.METROPOLITAN_CITY && officialName.endsWith("군")) {
            return METROPOLITAN_COUNTY;
        }
        return null;
    }
}
