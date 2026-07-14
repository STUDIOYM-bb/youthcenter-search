package com.themoa.youthcentersearch.rag.dto;

import java.util.List;

public record PolicySearchResponse(
        String answer,
        PolicySearchCondition interpretedCondition,
        String parserMode,
        boolean fallback,
        String searchMode,
        String queryType,
        int candidateCount,
        int filteredCount,
        long totalMatched,
        int page,
        int size,
        boolean hasNext,
        List<PolicySearchResultItem> results,
        PolicySearchDiagnostics diagnostics
) {
    public PolicySearchResponse(String answer, PolicySearchCondition interpretedCondition, String parserMode,
                                boolean fallback, String searchMode, int candidateCount, int filteredCount,
                                List<PolicySearchResultItem> results, PolicySearchDiagnostics diagnostics) {
        this(answer, interpretedCondition, parserMode, fallback, searchMode, searchMode, candidateCount,
                filteredCount, results == null ? 0 : results.size(), 0, results == null ? 0 : results.size(),
                false, results, diagnostics);
    }
}
