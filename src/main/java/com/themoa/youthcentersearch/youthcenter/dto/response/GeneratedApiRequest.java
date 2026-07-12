package com.themoa.youthcentersearch.youthcenter.dto.response;

public record GeneratedApiRequest(
        String filterType,
        String filterValue,
        int pagesRequested,
        int receivedCount,
        boolean succeeded,
        Integer statusCode,
        String responseType,
        String errorMessage
) {
}
