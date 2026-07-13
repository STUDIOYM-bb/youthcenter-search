package com.themoa.youthcentersearch.admin.service;

public record JobProgressUpdate(
        String stage,
        String stageLabel,
        long total,
        long processed,
        long success,
        long failed,
        int currentPage,
        int totalPages,
        int currentBatch,
        int totalBatches,
        String currentItem,
        long apiRequestCount,
        long retryCount,
        String message
) {
    public static JobProgressUpdate of(String stage, String stageLabel, long total, long processed, String message) {
        return new JobProgressUpdate(stage, stageLabel, total, processed, 0, 0, 0, 0, 0, 0, null, 0, 0, message);
    }
}
