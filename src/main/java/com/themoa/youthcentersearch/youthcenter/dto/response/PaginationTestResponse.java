package com.themoa.youthcentersearch.youthcenter.dto.response;

import java.util.List;

public record PaginationTestResponse(
        int requestedPages,
        int apiRequestCount,
        int parsedPolicyCount,
        int uniquePolicyCount,
        int duplicatePolicyCount,
        boolean repeatedPageDetected,
        String stopReason,
        List<PageResult> pages
) {
    public record PageResult(
            int page,
            int statusCode,
            int receivedCount,
            String firstPolicyNumber,
            String lastPolicyNumber,
            boolean duplicate,
            long elapsedTimeMs
    ) {
    }
}
