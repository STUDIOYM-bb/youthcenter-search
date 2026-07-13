package com.themoa.youthcentersearch.admin.dto;

import java.time.Instant;

public record AdminJobStatus(
        String jobId,
        String jobType,
        String status,
        String stage,
        String stageLabel,
        Integer overallProgressPercent,
        Integer stageProgressPercent,
        boolean indeterminate,
        long totalCount,
        long processedCount,
        long successCount,
        long failedCount,
        long remainingCount,
        int currentPage,
        int totalPages,
        int currentBatch,
        int totalBatches,
        String currentItem,
        long apiRequestCount,
        long retryCount,
        Instant startedAt,
        Instant updatedAt,
        Instant completedAt,
        long elapsedTimeMs,
        Long estimatedRemainingSeconds,
        Double throughputPerSecond,
        String message
) {
    public AdminJobStatus(String jobId,
                          String jobType,
                          String status,
                          long totalCount,
                          long processedCount,
                          long successCount,
                          long failedCount,
                          long remainingCount,
                          int currentPage,
                          int currentBatch,
                          String message) {
        this(jobId, jobType, status, null, null, null, null, totalCount <= 0,
                totalCount, processedCount, successCount, failedCount, remainingCount,
                currentPage, 0, currentBatch, 0, null, 0, 0, null, null, null, 0, null, null, message);
    }
}
