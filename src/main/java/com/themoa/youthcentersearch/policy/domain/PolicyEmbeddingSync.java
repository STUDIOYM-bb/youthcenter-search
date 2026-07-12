package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_embedding_sync")
public class PolicyEmbeddingSync {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    private Policy policy;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    protected PolicyEmbeddingSync() {
    }

    public PolicyEmbeddingSync(Policy policy, String contentHash) {
        this.policy = policy;
        queue(contentHash);
        this.retryCount = 0;
    }

    public void queue(String contentHash) {
        this.contentHash = contentHash;
        this.syncStatus = "PENDING";
        this.lastError = null;
        this.requestedAt = LocalDateTime.now();
        this.syncedAt = null;
    }

    public void processing() {
        this.syncStatus = "PROCESSING";
    }

    public void synced() {
        this.syncStatus = "SYNCED";
        this.syncedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void failed(String message) {
        this.syncStatus = "FAILED";
        this.retryCount++;
        this.lastError = message == null || message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public Long getId() {
        return id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getSyncStatus() {
        return syncStatus;
    }
}
