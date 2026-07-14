package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy_region_classification")
public class PolicyRegionClassification {
    @Id
    @Column(name = "policy_id")
    private Integer policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(name = "region_scope", nullable = false, length = 30)
    private String regionScope;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "evidence_json", nullable = false, columnDefinition = "longtext")
    private String evidenceJson;

    @Column(name = "classifier_version", nullable = false, length = 50)
    private String classifierVersion;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @Column(name = "classified_at", nullable = false)
    private LocalDateTime classifiedAt;

    protected PolicyRegionClassification() {
    }

    public PolicyRegionClassification(Policy policy, String regionScope, BigDecimal confidence,
                                      String evidenceJson, String classifierVersion, boolean needsReview) {
        this.policy = policy;
        this.policyId = policy.getId();
        update(regionScope, confidence, evidenceJson, classifierVersion, needsReview);
    }

    public void update(String regionScope, BigDecimal confidence, String evidenceJson,
                       String classifierVersion, boolean needsReview) {
        this.regionScope = regionScope;
        this.confidence = confidence;
        this.evidenceJson = evidenceJson;
        this.classifierVersion = classifierVersion;
        this.needsReview = needsReview;
        this.classifiedAt = LocalDateTime.now();
    }

    public Integer getPolicyId() {
        return policyId;
    }

    public String getRegionScope() {
        return regionScope;
    }

    public String getClassifierVersion() {
        return classifierVersion;
    }
}
