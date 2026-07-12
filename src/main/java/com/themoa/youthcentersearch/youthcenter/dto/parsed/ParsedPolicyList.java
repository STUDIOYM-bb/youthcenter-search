package com.themoa.youthcentersearch.youthcenter.dto.parsed;

import java.util.List;

public record ParsedPolicyList(
        boolean listNodeFound,
        String listNodePath,
        List<YouthPolicyItem> policies,
        Integer totalCount,
        Integer currentPage,
        Integer pageSize,
        SchemaAnalysis schemaAnalysis,
        boolean apiError,
        String errorCode,
        String errorMessage
) {
}
