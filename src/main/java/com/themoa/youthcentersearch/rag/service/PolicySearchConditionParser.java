package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;

public interface PolicySearchConditionParser {
    ParsedPolicySearchCondition parse(String query, Integer resultSize);

    record ParsedPolicySearchCondition(
            PolicySearchCondition condition,
            PolicyQuerySemantics semantics,
            String parserMode,
            boolean fallback,
            String fallbackReason
    ) {
        public ParsedPolicySearchCondition(PolicySearchCondition condition, String parserMode, boolean fallback,
                                           String fallbackReason) {
            this(condition, PolicyQuerySemantics.empty(), parserMode, fallback, fallbackReason);
        }
    }
}
