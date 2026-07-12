package com.themoa.youthcentersearch.youthcenter.dto.response;

import com.themoa.youthcentersearch.youthcenter.dto.parsed.SchemaAnalysis;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;

import java.util.List;

public record YouthPolicyListResponse(
        String sourceMode,
        String maskedRequestUrl,
        int statusCode,
        String contentType,
        ResponseType responseType,
        boolean redirected,
        String redirectLocation,
        long elapsedTimeMs,
        int responseLength,
        String responsePreview,
        String rawResponseFilePath,
        boolean listNodeFound,
        String listNodePath,
        int parsedCount,
        Integer totalCount,
        Integer currentPage,
        Integer pageSize,
        YouthPolicyView firstPolicy,
        List<YouthPolicyView> policies,
        SchemaAnalysis schemaAnalysis
) {
}
