package com.themoa.youthcentersearch.region.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegionMunicipalityNormalizer {
    private static final Pattern COMPOSITE_CITY_DISTRICT = Pattern.compile("^(.+시)\\s+(.+구)$");
    private static final Pattern MUNICIPALITY_WITH_EMD = Pattern.compile("^(.+(?:시|군|구))\\s+.+(?:읍|면|동|리)$");

    private final RegionAdministrativeLevelResolver levelResolver;

    public RegionMunicipalityNormalizer(RegionAdministrativeLevelResolver levelResolver) {
        this.levelResolver = levelResolver;
    }

    public NormalizedMunicipality normalize(String provinceName, String childCode, String childName, String fullAddress) {
        if (!StringUtils.hasText(provinceName) || !StringUtils.hasText(childCode) || !StringUtils.hasText(childName)) {
            return new NormalizedMunicipality(provinceName, childName, childCode, false, "INVALID_REGION");
        }
        if (levelResolver.isProvinceUnitOnly(provinceName)) {
            return new NormalizedMunicipality(provinceName, null, childCode, false, "SEJONG_CHILD_IGNORED");
        }
        String name = childName.trim();
        String[] parts = name.split("\\s+");
        if (parts.length >= 2 && levelResolver.isMunicipalityName(parts[0]) && levelResolver.isTownVillageOrDong(parts[parts.length - 1])) {
            return new NormalizedMunicipality(provinceName, parts[0], childCode, true, null);
        }
        if (levelResolver.isTownVillageOrDong(name)) {
            return new NormalizedMunicipality(provinceName, null, childCode, false, "EMD_NOT_SUPPORTED");
        }
        Matcher emd = MUNICIPALITY_WITH_EMD.matcher(name);
        if (emd.matches()) {
            return new NormalizedMunicipality(provinceName, emd.group(1), childCode, true, null);
        }
        Matcher cityDistrict = COMPOSITE_CITY_DISTRICT.matcher(name);
        if (cityDistrict.matches() && levelResolver.isProvinceLike(provinceName)) {
            return new NormalizedMunicipality(provinceName, cityDistrict.group(1), childCode, true, null);
        }
        if (name.endsWith("구") && levelResolver.isProvinceLike(provinceName)) {
            return new NormalizedMunicipality(provinceName, null, childCode, false, "GENERAL_DISTRICT_WITHOUT_CITY");
        }
        if (!levelResolver.isMunicipalityName(name)) {
            return new NormalizedMunicipality(provinceName, null, childCode, false, "UNSUPPORTED_LEVEL");
        }
        return new NormalizedMunicipality(provinceName, name, childCode, true, null);
    }
}
