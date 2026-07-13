package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.policy.region.PolicyRegionResolver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RegionResolver {
    private final PolicyRegionResolver policyRegionResolver;
    private final RegionCodeRepository regionCodeRepository;

    public RegionResolver(PolicyRegionResolver policyRegionResolver, RegionCodeRepository regionCodeRepository) {
        this.policyRegionResolver = policyRegionResolver;
        this.regionCodeRepository = regionCodeRepository;
    }

    public Set<RegionCode> resolve(Map<String, Object> fields) {
        var resolution = policyRegionResolver.resolve(fields);
        return resolution.regionIds().stream()
                .map(regionCodeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
