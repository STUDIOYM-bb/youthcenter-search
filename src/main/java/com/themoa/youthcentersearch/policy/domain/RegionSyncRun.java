package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "region_sync_run")
public class RegionSyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "api_province_count", nullable = false)
    private int apiProvinceCount;

    @Column(name = "api_municipality_count", nullable = false)
    private int apiMunicipalityCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "unchanged_count", nullable = false)
    private int unchangedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "current_province_code", length = 50)
    private String currentProvinceCode;

    @Column(name = "current_province_name", length = 100)
    private String currentProvinceName;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_summary", length = 1000)
    private String errorSummary;

    protected RegionSyncRun() {
    }

    public static RegionSyncRun start() {
        RegionSyncRun run = new RegionSyncRun();
        run.status = "RUNNING";
        run.startedAt = LocalDateTime.now();
        return run;
    }

    public void provinceCount(int count) { this.apiProvinceCount = count; }
    public void current(String code, String name, int processed) {
        this.currentProvinceCode = code;
        this.currentProvinceName = name;
        this.progressPercent = apiProvinceCount <= 0 ? 0 : Math.min(99, (int) Math.floor(processed * 100.0 / apiProvinceCount));
    }
    public void counts(int municipalities, int inserted, int updated, int unchanged, int failed) {
        this.apiMunicipalityCount = municipalities;
        this.insertedCount = inserted;
        this.updatedCount = updated;
        this.unchangedCount = unchanged;
        this.failedCount = failed;
    }
    public void complete(String status, String errorSummary) {
        this.status = status;
        this.errorSummary = errorSummary;
        this.completedAt = LocalDateTime.now();
        this.progressPercent = 100;
    }
    public Long getId() { return id; }
    public String getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public int getApiProvinceCount() { return apiProvinceCount; }
    public int getApiMunicipalityCount() { return apiMunicipalityCount; }
    public int getInsertedCount() { return insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getUnchangedCount() { return unchangedCount; }
    public int getFailedCount() { return failedCount; }
    public int getProgressPercent() { return progressPercent; }
    public String getCurrentProvinceCode() { return currentProvinceCode; }
    public String getCurrentProvinceName() { return currentProvinceName; }
    public String getErrorSummary() { return errorSummary; }
}
