package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.RegionScope;
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
        List<RegionCode> regions = policy.getRegions().stream().map(region -> region.getRegion()).toList();
        RegionScope scope = regionScope(regions);
        put(metadata, "regionScope", scope.name());
        put(metadata, "regionCodes", regions.stream().map(RegionCode::getRegionCode).toList());
        put(metadata, "regionNames", regions.stream().map(RegionCode::displayName).toList());
        put(metadata, "provinceNames", regions.stream().map(RegionCode::getProvince).distinct().toList());
        put(metadata, "cityNames", regions.stream().map(this::cityName).filter(java.util.Objects::nonNull).distinct().toList());
        put(metadata, "districtNames", regions.stream().filter(region -> "DISTRICT".equals(region.getRegionLevel()))
                .map(this::districtName).filter(java.util.Objects::nonNull).distinct().toList());
        put(metadata, "nationwide", scope == RegionScope.NATIONWIDE);
        put(metadata, "regionUnknown", scope == RegionScope.UNKNOWN);
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

    private RegionScope regionScope(List<RegionCode> regions) {
        if (regions.isEmpty()) return RegionScope.UNKNOWN;
        if (regions.stream().anyMatch(region -> "KR".equals(region.getRegionCode()))) return RegionScope.NATIONWIDE;
        if (regions.size() > 1) return RegionScope.MULTIPLE;
        return switch (regions.get(0).getRegionLevel()) {
            case "PROVINCE" -> RegionScope.PROVINCE;
            case "CITY" -> RegionScope.CITY;
            case "DISTRICT" -> RegionScope.DISTRICT;
            default -> RegionScope.UNKNOWN;
        };
    }

    private String cityName(RegionCode region) {
        if (region.getCity() == null) {
            return null;
        }
        int idx = region.getCity().indexOf(' ');
        return idx > 0 ? region.getCity().substring(0, idx) : region.getCity();
    }

    private String districtName(RegionCode region) {
        if (region.getCity() == null) {
            return null;
        }
        int idx = region.getCity().indexOf(' ');
        return idx > 0 ? region.getCity().substring(idx + 1) : region.getCity();
    }

    private void put(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
