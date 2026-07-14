package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class RegionCatalog {
    private final RegionCodeRepository repository;
    private final RegionExternalCodeRepository externalCodeRepository;
    private final RegionNameAliasGenerator aliasGenerator;
    private final RegionNormalizer normalizer;
    private volatile List<RegionCode> specificRegionsByLongestName;
    private volatile List<RegionCode> allRegions;
    private volatile Map<String, RegionCode> uniqueSigunguShortAliases;

    @Autowired
    public RegionCatalog(RegionCodeRepository repository, RegionExternalCodeRepository externalCodeRepository,
                         RegionNameAliasGenerator aliasGenerator, RegionNormalizer normalizer) {
        this.repository = repository;
        this.externalCodeRepository = externalCodeRepository;
        this.aliasGenerator = aliasGenerator;
        this.normalizer = normalizer;
    }

    public RegionCatalog(RegionCodeRepository repository, RegionAliasCatalog aliases, RegionNormalizer normalizer) {
        this(repository, null, new RegionNameAliasGenerator(), normalizer);
    }

    public RegionCatalog(RegionCodeRepository repository, RegionExternalCodeRepository externalCodeRepository,
                         RegionAliasCatalog aliases, RegionNormalizer normalizer) {
        this(repository, externalCodeRepository, new RegionNameAliasGenerator(), normalizer);
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
            if (externalCodeRepository != null) {
                externalCodeRepository.findByCodeSystemAndExternalCode("YOUTH_CENTER_ZIP", code)
                        .map(external -> external.getRegion())
                        .ifPresent(regions::add);
            }
        }
        return regions;
    }

    public List<RegionCode> allSpecificRegionsByLongestName() {
        List<RegionCode> cached = specificRegionsByLongestName;
        if (cached != null) {
            return cached;
        }
        List<RegionCode> loaded = allRegions().stream()
                .filter(region -> !"KR".equals(region.getRegionCode()))
                .filter(this::searchSupportedLevel)
                .sorted(Comparator.comparingInt((RegionCode region) -> region.displayName().length()).reversed())
                .toList();
        specificRegionsByLongestName = loaded;
        return loaded;
    }

    public List<RegionCode> allRegions() {
        List<RegionCode> cached = allRegions;
        if (cached != null) {
            return cached;
        }
        List<RegionCode> loaded = repository.findAll();
        allRegions = loaded;
        return loaded;
    }

    public void refreshCache() {
        specificRegionsByLongestName = null;
        allRegions = null;
        uniqueSigunguShortAliases = null;
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
        return logicalDistinct(matches);
    }

    public Optional<RegionCode> findProvince(String province) {
        if (!StringUtils.hasText(province)) {
            return Optional.empty();
        }
        String compactInput = normalizer.compact(province);
        return allRegions().stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .filter(region -> aliasGenerator.aliasesForSido(region).stream()
                        .map(normalizer::compact)
                        .anyMatch(compactInput::equals))
                .reduce(this::choosePreferred);
    }

    public Optional<RegionCode> findCity(String province, String city) {
        if (!StringUtils.hasText(city)) {
            return Optional.empty();
        }
        String compactProvince = normalizer.compact(province);
        String compactCity = normalizer.compact(city);
        return allRegions().stream()
                .filter(this::searchSupportedLevel)
                .filter(region -> StringUtils.hasText(region.getCity()))
                .filter(region -> !StringUtils.hasText(province) || aliasGenerator.aliasesForSido(provinceRegion(region)).stream()
                        .map(normalizer::compact)
                        .anyMatch(compactProvince::equals))
                .filter(region -> aliasGenerator.aliasesForSigungu(provinceRegion(region), region).stream()
                        .map(alias -> compactMunicipalityAlias(alias, region))
                        .anyMatch(compactCity::equals))
                .reduce(this::choosePreferred);
    }

    private boolean searchSupportedLevel(RegionCode region) {
        if ("PROVINCE".equals(region.getRegionLevel()) || "CITY".equals(region.getRegionLevel())) {
            return true;
        }
        return "DISTRICT".equals(region.getRegionLevel()) && isAutonomousDistrict(region);
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
        if (aliasesForRegion(region).stream().map(normalizer::compact).anyMatch(compactText::contains)) {
            return true;
        }
        return false;
    }

    private Set<String> aliasesForRegion(RegionCode region) {
        if ("PROVINCE".equals(region.getRegionLevel())) {
            return aliasGenerator.aliasesForSido(region);
        }
        return aliasGenerator.aliasesForSigungu(provinceRegion(region), region);
    }

    private String compactMunicipalityAlias(String alias, RegionCode region) {
        String compact = normalizer.compact(alias);
        for (String provinceAlias : aliasGenerator.aliasesForSido(provinceRegion(region))) {
            String compactProvince = normalizer.compact(provinceAlias);
            if (compact.startsWith(compactProvince)) {
                return compact.substring(compactProvince.length());
            }
        }
        return compact;
    }

    private Set<RegionCode> logicalDistinct(Set<RegionCode> regions) {
        return regions.stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(this::logicalKey, region -> region, this::choosePreferred, LinkedHashMap::new),
                        map -> new LinkedHashSet<>(map.values())));
    }

    public Set<String> generalDistrictAliasesFor(RegionCode cityRegion) {
        Set<String> aliases = new LinkedHashSet<>();
        if (cityRegion == null || !"CITY".equals(cityRegion.getRegionLevel()) || !StringUtils.hasText(cityRegion.getCity())) {
            return aliases;
        }
        String prefix = cityRegion.getCity() + " ";
        allRegions().stream()
                .filter(region -> "DISTRICT".equals(region.getRegionLevel()))
                .filter(region -> cityRegion.getProvince().equals(region.getProvince()))
                .filter(region -> StringUtils.hasText(region.getCity()) && region.getCity().startsWith(prefix))
                .forEach(region -> {
                    aliases.add(region.getCity());
                    String districtName = region.getCity().substring(prefix.length()).trim();
                    if (StringUtils.hasText(districtName)) {
                        aliases.add(districtName);
                        String shortAlias = aliasGenerator.shortAlias(districtName);
                        if (StringUtils.hasText(shortAlias)) {
                            aliases.add(shortAlias);
                        }
                    }
                });
        return aliases;
    }

    public Optional<RegionCode> uniqueSigunguByShortAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            return Optional.empty();
        }
        return Optional.ofNullable(uniqueSigunguShortAliases().get(normalizer.compact(alias)));
    }

    private Map<String, RegionCode> uniqueSigunguShortAliases() {
        Map<String, RegionCode> cached = uniqueSigunguShortAliases;
        if (cached != null) {
            return cached;
        }
        Map<String, List<RegionCode>> grouped = allSpecificRegionsByLongestName().stream()
                .filter(region -> "CITY".equals(region.getRegionLevel()) || "DISTRICT".equals(region.getRegionLevel()))
                .collect(java.util.stream.Collectors.groupingBy(region -> {
                    String shortAlias = aliasGenerator.shortAlias(region.getCity());
                    return StringUtils.hasText(shortAlias) ? normalizer.compact(shortAlias) : "";
                }, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        Map<String, RegionCode> unique = new LinkedHashMap<>();
        grouped.forEach((alias, regions) -> {
            if (StringUtils.hasText(alias) && regions.size() == 1) {
                unique.put(alias, regions.get(0));
            }
        });
        uniqueSigunguShortAliases = unique;
        return unique;
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
        return SearchRegionLevel.from(region) + "|" + region.getProvince() + "|" + (region.getCity() == null ? "" : region.getCity());
    }

    private RegionCode provinceRegion(RegionCode region) {
        return new RegionCode(null, "", region.getProvince(), null, "PROVINCE");
    }

    private boolean isAutonomousDistrict(RegionCode region) {
        SidoType sidoType = SidoType.fromOfficialName(region.getProvince());
        return SigunguType.from(sidoType, region.getCity()) == SigunguType.AUTONOMOUS_DISTRICT;
    }
}
