package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.admin.dto.RegionAnomalyResponse;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.PolicyRegionResolver;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminRegionDiagnosticsService {
    private final PolicyRepository policyRepository;
    private final PolicyRegionResolver resolver;
    private final RegionCatalog regionCatalog;

    public AdminRegionDiagnosticsService(PolicyRepository policyRepository, PolicyRegionResolver resolver, RegionCatalog regionCatalog) {
        this.policyRepository = policyRepository;
        this.resolver = resolver;
        this.regionCatalog = regionCatalog;
    }

    public List<RegionAnomalyResponse> anomalies() {
        return policyRepository.findActivePolicyIds(PageRequest.of(0, 5000)).stream()
                .map(id -> policyRepository.findWithRelationsByIdIn(List.of(id)).stream().findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::anomaly)
                .filter(java.util.Objects::nonNull)
                .limit(100)
                .toList();
    }

    private RegionAnomalyResponse anomaly(Policy policy) {
        var resolved = resolver.resolve(fields(policy));
        List<String> current = policy.getRegions().stream().map(region -> region.getRegion().displayName()).sorted().toList();
        List<String> next = resolved.regionNames().stream().sorted().toList();
        boolean currentlyNationwide = current.contains("전국");
        boolean titleHasRegion = policy.getTitle() != null && !regionCatalog.findInText(policy.getTitle()).isEmpty();
        if ((currentlyNationwide && !next.contains("전국")) || (titleHasRegion && !current.equals(next))) {
            return new RegionAnomalyResponse(policy.getId(), policy.getTitle(), current, next, resolved.evidence());
        }
        return null;
    }

    private Map<String, Object> fields(Policy policy) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("plcyNm", policy.getTitle());
        fields.put("title", policy.getTitle());
        fields.put("plcyExplnCn", policy.getSummary());
        fields.put("summary", policy.getSummary());
        fields.put("sprvsnInstCdNm", policy.getAgencyName());
        fields.put("agencyName", policy.getAgencyName());
        if (policy.getCondition() != null) {
            fields.put("ptcpPrpTrgtCn", policy.getCondition().getConditionSummary());
            fields.put("conditionSummary", policy.getCondition().getConditionSummary());
        }
        return fields;
    }
}
