package com.themoa.youthcentersearch.rag.dto;

import java.util.List;

public record PolicySearchResponse(
        String answer,
        PolicySearchCondition interpretedCondition,
        String parserMode,
        boolean fallback,
        String searchMode,
        int candidateCount,
        int filteredCount,
        List<PolicySearchResultItem> results,
        PolicySearchDiagnostics diagnostics
) {
}
