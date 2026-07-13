package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.PolicyRegionResolution;
import com.themoa.youthcentersearch.policy.repository.PolicyRegionRepository;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyRegionSyncService {
    private final PolicyRegionRepository policyRegionRepository;
    private final RegionCodeRepository regionCodeRepository;

    public PolicyRegionSyncService(PolicyRegionRepository policyRegionRepository, RegionCodeRepository regionCodeRepository) {
        this.policyRegionRepository = policyRegionRepository;
        this.regionCodeRepository = regionCodeRepository;
    }

    public RegionSyncResult syncRegions(Policy policy, PolicyRegionResolution resolution) {
        Set<RegionCode> targetRegions = resolution.regionIds().stream()
                .map(regionCodeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return syncRegions(policy, targetRegions);
    }

    public RegionSyncResult syncRegions(Policy policy, Set<RegionCode> targetRegions) {
        Map<Integer, PolicyRegion> existing = policyRegionRepository.findByPolicy(policy).stream()
                .collect(Collectors.toMap(region -> region.getRegion().getId(), region -> region));
        Set<Integer> targetIds = targetRegions.stream().map(RegionCode::getId).collect(Collectors.toSet());
        Set<Integer> removedIds = existing.keySet().stream()
                .filter(id -> !targetIds.contains(id))
                .collect(Collectors.toSet());
        int removed = 0;
        int added = 0;
        if (!removedIds.isEmpty()) {
            var removedRegions = existing.entrySet().stream()
                    .filter(entry -> removedIds.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            policy.getRegions().removeIf(region -> removedIds.contains(region.getRegion().getId()));
            policyRegionRepository.deleteAll(removedRegions);
            policyRegionRepository.flush();
            removed = removedRegions.size();
        }
        for (RegionCode region : targetRegions) {
            if (!existing.containsKey(region.getId())) {
                PolicyRegion policyRegion = policyRegionRepository.save(new PolicyRegion(policy, region));
                policy.getRegions().add(policyRegion);
                added++;
            }
        }
        if (added > 0) {
            policyRegionRepository.flush();
        }
        return new RegionSyncResult(added, removed, added > 0 || removed > 0);
    }

    public record RegionSyncResult(int addedCount, int removedCount, boolean changed) {
    }
}
