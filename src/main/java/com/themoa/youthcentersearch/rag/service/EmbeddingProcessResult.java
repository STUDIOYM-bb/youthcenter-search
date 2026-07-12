package com.themoa.youthcentersearch.rag.service;

public record EmbeddingProcessResult(
        int processedCount,
        int successCount,
        int failedCount,
        long pendingCountAfter,
        String message
) {
}
