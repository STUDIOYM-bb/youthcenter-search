package com.themoa.youthcentersearch.youthcenter.dto.response;

import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;

public record YouthCenterFilterProbeResponse(
        String filterType,
        String filterValue,
        String maskedRequestUrl,
        int statusCode,
        String contentType,
        ResponseType responseType,
        int parsedCount,
        Integer totalCount,
        long elapsedTimeMs,
        String errorMessage
) {
}
