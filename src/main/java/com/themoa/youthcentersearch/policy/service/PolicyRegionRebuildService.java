package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.region.PolicyRegionResolver;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.service.PolicyDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyRegionRebuildService {
    private static final Logger log = LoggerFactory.getLogger(PolicyRegionRebuildService.class);

    private final PolicyRepository policyRepository;
    private final PolicyRegionResolver regionResolver;
    private final PolicyRegionSyncService regionSyncService;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final PolicyDocumentBuilder documentBuilder;
    private final RegionRebuildProperties properties;
    private final TransactionTemplate transactionTemplate;

    public PolicyRegionRebuildService(PolicyRepository policyRepository,
                                      PolicyRegionResolver regionResolver,
                                      PolicyRegionSyncService regionSyncService,
                                      PolicyEmbeddingSyncRepository syncRepository,
                                      PolicyDocumentBuilder documentBuilder,
                                      RegionRebuildProperties properties,
                                      TransactionTemplate transactionTemplate) {
        this.policyRepository = policyRepository;
        this.regionResolver = regionResolver;
        this.regionSyncService = regionSyncService;
        this.syncRepository = syncRepository;
        this.documentBuilder = documentBuilder;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    public PolicyRegionRebuildResult rebuildAll() {
        long total = policyRepository.countByActiveTrue();
        long processed = 0;
        long changed = 0;
        long nationwideToRegion = 0;
        long nationwideToUnknown = 0;
        long unchanged = 0;
        long failed = 0;
        long pendingQueued = 0;
        int page = 0;
        while (true) {
            var ids = policyRepository.findActivePolicyIds(PageRequest.of(page++, Math.max(1, properties.getBatchSize())));
            if (ids.isEmpty()) {
                break;
            }
            for (Integer id : ids) {
                try {
                    RebuildOneResult result = rebuildOne(id);
                    processed++;
                    if (result.changed()) {
                        changed++;
                        if (result.wasNationwide() && result.nowUnknown()) nationwideToUnknown++;
                        if (result.wasNationwide() && result.nowRegion()) nationwideToRegion++;
                        if (result.queued()) pendingQueued++;
                    } else {
                        unchanged++;
                    }
                } catch (RuntimeException ex) {
                    failed++;
                    log.warn("Policy region rebuild failed. policyId={}", id, ex);
                }
            }
        }
        return new PolicyRegionRebuildResult(total, processed, changed, nationwideToRegion, nationwideToUnknown,
                unchanged, failed, pendingQueued);
    }

    private RebuildOneResult rebuildOne(Integer policyId) {
        return transactionTemplate.execute(status -> {
            Policy policy = policyRepository.findWithRelationsByIdIn(java.util.List.of(policyId)).stream()
                    .findFirst().orElseThrow();
            Set<String> before = policy.getRegions().stream()
                    .map(region -> region.getRegion().getRegionCode())
                    .collect(Collectors.toSet());
            boolean wasNationwide = before.contains("KR");
            var resolution = regionResolver.resolve(fields(policy));
            var syncResult = regionSyncService.syncRegions(policy, resolution);
            boolean queued = false;
            if (syncResult.changed() && properties.isEnqueueChangedPolicies()) {
                Policy reloaded = policyRepository.findWithRelationsByIdIn(java.util.List.of(policyId)).stream()
                        .findFirst().orElse(policy);
                String contentHash = documentBuilder.build(reloaded).contentHash();
                PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policyId).orElse(null);
                if (sync == null) {
                    syncRepository.save(new PolicyEmbeddingSync(reloaded, contentHash));
                } else {
                    sync.queue(contentHash);
                }
                queued = true;
            }
            return new RebuildOneResult(syncResult.changed(), wasNationwide,
                    resolution.scope() == com.themoa.youthcentersearch.policy.region.RegionScope.UNKNOWN,
                    !resolution.regionCodes().isEmpty() && !resolution.regionCodes().contains("KR"), queued);
        });
    }

    private Map<String, Object> fields(Policy policy) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", policy.getTitle());
        fields.put("plcyNm", policy.getTitle());
        fields.put("summary", policy.getSummary());
        fields.put("plcyExplnCn", policy.getSummary());
        fields.put("agencyName", policy.getAgencyName());
        fields.put("sprvsnInstCdNm", policy.getAgencyName());
        if (policy.getCondition() != null) {
            fields.put("conditionSummary", policy.getCondition().getConditionSummary());
            fields.put("ptcpPrpTrgtCn", policy.getCondition().getConditionSummary());
        }
        return fields;
    }

    private record RebuildOneResult(boolean changed, boolean wasNationwide, boolean nowUnknown, boolean nowRegion, boolean queued) {
    }
}
