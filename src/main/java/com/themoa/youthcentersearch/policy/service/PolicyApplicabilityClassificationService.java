package com.themoa.youthcentersearch.policy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import com.themoa.youthcentersearch.policy.region.PolicyGeographyClassifier;
import com.themoa.youthcentersearch.policy.region.PolicyRegionClassificationResult;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import com.themoa.youthcentersearch.rag.service.PolicyDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 정책의 신청 가능 지역을 판정하는 단일 진입점이다.
 * 수집 직후와 관리자 재분류가 같은 경로를 사용해야 policy_region, 분류 evidence,
 * 부분 재임베딩 큐 상태가 서로 어긋나지 않는다.
 */
@Service
public class PolicyApplicabilityClassificationService {
    private static final Logger log = LoggerFactory.getLogger(PolicyApplicabilityClassificationService.class);

    private final PolicyGeographyClassifier geographyClassifier;
    private final PolicyRegionSyncService regionSyncService;
    private final PolicyRegionClassificationStore classificationStore;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final PolicyDocumentBuilder documentBuilder;
    private final ObjectMapper objectMapper;

    public PolicyApplicabilityClassificationService(PolicyGeographyClassifier geographyClassifier,
                                                    PolicyRegionSyncService regionSyncService,
                                                    PolicyRegionClassificationStore classificationStore,
                                                    PolicySourceSnapshotRepository snapshotRepository,
                                                    PolicyEmbeddingSyncRepository syncRepository,
                                                    PolicyDocumentBuilder documentBuilder,
                                                    ObjectMapper objectMapper) {
        this.geographyClassifier = geographyClassifier;
        this.regionSyncService = regionSyncService;
        this.classificationStore = classificationStore;
        this.snapshotRepository = snapshotRepository;
        this.syncRepository = syncRepository;
        this.documentBuilder = documentBuilder;
        this.objectMapper = objectMapper;
    }

    /**
     * 수집 직후 이미 파싱된 원문 필드로 지역을 판정한다.
     * policy_region은 기존 값과 병합하지 않고 이번 판정 결과로 교체한다.
     */
    public ApplicabilityClassificationResult classifyFromFields(Policy policy, Map<String, Object> fields,
                                                                boolean enqueueChangedPolicy) {
        PolicyRegionClassificationResult classification = geographyClassifier.classify(fields);
        classificationStore.save(policy, classification);
        var syncResult = regionSyncService.syncRegions(policy, classification.toResolution());
        boolean queued = syncResult.changed() && enqueueChangedPolicy && queueEmbedding(policy);
        return new ApplicabilityClassificationResult(classification, syncResult.changed(),
                syncResult.addedCount(), syncResult.removedCount(), queued, false, false);
    }

    /**
     * 관리자 재분류는 기존 policy_region을 신뢰하지 않고 Snapshot 원문을 우선 사용한다.
     * Snapshot이 없거나 파싱이 실패한 경우에만 현재 Policy 필드로 제한된 fallback을 사용한다.
     */
    public ApplicabilityClassificationResult classifyFromSnapshot(Policy policy, boolean enqueueChangedPolicy) {
        FieldSource source = fields(policy);
        ApplicabilityClassificationResult result = classifyFromFields(policy, source.fields(), enqueueChangedPolicy);
        return new ApplicabilityClassificationResult(result.classification(), result.changed(), result.addedCount(),
                result.removedCount(), result.embeddingQueued(), source.snapshotUsed(), source.snapshotMissing());
    }

    private boolean queueEmbedding(Policy policy) {
        String contentHash = documentBuilder.build(policy).contentHash();
        PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policy.getId()).orElse(null);
        if (sync == null) {
            syncRepository.save(new PolicyEmbeddingSync(policy, contentHash));
        } else {
            sync.queue(contentHash);
        }
        return true;
    }

    private FieldSource fields(Policy policy) {
        var snapshot = snapshotRepository.findByPolicyId(policy.getId());
        if (snapshot.isPresent()) {
            try {
                Map<String, Object> fields = objectMapper.readValue(snapshot.get().getRawPolicyJson(), new TypeReference<>() {
                });
                return new FieldSource(fields, true, false);
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
        return new FieldSource(fields, false, true);
    }

    private record FieldSource(Map<String, Object> fields, boolean snapshotUsed, boolean snapshotMissing) {
    }

    public record ApplicabilityClassificationResult(PolicyRegionClassificationResult classification,
                                                    boolean changed,
                                                    int addedCount,
                                                    int removedCount,
                                                    boolean embeddingQueued,
                                                    boolean snapshotUsed,
                                                    boolean snapshotMissing) {
    }
}
