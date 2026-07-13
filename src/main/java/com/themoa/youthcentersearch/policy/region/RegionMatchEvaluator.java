package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class RegionMatchEvaluator {
    private final RegionCatalog catalog;
    private final RegionNormalizer normalizer;

    public RegionMatchEvaluator(RegionCatalog catalog, RegionNormalizer normalizer) {
        this.catalog = catalog;
        this.normalizer = normalizer;
    }

    public ResolvedUserRegion resolveUserRegion(String province, String city, String district) {
        String normalizedProvince = normalizer.normalizeProvince(province);
        String normalizedCity = normalizer.normalizeCity(city);
        if (!StringUtils.hasText(normalizedProvince) && StringUtils.hasText(normalizedCity)) {
            for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
                if (normalizedCity.equals(region.getCity()) || region.getCity() != null && region.getCity().contains(normalizedCity)) {
                    normalizedProvince = region.getProvince();
                    normalizedCity = cityPart(region);
                    break;
                }
            }
        }
        if (StringUtils.hasText(district) && !StringUtils.hasText(normalizedCity)) {
            for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
                if (region.getCity() != null && region.getCity().contains(district)) {
                    normalizedProvince = region.getProvince();
                    normalizedCity = cityPart(region);
                    break;
                }
            }
        }
        return new ResolvedUserRegion(emptyToNull(normalizedProvince), emptyToNull(normalizedCity), emptyToNull(district));
    }

    public RegionMatchResult evaluate(Policy policy, ResolvedUserRegion userRegion) {
        if (userRegion == null || !userRegion.hasRegion()) {
            return new RegionMatchResult(RegionMatchStatus.UNKNOWN, 0, "지역 조건 없음");
        }
        Set<com.themoa.youthcentersearch.policy.domain.PolicyRegion> regions = policy.getRegions();
        if (regions.isEmpty()) {
            return new RegionMatchResult(RegionMatchStatus.UNKNOWN, 0, "지역 확인 필요");
        }
        boolean multiple = regions.size() > 1;
        for (var policyRegion : regions) {
            RegionCode region = policyRegion.getRegion();
            if ("KR".equals(region.getRegionCode())) {
                return new RegionMatchResult(RegionMatchStatus.NATIONWIDE, 45, "전국 정책");
            }
            if (matchesDistrict(region, userRegion)) {
                return new RegionMatchResult(RegionMatchStatus.EXACT_DISTRICT, 100, region.displayName() + " 정책");
            }
            if (matchesCity(region, userRegion)) {
                return new RegionMatchResult(multiple ? RegionMatchStatus.MULTIPLE_REGION_MATCH : RegionMatchStatus.EXACT_CITY,
                        multiple ? 95 : 100, region.displayName() + " 정책");
            }
            if (matchesProvince(region, userRegion)) {
                return new RegionMatchResult(RegionMatchStatus.PROVINCE_MATCH, 80, region.getProvince() + " 전체 정책");
            }
        }
        return new RegionMatchResult(RegionMatchStatus.NOT_MATCHED, 0, "다른 지역 정책");
    }

    private boolean matchesProvince(RegionCode region, ResolvedUserRegion userRegion) {
        return "PROVINCE".equals(region.getRegionLevel()) && region.getProvince().equals(userRegion.province());
    }

    private boolean matchesCity(RegionCode region, ResolvedUserRegion userRegion) {
        if (!StringUtils.hasText(userRegion.city()) || region.getCity() == null) {
            return false;
        }
        return cityPart(region).equals(userRegion.city());
    }

    private boolean matchesDistrict(RegionCode region, ResolvedUserRegion userRegion) {
        return "DISTRICT".equals(region.getRegionLevel()) && StringUtils.hasText(userRegion.district())
                && region.getCity() != null && region.getCity().contains(userRegion.district());
    }

    private String cityPart(RegionCode region) {
        if (region.getCity() == null) {
            return null;
        }
        int idx = region.getCity().indexOf(' ');
        return idx > 0 ? region.getCity().substring(0, idx) : region.getCity();
    }

    private String emptyToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
