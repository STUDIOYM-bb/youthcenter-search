package com.themoa.youthcentersearch.youthcenter.dto.response;

import com.themoa.youthcentersearch.youthcenter.dto.parsed.SchemaAnalysis;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyView;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;

import java.util.List;
import java.util.Map;

public record YouthCenterProbeResponse(
        String sourceMode,
        boolean apiKeyConfigured,
        String maskedRequestUrl,
        Map<String, Object> requestParameters,
        int statusCode,
        String contentType,
        ResponseType responseType,
        boolean redirected,
        String redirectLocation,
        List<String> redirectHistory,
        String finalUrl,
        List<String> warnings,
        long elapsedTimeMs,
        int responseLength,
        String responsePreview,
        String rawResponseFilePath,
        boolean errorResponse,
        String errorCode,
        String errorMessage,
        boolean listNodeFound,
        String listNodePath,
        int parsedCount,
        Integer totalCount,
        Integer currentPage,
        Integer pageSize,
        YouthPolicyView firstPolicy,
        SchemaAnalysis schemaAnalysis
) {
}
