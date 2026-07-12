package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;

public interface PolicySearchConditionParser {
    ParsedPolicySearchCondition parse(String query, Integer resultSize);

    record ParsedPolicySearchCondition(
            PolicySearchCondition condition,
            String parserMode,
            boolean fallback,
            String fallbackReason
    ) {
    }
}
