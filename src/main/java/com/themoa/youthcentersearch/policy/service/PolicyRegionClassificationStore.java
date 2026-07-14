package com.themoa.youthcentersearch.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyRegionClassification;
import com.themoa.youthcentersearch.policy.region.PolicyRegionClassificationResult;
import com.themoa.youthcentersearch.policy.repository.PolicyRegionClassificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PolicyRegionClassificationStore {
    private final PolicyRegionClassificationRepository repository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public PolicyRegionClassificationStore(PolicyRegionClassificationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(Policy policy, PolicyRegionClassificationResult result) {
        String evidenceJson = evidenceJson(result);
        BigDecimal confidence = BigDecimal.valueOf(result.confidence()).setScale(4, RoundingMode.HALF_UP);
        var existing = repository.findById(policy.getId());
        PolicyRegionClassification classification = existing
                .orElseGet(() -> new PolicyRegionClassification(policy, result.scope().name(), confidence,
                        evidenceJson, result.classifierVersion(), result.needsReview()));
        classification.update(result.scope().name(), confidence, evidenceJson,
                result.classifierVersion(), result.needsReview());
        if (existing.isEmpty()) {
            entityManager.persist(classification);
        }
    }

    private String evidenceJson(PolicyRegionClassificationResult result) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "evidence", result.evidence(),
                    "conflictingEvidence", result.conflictingEvidence()
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Policy region classification evidence serialization failed", ex);
        }
    }
}
