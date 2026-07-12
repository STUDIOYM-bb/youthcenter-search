package com.themoa.youthcentersearch.admin.dto;

public record AdminJobStatus(
        String jobId,
        String jobType,
        String status,
        long totalCount,
        long processedCount,
        long successCount,
        long failedCount,
        long remainingCount,
        int currentPage,
        int currentBatch,
        String message
) {
}
