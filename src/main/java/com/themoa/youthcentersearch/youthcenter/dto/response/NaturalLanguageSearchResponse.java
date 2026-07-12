package com.themoa.youthcentersearch.youthcenter.dto.response;

import java.util.List;

public record NaturalLanguageSearchResponse(
        String originalQuery,
        ParserMode parserMode,
        boolean fallback,
        String fallbackReason,
        NaturalLanguagePolicyCondition interpretedCondition,
        List<GeneratedApiRequest> generatedApiRequests,
        int apiRequestCount,
        int successfulApiRequestCount,
        int failedApiRequestCount,
        int apiReceivedCount,
        int uniquePolicyCount,
        int finalResultCount,
        long elapsedTimeMs,
        boolean partialSuccess,
        List<String> warnings,
        List<NaturalLanguagePolicyResult> results
) {
}
