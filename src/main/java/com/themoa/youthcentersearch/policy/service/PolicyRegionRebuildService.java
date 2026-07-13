package com.themoa.youthcentersearch.policy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.admin.service.JobProgressUpdate;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.region.PolicyRegionResolver;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import com.themoa.youthcentersearch.rag.service.PolicyDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public PolicyRegionRebuildService(PolicyRepository policyRepository,
                                      PolicyRegionResolver regionResolver,
                                      PolicyRegionSyncService regionSyncService,
                                      PolicyEmbeddingSyncRepository syncRepository,
                                      PolicyDocumentBuilder documentBuilder,
                                      RegionRebuildProperties properties,
                                      TransactionTemplate transactionTemplate,
                                      PolicySourceSnapshotRepository snapshotRepository,
                                      ObjectMapper objectMapper) {
        this.policyRepository = policyRepository;
        this.regionResolver = regionResolver;
        this.regionSyncService = regionSyncService;
        this.syncRepository = syncRepository;
        this.documentBuilder = documentBuilder;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    public PolicyRegionRebuildResult rebuildAll() {
        return rebuildAll(null);
    }

    public PolicyRegionRebuildResult rebuildAll(Consumer<JobProgressUpdate> progressConsumer) {
        long total = policyRepository.countByActiveTrue();
        long processed = 0;
        long changed = 0;
        long nationwideToRegion = 0;
        long nationwideToUnknown = 0;
        long unchanged = 0;
        long failed = 0;
        long pendingQueued = 0;
        long snapshotUsed = 0;
        long snapshotMissing = 0;
        long fallbackUsed = 0;
        long reviewRequired = 0;
        int page = 0;
        int totalBatches = (int) Math.ceil((double) total / Math.max(1, properties.getBatchSize()));
        while (true) {
            var ids = policyRepository.findActivePolicyIds(PageRequest.of(page++, Math.max(1, properties.getBatchSize())));
            if (ids.isEmpty()) {
                break;
            }
            for (Integer id : ids) {
                try {
                    notify(progressConsumer, new JobProgressUpdate("REBUILDING", "지역 재계산 중", total, processed,
                            changed, failed, 0, 0, page, totalBatches, String.valueOf(id), 0, 0,
                            "정책 지역을 다시 계산합니다."));
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
                    if (result.snapshotUsed()) snapshotUsed++;
                    if (result.snapshotMissing()) snapshotMissing++;
                    if (result.fallbackUsed()) fallbackUsed++;
                    if (result.reviewRequired()) reviewRequired++;
                } catch (RuntimeException ex) {
                    failed++;
                    log.warn("Policy region rebuild failed. policyId={}", id, ex);
                }
            }
            notify(progressConsumer, new JobProgressUpdate("REBUILDING", "지역 재계산 중", total, processed,
                    changed, failed, 0, 0, page, totalBatches, null, 0, 0, "지역 재계산 배치 완료"));
        }
        return new PolicyRegionRebuildResult(total, processed, changed, nationwideToRegion, nationwideToUnknown,
                unchanged, failed, pendingQueued, snapshotUsed, snapshotMissing, fallbackUsed, reviewRequired);
    }

    private RebuildOneResult rebuildOne(Integer policyId) {
        return transactionTemplate.execute(status -> {
            Policy policy = policyRepository.findWithRelationsByIdIn(java.util.List.of(policyId)).stream()
                    .findFirst().orElseThrow();
            Set<String> before = policy.getRegions().stream()
                    .map(region -> region.getRegion().getRegionCode())
                    .collect(Collectors.toSet());
            boolean wasNationwide = before.contains("KR");
            FieldSource fieldSource = fields(policy);
            var resolution = regionResolver.resolve(fieldSource.fields());
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
                    !resolution.regionCodes().isEmpty() && !resolution.regionCodes().contains("KR"), queued,
                    fieldSource.snapshotUsed(), fieldSource.snapshotMissing(), fieldSource.fallbackUsed(),
                    fieldSource.snapshotMissing() || resolution.needsReview());
        });
    }

    private FieldSource fields(Policy policy) {
        var snapshot = snapshotRepository.findByPolicyId(policy.getId());
        if (snapshot.isPresent()) {
            try {
                Map<String, Object> fields = objectMapper.readValue(snapshot.get().getRawPolicyJson(), new TypeReference<>() {
                });
                return new FieldSource(fields, true, false, false);
            } catch (Exception ex) {
                log.warn("Policy source snapshot parse failed. policyId={}", policy.getId(), ex);
            }
        }
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
        return new FieldSource(fields, false, true, true);
    }

    private void notify(Consumer<JobProgressUpdate> consumer, JobProgressUpdate update) {
        if (consumer != null) {
            consumer.accept(update);
        }
    }

    private record FieldSource(Map<String, Object> fields, boolean snapshotUsed, boolean snapshotMissing, boolean fallbackUsed) {
    }

    private record RebuildOneResult(boolean changed, boolean wasNationwide, boolean nowUnknown, boolean nowRegion, boolean queued,
                                    boolean snapshotUsed, boolean snapshotMissing, boolean fallbackUsed, boolean reviewRequired) {
    }
}
