package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionEligiblePolicyCandidate;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.policy.region.SearchRegionLevel;
import com.themoa.youthcentersearch.policy.repository.RegionEligiblePolicyCandidateRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RegionEligiblePolicyCandidateService {
    private final RegionEligiblePolicyCandidateRepository repository;
    private final RegionCatalog catalog;

    public RegionEligiblePolicyCandidateService(RegionEligiblePolicyCandidateRepository repository, RegionCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    public List<RegionEligiblePolicyCandidate> findEligibleCandidates(ResolvedUserRegion userRegion) {
        if (userRegion == null || !userRegion.hasRegion() || userRegion.region() == null) {
            return List.of();
        }
        Integer userRegionId = userRegion.region().getId();
        Integer parentSidoId = parentSidoId(userRegion);
        Integer nationwideId = catalog.nationwide().map(RegionCode::getId).orElse(null);
        if (userRegionId == null || nationwideId == null) {
            return List.of();
        }
        Set<Integer> exactIds = exactRegionIds(userRegion);
        Set<Integer> parentIds = parentSidoIds(userRegion);
        Set<Integer> nationwideIds = nationwideId == null ? Set.of() : Set.of(nationwideId);
        LinkedHashSet<Integer> eligibleRegionIds = new LinkedHashSet<>();
        eligibleRegionIds.addAll(exactIds);
        if (userRegion.level() == SearchRegionLevel.SIGUNGU) {
            eligibleRegionIds.addAll(parentIds);
        }
        eligibleRegionIds.addAll(nationwideIds);
        if (eligibleRegionIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = repository.findEligibleRegionRows(eligibleRegionIds.stream().toList());
        Map<Integer, RegionCompatibility> best = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Integer policyId = ((Number) row[0]).intValue();
            Integer regionId = ((Number) row[1]).intValue();
            int regionCount = ((Number) row[2]).intValue();
            RegionCompatibility compatibility = compatibility(regionId, regionCount, userRegion, exactIds, parentIds, nationwideIds);
            best.merge(policyId, compatibility, this::moreSpecific);
        }
        return best.entrySet().stream()
                .map(entry -> new RegionEligiblePolicyCandidate(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Set<Integer> exactRegionIds(ResolvedUserRegion userRegion) {
        if (userRegion.level() == SearchRegionLevel.SIGUNGU) {
            return catalog.allRegions().stream()
                    .filter(region -> java.util.Objects.equals(region.getProvince(), userRegion.region().getProvince()))
                    .filter(region -> java.util.Objects.equals(region.getCity(), userRegion.region().getCity()))
                    .map(RegionCode::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (userRegion.level() == SearchRegionLevel.SIDO) {
            return parentSidoIds(userRegion);
        }
        return Set.of();
    }

    private Set<Integer> parentSidoIds(ResolvedUserRegion userRegion) {
        String province = userRegion.region() == null ? userRegion.province() : userRegion.region().getProvince();
        return catalog.allRegions().stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .filter(region -> java.util.Objects.equals(region.getProvince(), province))
                .map(RegionCode::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private RegionCompatibility compatibility(Integer regionId, int regionCount, ResolvedUserRegion userRegion, Set<Integer> exactIds,
                                              Set<Integer> parentIds, Set<Integer> nationwideIds) {
        if (nationwideIds.contains(regionId)) {
            return RegionCompatibility.NATIONWIDE;
        }
        if (regionCount > 1) {
            return RegionCompatibility.MULTIPLE_REGION_MATCH;
        }
        if (userRegion.level() == SearchRegionLevel.SIGUNGU && exactIds.contains(regionId)) {
            return RegionCompatibility.EXACT_SIGUNGU;
        }
        if (userRegion.level() == SearchRegionLevel.SIGUNGU && parentIds.contains(regionId)) {
            return RegionCompatibility.PARENT_SIDO;
        }
        if (userRegion.level() == SearchRegionLevel.SIDO && exactIds.contains(regionId)) {
            return RegionCompatibility.EXACT_SIDO;
        }
        return RegionCompatibility.MULTIPLE_REGION_MATCH;
    }

    private Integer parentSidoId(ResolvedUserRegion userRegion) {
        if (userRegion.level() == SearchRegionLevel.SIGUNGU) {
            RegionCode region = userRegion.region();
            if (region != null && region.getParent() != null) {
                return region.getParent().getId();
            }
            return catalog.findProvince(userRegion.province()).map(RegionCode::getId).orElse(null);
        }
        return userRegion.region() == null ? null : userRegion.region().getId();
    }

    private RegionCompatibility moreSpecific(RegionCompatibility left, RegionCompatibility right) {
        return specificity(left) <= specificity(right) ? left : right;
    }

    private int specificity(RegionCompatibility compatibility) {
        return switch (compatibility) {
            case EXACT_SIGUNGU -> 0;
            case PARENT_SIDO, EXACT_SIDO -> 1;
            case NATIONWIDE -> 2;
            case MULTIPLE_REGION_MATCH -> 3;
            case UNKNOWN -> 4;
            case NOT_MATCHED -> 5;
        };
    }
}
