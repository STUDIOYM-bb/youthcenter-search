package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.admin.service.JobProgressUpdate;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class PolicyRegionRebuildService {
    private static final Logger log = LoggerFactory.getLogger(PolicyRegionRebuildService.class);

    private final PolicyRepository policyRepository;
    private final RegionRebuildProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final PolicyApplicabilityClassificationService applicabilityClassificationService;

    public PolicyRegionRebuildService(PolicyRepository policyRepository,
                                      RegionRebuildProperties properties,
                                      TransactionTemplate transactionTemplate,
                                      PolicyApplicabilityClassificationService applicabilityClassificationService) {
        this.policyRepository = policyRepository;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.applicabilityClassificationService = applicabilityClassificationService;
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
            var result = applicabilityClassificationService.classifyFromSnapshot(policy, properties.isEnqueueChangedPolicies());
            var resolution = result.classification().toResolution();
            return new RebuildOneResult(result.changed(), wasNationwide,
                    resolution.scope() == com.themoa.youthcentersearch.policy.region.RegionScope.UNKNOWN,
                    !resolution.regionCodes().isEmpty() && !resolution.regionCodes().contains("KR"), result.embeddingQueued(),
                    result.snapshotUsed(), result.snapshotMissing(), result.snapshotMissing(),
                    result.snapshotMissing() || resolution.needsReview());
        });
    }

    private void notify(Consumer<JobProgressUpdate> consumer, JobProgressUpdate update) {
        if (consumer != null) {
            consumer.accept(update);
        }
    }

    private record RebuildOneResult(boolean changed, boolean wasNationwide, boolean nowUnknown, boolean nowRegion, boolean queued,
                                    boolean snapshotUsed, boolean snapshotMissing, boolean fallbackUsed, boolean reviewRequired) {
    }
}
