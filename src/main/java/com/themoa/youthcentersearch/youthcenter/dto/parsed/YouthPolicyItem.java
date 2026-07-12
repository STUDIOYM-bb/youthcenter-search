package com.themoa.youthcentersearch.youthcenter.dto.parsed;

import java.util.LinkedHashMap;
import java.util.Map;

public record YouthPolicyItem(
        String policyNumber,
        String policyName,
        String policyDescription,
        String keywordNames,
        Map<String, Object> fields
) {
    public YouthPolicyItem {
        fields = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
    }
}
