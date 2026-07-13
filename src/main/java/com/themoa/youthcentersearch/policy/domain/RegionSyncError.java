package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "region_sync_error")
public class RegionSyncError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_run_id", nullable = false)
    private RegionSyncRun syncRun;

    @Column(name = "province_code", length = 50)
    private String provinceCode;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Column(name = "error_type", nullable = false, length = 100)
    private String errorType;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected RegionSyncError() {
    }

    public RegionSyncError(RegionSyncRun syncRun, String provinceCode, String provinceName, String errorType, String errorMessage, int retryCount) {
        this.syncRun = syncRun;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.errorType = errorType;
        this.errorMessage = errorMessage == null ? "" : errorMessage.substring(0, Math.min(1000, errorMessage.length()));
        this.retryCount = retryCount;
        this.occurredAt = LocalDateTime.now();
    }
}
