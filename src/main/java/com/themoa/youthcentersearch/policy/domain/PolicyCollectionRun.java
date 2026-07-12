package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_collection_run")
public class PolicyCollectionRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "execution_type", nullable = false, length = 30)
    private String executionType;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "requested_page_count", nullable = false)
    private int requestedPageCount;

    @Column(name = "api_request_count", nullable = false)
    private int apiRequestCount;

    @Column(name = "received_count", nullable = false)
    private int receivedCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "representative_error", length = 1000)
    private String representativeError;

    @Column(name = "failed_page")
    private Integer failedPage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    protected PolicyCollectionRun() {
    }

    public PolicyCollectionRun(String source, String executionType) {
        this.source = source;
        this.executionType = executionType;
        this.startedAt = LocalDateTime.now();
        this.status = "RUNNING";
    }

    public void complete(String status) {
        this.status = status;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(Integer page, String message) {
        this.status = "FAILED";
        this.failedPage = page;
        this.representativeError = truncate(message, 1000);
        this.completedAt = LocalDateTime.now();
    }

    public void addPage(int received) {
        this.requestedPageCount++;
        this.apiRequestCount++;
        this.receivedCount += received;
    }

    public void inserted() {
        this.insertedCount++;
    }

    public void updated() {
        this.updatedCount++;
    }

    public void failed() {
        this.failedCount++;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public int getReceivedCount() {
        return receivedCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
