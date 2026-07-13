package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserRegionTextResolver {
    private final RegionCatalog regionCatalog;
    private final RegionAliasCatalog aliases;
    private final RegionNormalizer normalizer;

    public UserRegionTextResolver(RegionCatalog regionCatalog, RegionAliasCatalog aliases, RegionNormalizer normalizer) {
        this.regionCatalog = regionCatalog;
        this.aliases = aliases;
        this.normalizer = normalizer;
    }

    public UserRegionResolution resolve(String text) {
        if (!StringUtils.hasText(text)) {
            return UserRegionResolution.notFound();
        }
        String compact = normalizer.compact(text);
        List<RegionCode> regions = regionCatalog.allSpecificRegionsByLongestName();

        List<RegionCode> exact = new ArrayList<>();
        for (RegionCode region : regions) {
            if (matchesExact(compact, region)) {
                exact.add(region);
            }
        }
        exact = logicalDistinct(exact);
        exact = removeCoveredProvinces(exact);
        if (exact.size() == 1) {
            return UserRegionResolution.of(UserRegionResolutionStatus.EXACT, exact.get(0));
        }
        if (exact.size() > 1) {
            return pickMostSpecificOrAmbiguous(exact, UserRegionResolutionStatus.EXACT);
        }

        Map<String, List<RegionCode>> aliasMatches = new LinkedHashMap<>();
        for (var entry : aliases.provinceAliases().entrySet()) {
            if (compact.contains(normalizer.compact(entry.getKey()))) {
                regionCatalog.findProvince(entry.getValue())
                        .ifPresent(region -> aliasMatches.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(region));
            }
        }
        for (RegionCode region : regions) {
            String alias = shortAlias(region);
            if (StringUtils.hasText(alias) && compact.contains(normalizer.compact(alias))) {
                aliasMatches.computeIfAbsent(alias, key -> new ArrayList<>()).add(region);
            }
        }
        List<RegionCode> candidates = logicalDistinct(aliasMatches.values().stream().flatMap(List::stream).toList());
        if (candidates.size() == 1) {
            return UserRegionResolution.of(UserRegionResolutionStatus.UNIQUE_ALIAS, candidates.get(0));
        }
        if (candidates.size() > 1) {
            List<RegionCode> withoutCoveredProvince = removeCoveredProvinces(candidates);
            if (withoutCoveredProvince.size() == 1) {
                return UserRegionResolution.of(UserRegionResolutionStatus.UNIQUE_ALIAS, withoutCoveredProvince.get(0));
            }
            return UserRegionResolution.ambiguous(withoutCoveredProvince);
        }
        return UserRegionResolution.notFound();
    }

    private boolean matchesExact(String compactText, RegionCode region) {
        String display = normalizer.compact(region.displayName());
        if (compactText.contains(display)) {
            return true;
        }
        String province = normalizer.compact(region.getProvince());
        if ("PROVINCE".equals(region.getRegionLevel()) && compactText.contains(province)) {
            return true;
        }
        String officialCity = normalizer.compact(region.getCity());
        String officialShortCity = normalizer.compact(shortAlias(region));
        if (StringUtils.hasText(officialCity)
                && (compactText.contains(province + officialCity) || compactText.contains(province + officialShortCity))) {
            return true;
        }
        for (var entry : aliases.provinceAliases().entrySet()) {
            if (!entry.getValue().equals(region.getProvince())) {
                continue;
            }
            String provinceAlias = normalizer.compact(entry.getKey());
            if (StringUtils.hasText(officialCity) && (compactText.contains(provinceAlias + officialCity) || compactText.contains(provinceAlias + officialShortCity))) {
                return true;
            }
        }
        return false;
    }

    private UserRegionResolution pickMostSpecificOrAmbiguous(List<RegionCode> candidates, UserRegionResolutionStatus status) {
        List<RegionCode> withoutCoveredProvince = removeCoveredProvinces(candidates);
        List<RegionCode> sorted = withoutCoveredProvince.stream()
                .sorted(Comparator.comparingInt((RegionCode region) -> specificity(region)).reversed()
                        .thenComparing(region -> region.displayName().length(), Comparator.reverseOrder()))
                .toList();
        int topSpecificity = specificity(sorted.get(0));
        List<RegionCode> top = sorted.stream().filter(region -> specificity(region) == topSpecificity).toList();
        if (top.size() == 1) {
            return UserRegionResolution.of(status, top.get(0));
        }
        return UserRegionResolution.ambiguous(top);
    }

    private List<RegionCode> removeCoveredProvinces(List<RegionCode> candidates) {
        var provincesWithSpecific = candidates.stream()
                .filter(region -> !"PROVINCE".equals(region.getRegionLevel()))
                .map(RegionCode::getProvince)
                .collect(java.util.stream.Collectors.toSet());
        return candidates.stream()
                .filter(region -> !"PROVINCE".equals(region.getRegionLevel()) || !provincesWithSpecific.contains(region.getProvince()))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(this::logicalKey, region -> region, this::choosePreferred, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));
    }

    private List<RegionCode> logicalDistinct(List<RegionCode> candidates) {
        return new ArrayList<>(candidates.stream()
                .collect(java.util.stream.Collectors.toMap(this::logicalKey, region -> region, this::choosePreferred, LinkedHashMap::new))
                .values());
    }

    private RegionCode choosePreferred(RegionCode left, RegionCode right) {
        if (standardInternalCode(right) && !standardInternalCode(left)) {
            return right;
        }
        return left;
    }

    private boolean standardInternalCode(RegionCode region) {
        String code = region.getRegionCode();
        return "KR".equals(code) || code.startsWith("P:") || code.startsWith("M:");
    }

    private String logicalKey(RegionCode region) {
        return region.getRegionLevel() + "|" + region.getProvince() + "|" + (region.getCity() == null ? "" : region.getCity());
    }

    private int specificity(RegionCode region) {
        return switch (region.getRegionLevel()) {
            case "DISTRICT" -> 3;
            case "CITY" -> 2;
            case "PROVINCE" -> 1;
            default -> 0;
        };
    }

    private String shortAlias(RegionCode region) {
        String value = region.getCity();
        if (!StringUtils.hasText(value)) {
            value = region.getProvince();
        }
        String alias = value.replaceAll("(특별자치도|특별자치시|특별시|광역시|시|군|구)$", "");
        return alias.length() >= 2 ? alias : null;
    }
}
