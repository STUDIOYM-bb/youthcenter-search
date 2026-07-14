package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class RegionNameAliasGenerator {
    private static final String[] SUFFIXES = {
            "특별자치도",
            "특별자치시",
            "특별시",
            "광역시",
            "시",
            "군",
            "구",
            "도"
    };

    public Set<String> aliasesForSido(RegionCode sido) {
        Set<String> aliases = new LinkedHashSet<>();
        if (sido == null || !StringUtils.hasText(sido.getProvince())) {
            return aliases;
        }
        String official = sido.getProvince().trim();
        addAlias(aliases, official);
        String base = removeAdministrativeSuffix(official);
        addAlias(aliases, base);
        if (base != null && base.length() >= 3 && (base.endsWith("남") || base.endsWith("북"))) {
            addAlias(aliases, base.substring(0, 1) + base.substring(base.length() - 1));
        }
        SidoType type = SidoType.fromOfficialName(official);
        if (type == SidoType.SPECIAL_CITY || type == SidoType.METROPOLITAN_CITY || type == SidoType.SPECIAL_AUTONOMOUS_CITY) {
            addAlias(aliases, base + "시");
        }
        if (type == SidoType.SPECIAL_AUTONOMOUS_PROVINCE) {
            addAlias(aliases, base + "도");
        }
        return aliases;
    }

    public Set<String> aliasesForSigungu(RegionCode sido, RegionCode sigungu) {
        Set<String> aliases = new LinkedHashSet<>();
        if (sigungu == null || !StringUtils.hasText(sigungu.getCity())) {
            return aliases;
        }
        String official = sigungu.getCity().trim();
        addAlias(aliases, official);
        String base = removeAdministrativeSuffix(official);
        addAlias(aliases, base);
        if (sido != null) {
            for (String sidoAlias : aliasesForSido(sido)) {
                addAlias(aliases, sidoAlias + " " + official);
                addAlias(aliases, sidoAlias + " " + base);
            }
        }
        return aliases;
    }

    public String shortAlias(String value) {
        String alias = removeAdministrativeSuffix(value);
        return validAlias(alias) ? alias.trim() : null;
    }

    public String removeAdministrativeSuffix(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        for (String suffix : SUFFIXES) {
            if (trimmed.endsWith(suffix)) {
                return trimmed.substring(0, trimmed.length() - suffix.length()).trim();
            }
        }
        return trimmed;
    }

    private void addAlias(Set<String> aliases, String alias) {
        if (validAlias(alias)) {
            aliases.add(alias.trim());
        }
    }

    private boolean validAlias(String alias) {
        return StringUtils.hasText(alias) && alias.trim().length() >= 2;
    }
}
