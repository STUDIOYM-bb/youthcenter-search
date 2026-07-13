package com.themoa.youthcentersearch.policy.region;

import org.springframework.stereotype.Component;

@Component
public class RegionNormalizer {
    private final RegionAliasCatalog aliases;

    public RegionNormalizer(RegionAliasCatalog aliases) {
        this.aliases = aliases;
    }

    public String normalizeProvince(String value) {
        return aliases.province(value);
    }

    public String normalizeCity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.endsWith("시") || text.endsWith("군") || text.endsWith("구")) {
            return text;
        }
        return text + "시";
    }

    public String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s·ㆍ,()\\[\\]{}<>]", "");
    }
}
