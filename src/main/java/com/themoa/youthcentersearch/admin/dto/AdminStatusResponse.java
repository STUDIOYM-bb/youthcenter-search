package com.themoa.youthcentersearch.admin.dto;

public record AdminStatusResponse(
        String springBoot,
        boolean secretConfigFileFound,
        boolean youthCenterApiKeyConfigured,
        boolean openAiApiKeyConfigured,
        String springAiChatModel,
        String springAiEmbeddingModel,
        boolean chatModelAvailable,
        boolean embeddingModelAvailable,
        boolean mysql,
        boolean mysqlAvailable,
        boolean qdrant,
        boolean qdrantAvailable,
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
        Long qdrantDocumentCount,
        long nationwidePolicyCount,
        long provincePolicyCount,
        long cityPolicyCount,
        long districtPolicyCount,
        long multiplePolicyCount,
        long unknownPolicyCount
) {
}
