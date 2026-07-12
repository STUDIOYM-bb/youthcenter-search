package com.themoa.youthcentersearch.youthcenter.dto.parsed;

import java.util.List;

public record ParsedPolicyDetail(
        YouthPolicyItem policy,
        SchemaAnalysis schemaAnalysis,
        boolean apiError,
        String errorCode,
        String errorMessage,
        List<String> warnings
) {
    public ParsedPolicyDetail(YouthPolicyItem policy, SchemaAnalysis schemaAnalysis, boolean apiError,
                              String errorCode, String errorMessage) {
        this(policy, schemaAnalysis, apiError, errorCode, errorMessage, List.of());
    }
}
