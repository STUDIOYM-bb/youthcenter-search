package com.themoa.youthcentersearch.youthcenter.search;

public record YouthCenterSearchExecutionResult(
        YouthCenterSearchPlan plan,
        int pagesRequested,
        int receivedCount,
        boolean succeeded,
        Integer statusCode,
        String responseType,
        String errorMessage
) {
}
