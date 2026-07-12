package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_collection_error")
public class PolicyCollectionError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_run_id")
    private PolicyCollectionRun collectionRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_data_id")
    private PolicyRawData rawData;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "failed_page")
    private Integer failedPage;

    @Column(name = "source_policy_id", length = 150)
    private String sourcePolicyId;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected PolicyCollectionError() {
    }

    public PolicyCollectionError(PolicyCollectionRun collectionRun, PolicyRawData rawData, String source,
                                 Integer failedPage, String sourcePolicyId, String errorType, String errorMessage) {
        this.collectionRun = collectionRun;
        this.rawData = rawData;
        this.source = source;
        this.failedPage = failedPage;
        this.sourcePolicyId = sourcePolicyId;
        this.errorType = errorType;
        this.errorMessage = errorMessage == null || errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
        this.occurredAt = LocalDateTime.now();
    }
}
