package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PolicyDocumentMetadataBuilder {
    public Map<String, Object> metadata(Policy policy, String contentHash) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        put(metadata, "policyId", policy.getId());
        put(metadata, "sourcePolicyId", policy.getSourcePolicyId());
        put(metadata, "source", policy.getSourceType());
        put(metadata, "title", policy.getTitle());
        put(metadata, "category", policy.getCategory() == null ? null : policy.getCategory().name());
        put(metadata, "agencyName", policy.getAgencyName());
        put(metadata, "regionCodes", policy.getRegions().stream().map(region -> region.getRegion().getRegionCode()).toList());
        put(metadata, "regionNames", policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList());
        put(metadata, "regionScope", regionScope(policy));
        PolicyCondition condition = policy.getCondition();
        if (condition != null) {
            put(metadata, "minimumAge", condition.getMinAge());
            put(metadata, "maximumAge", condition.getMaxAge());
            put(metadata, "employmentStatus", condition.getEmploymentStatus());
            put(metadata, "studentStatus", condition.getStudentStatus());
        }
        put(metadata, "applicationStatus", policy.getStatus());
        put(metadata, "startDate", policy.getStartDate() == null ? null : policy.getStartDate().toString());
        put(metadata, "dueDate", policy.getDueDate() == null ? null : policy.getDueDate().toString());
        put(metadata, "alwaysOpen", policy.isAlwaysOpen() ? 1 : 0);
        put(metadata, "active", policy.isActive() ? 1 : 0);
        put(metadata, "contentHash", contentHash);
        put(metadata, "officialUrl", policy.getOfficialUrl());
        return metadata;
    }

    private String regionScope(Policy policy) {
        List<String> codes = policy.getRegions().stream().map(region -> region.getRegion().getRegionCode()).toList();
        if (codes.contains("KR")) return "NATIONWIDE";
        if (codes.isEmpty()) return "UNKNOWN";
        if (codes.size() > 1) return "MULTIPLE";
        String code = codes.get(0);
        if (code.length() == 2) return "PROVINCE";
        if (code.length() == 5) return "CITY";
        return "UNKNOWN";
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
