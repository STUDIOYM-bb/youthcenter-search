package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_source_snapshot")
public class PolicySourceSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_data_id")
    private PolicyRawData rawData;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_policy_id", nullable = false, length = 150)
    private String sourcePolicyId;

    @Column(name = "raw_policy_json", nullable = false, columnDefinition = "longtext")
    private String rawPolicyJson;

    @Column(name = "raw_content_hash", nullable = false, length = 64)
    private String rawContentHash;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PolicySourceSnapshot() {
    }

    public PolicySourceSnapshot(Policy policy, PolicyRawData rawData, String source, String sourcePolicyId,
                                String rawPolicyJson, String rawContentHash) {
        this.policy = policy;
        this.rawData = rawData;
        this.source = source;
        this.sourcePolicyId = sourcePolicyId;
        this.rawPolicyJson = rawPolicyJson;
        this.rawContentHash = rawContentHash;
        this.collectedAt = LocalDateTime.now();
        this.updatedAt = this.collectedAt;
    }

    public void update(PolicyRawData rawData, String rawPolicyJson, String rawContentHash) {
        this.rawData = rawData;
        this.rawPolicyJson = rawPolicyJson;
        this.rawContentHash = rawContentHash;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public PolicyRawData getRawData() {
        return rawData;
    }

    public String getSource() {
        return source;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public String getRawPolicyJson() {
        return rawPolicyJson;
    }

    public String getRawContentHash() {
        return rawContentHash;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
