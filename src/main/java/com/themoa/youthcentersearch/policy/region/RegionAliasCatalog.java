package com.themoa.youthcentersearch.policy.region;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class RegionAliasCatalog {
    private final RegionNameAliasGenerator aliasGenerator;

    public RegionAliasCatalog(RegionNameAliasGenerator aliasGenerator) {
        this.aliasGenerator = aliasGenerator;
    }

    public RegionAliasCatalog() {
        this(new RegionNameAliasGenerator());
    }

    public String province(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    public Set<String> aliasesForProvince(String officialName) {
        if (!StringUtils.hasText(officialName)) {
            return new LinkedHashSet<>();
        }
        return aliasGenerator.aliasesForSido(new com.themoa.youthcentersearch.policy.domain.RegionCode(null, "", officialName, null, "PROVINCE"));
    }

    public Set<String> aliasesForMunicipality(String provinceName, String municipalityName) {
        if (!StringUtils.hasText(municipalityName)) {
            return new LinkedHashSet<>();
        }
        var sido = new com.themoa.youthcentersearch.policy.domain.RegionCode(null, "", provinceName, null, "PROVINCE");
        var sigungu = new com.themoa.youthcentersearch.policy.domain.RegionCode(null, "", provinceName, municipalityName, "CITY");
        return aliasGenerator.aliasesForSigungu(sido, sigungu);
    }

    public String shortAlias(String value) {
        return aliasGenerator.shortAlias(value);
    }
}
