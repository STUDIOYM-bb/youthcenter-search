package com.themoa.youthcentersearch.admin.dto;

public record AdminStatusResponse(
        String springBoot,
        boolean mysql,
        boolean qdrant,
        boolean openAiChatConfigured,
        boolean openAiEmbeddingConfigured,
        boolean ragEnabled,
        String collectionName,
        long totalPolicyCount,
        long activePolicyCount,
        long pendingCount,
        long processingCount,
        long syncedCount,
        long failedCount,
        Long qdrantDocumentCount
) {
}
