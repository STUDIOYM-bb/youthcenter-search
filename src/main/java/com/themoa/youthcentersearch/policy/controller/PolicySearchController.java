package com.themoa.youthcentersearch.policy.controller;

import com.themoa.youthcentersearch.common.response.ApiResponse;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyRawData;
import com.themoa.youthcentersearch.policy.domain.PolicySourceSnapshot;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResponse;
import com.themoa.youthcentersearch.rag.service.PolicyRagSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/policies")
public class PolicySearchController {
    private final PolicyRagSearchService searchService;
    private final PolicyRepository policyRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;

    public PolicySearchController(PolicyRagSearchService searchService, PolicyRepository policyRepository,
                                  PolicySourceSnapshotRepository snapshotRepository) {
        this.searchService = searchService;
        this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @PostMapping("/search")
    public ApiResponse<PolicySearchResponse> search(@Valid @RequestBody PolicySearchRequest request) {
        return ApiResponse.ok(searchService.search(request));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Integer policyId) {
        Policy policy = policyRepository.findWithRelationsByIdIn(java.util.List.of(policyId)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));
        return ApiResponse.ok(Map.of(
                "policyId", policy.getId(),
                "sourcePolicyId", policy.getSourcePolicyId(),
                "title", policy.getTitle(),
                "category", policy.getCategory().name(),
                "agencyName", policy.getAgencyName(),
                "summary", policy.getSummary() == null ? "" : policy.getSummary(),
                "officialUrl", policy.getOfficialUrl() == null ? "" : policy.getOfficialUrl(),
                "status", policy.getStatus(),
                "regions", policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList()
        ));
    }

    @GetMapping("/{policyId}/raw")
    public ApiResponse<Map<String, Object>> raw(@PathVariable Integer policyId) {
        Policy policy = policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));
        PolicySourceSnapshot snapshot = snapshotRepository.findByPolicyId(policyId)
                .orElseThrow(() -> new IllegalArgumentException("정책별 원본 Snapshot을 찾을 수 없습니다. 전체 정책 수집을 다시 실행하세요."));
        PolicyRawData raw = snapshot.getRawData();
        Map<String, Object> pageRawData = raw == null ? Map.of() : Map.of(
                "rawDataId", raw.getId(),
                "requestUrl", raw.getRequestUrl(),
                "responseFormat", raw.getResponseFormat(),
                "collectedAt", raw.getCollectedAt().toString()
        );
        return ApiResponse.ok(Map.of(
                "policyId", policy.getId(),
                "sourcePolicyId", snapshot.getSourcePolicyId(),
                "source", snapshot.getSource(),
                "rawPolicy", snapshot.getRawPolicyJson(),
                "pageRawData", pageRawData
        ));
    }
}
