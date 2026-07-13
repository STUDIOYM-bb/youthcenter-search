package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class RegionCatalog {
    private final RegionCodeRepository repository;
    private final RegionAliasCatalog aliases;
    private final RegionNormalizer normalizer;
    private volatile List<RegionCode> specificRegionsByLongestName;

    public RegionCatalog(RegionCodeRepository repository, RegionAliasCatalog aliases, RegionNormalizer normalizer) {
        this.repository = repository;
        this.aliases = aliases;
        this.normalizer = normalizer;
    }

    public Optional<RegionCode> nationwide() {
        return repository.findByRegionCode("KR");
    }

    public Optional<RegionCode> byCode(String code) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }
        return repository.findByRegionCode(code.trim());
    }

    public Set<RegionCode> byZipCd(String zipCd) {
        Set<RegionCode> regions = new LinkedHashSet<>();
        if (!StringUtils.hasText(zipCd)) {
            return regions;
        }
        for (String token : zipCd.split(",")) {
            String code = token.trim();
            byCode(code).ifPresent(regions::add);
        }
        return regions;
    }

    public List<RegionCode> allSpecificRegionsByLongestName() {
        List<RegionCode> cached = specificRegionsByLongestName;
        if (cached != null) {
            return cached;
        }
        List<RegionCode> loaded = repository.findAll().stream()
                .filter(region -> !"KR".equals(region.getRegionCode()))
                .filter(this::searchSupportedLevel)
                .sorted(Comparator.comparingInt((RegionCode region) -> region.displayName().length()).reversed())
                .toList();
        specificRegionsByLongestName = loaded;
        return loaded;
    }

    public void refreshCache() {
        specificRegionsByLongestName = null;
    }

    public Set<RegionCode> findInText(String text) {
        Set<RegionCode> matches = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return matches;
        }
        String compact = normalizer.compact(text);
        for (RegionCode region : allSpecificRegionsByLongestName()) {
            if (matchesRegion(compact, region)) {
                matches.add(region);
            }
        }
        for (var entry : aliases.provinceAliases().entrySet()) {
            if (compact.contains(entry.getKey())) {
                repository.findByProvince(entry.getValue()).stream()
                        .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                        .findFirst()
                        .ifPresent(matches::add);
            }
        }
        return removeCoveredProvince(matches);
    }

    public Optional<RegionCode> findProvince(String province) {
        String normalized = aliases.province(province);
        return repository.findByProvince(normalized).stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .findFirst();
    }

    public Optional<RegionCode> findCity(String province, String city) {
        String normalizedProvince = aliases.province(province);
        String normalizedCity = normalizer.normalizeCity(city);
        return repository.findByProvinceAndCity(normalizedProvince, normalizedCity).stream()
                .filter(this::searchSupportedLevel)
                .findFirst();
    }

    private boolean searchSupportedLevel(RegionCode region) {
        if ("PROVINCE".equals(region.getRegionLevel()) || "CITY".equals(region.getRegionLevel())) {
            return true;
        }
        return "DISTRICT".equals(region.getRegionLevel()) && !region.getProvince().endsWith("도");
    }

    public Optional<RegionCode> findProvinceOrCity(String province, String city) {
        if (StringUtils.hasText(city)) {
            Optional<RegionCode> resolved = findCity(province, city);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return findProvince(province);
    }

    private boolean matchesRegion(String compactText, RegionCode region) {
        String display = normalizer.compact(region.displayName());
        if (compactText.contains(display)) {
            return true;
        }
        String city = region.getCity();
        if (StringUtils.hasText(city) && compactText.contains(normalizer.compact(city))) {
            return true;
        }
        return compactText.contains(normalizer.compact(region.getProvince())) && "PROVINCE".equals(region.getRegionLevel());
    }

    private Set<RegionCode> removeCoveredProvince(Set<RegionCode> regions) {
        Set<String> specificProvinces = regions.stream()
                .filter(region -> !"PROVINCE".equals(region.getRegionLevel()))
                .map(RegionCode::getProvince)
                .collect(java.util.stream.Collectors.toSet());
        Set<RegionCode> result = new LinkedHashSet<>();
        for (RegionCode region : regions) {
            if ("PROVINCE".equals(region.getRegionLevel()) && specificProvinces.contains(region.getProvince())) {
                continue;
            }
            result.add(region);
        }
        return result;
    }
}
