package com.themoa.youthcentersearch.admin.controller;

import com.themoa.youthcentersearch.admin.dto.AdminJobStatus;
import com.themoa.youthcentersearch.admin.dto.AdminStatusResponse;
import com.themoa.youthcentersearch.admin.service.AdminJobService;
import com.themoa.youthcentersearch.admin.service.AdminRegionDiagnosticsService;
import com.themoa.youthcentersearch.admin.service.AdminStatusService;
import com.themoa.youthcentersearch.common.config.LocalSecretConfigurationStatus;
import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.common.response.ApiResponse;
import com.themoa.youthcentersearch.policy.region.UserRegionResolution;
import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionSyncRunRepository;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.service.YouthCenterDiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminStatusService statusService;
    private final AdminJobService jobService;
    private final YouthCenterDiagnosticService diagnosticService;
    private final AdminRegionDiagnosticsService regionDiagnosticsService;
    private final UserRegionTextResolver userRegionTextResolver;
    private final RegionCatalog regionCatalog;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionExternalCodeRepository externalCodeRepository;
    private final RegionSyncRunRepository syncRunRepository;
    private final LocalSecretConfigurationStatus configurationStatus;
    private final String adminApiKey;

    public AdminController(AdminStatusService statusService, AdminJobService jobService,
                           YouthCenterDiagnosticService diagnosticService,
                           AdminRegionDiagnosticsService regionDiagnosticsService,
                           UserRegionTextResolver userRegionTextResolver,
                           RegionCatalog regionCatalog,
                           RegionCodeRepository regionCodeRepository,
                           RegionExternalCodeRepository externalCodeRepository,
                           RegionSyncRunRepository syncRunRepository,
                           LocalSecretConfigurationStatus configurationStatus,
                           @Value("${app.admin-api-key:}") String adminApiKey) {
        this.statusService = statusService;
        this.jobService = jobService;
        this.diagnosticService = diagnosticService;
        this.regionDiagnosticsService = regionDiagnosticsService;
        this.userRegionTextResolver = userRegionTextResolver;
        this.regionCatalog = regionCatalog;
        this.regionCodeRepository = regionCodeRepository;
        this.externalCodeRepository = externalCodeRepository;
        this.syncRunRepository = syncRunRepository;
        this.configurationStatus = configurationStatus;
        this.adminApiKey = adminApiKey;
    }

    @GetMapping("/status")
    public ApiResponse<AdminStatusResponse> status(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(statusService.status());
    }

    @PostMapping("/youth-center/probe")
    public ApiResponse<YouthCenterProbeResponse> probe(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(diagnosticService.probe());
    }

    @PostMapping("/jobs/policy-collection")
    public ApiResponse<AdminJobStatus> collect(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("POLICY_COLLECTION"));
    }

    @PostMapping("/jobs/embedding-queue")
    public ApiResponse<AdminJobStatus> queue(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("EMBEDDING_QUEUE"));
    }

    @PostMapping("/jobs/embedding-process")
    public ApiResponse<AdminJobStatus> process(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("EMBEDDING_PROCESS"));
    }

    @PostMapping("/jobs/embedding-retry-failed")
    public ApiResponse<AdminJobStatus> retry(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("EMBEDDING_RETRY_FAILED"));
    }

    @PostMapping("/jobs/policy-region-rebuild")
    public ApiResponse<AdminJobStatus> rebuildRegions(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("POLICY_REGION_REBUILD"));
    }

    @PostMapping("/jobs/region-catalog-sync")
    public ApiResponse<AdminJobStatus> syncRegionCatalog(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("REGION_CATALOG_SYNC"));
    }

    @PostMapping("/jobs/region-catalog-repair")
    public ApiResponse<AdminJobStatus> repairRegionCatalog(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("REGION_CATALOG_REPAIR"));
    }

    @PostMapping("/jobs/full-reindex")
    public ApiResponse<AdminJobStatus> fullReindex(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.start("FULL_REINDEX"));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<AdminJobStatus> job(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                           @PathVariable String jobId) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.find(jobId).orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다.")));
    }

    @GetMapping("/jobs/latest")
    public ApiResponse<AdminJobStatus> latest(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(jobService.latest().orElse(null));
    }

    @PostMapping("/qdrant/search")
    public ApiResponse<String> qdrantSearch(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        if (!configurationStatus.ragEnabled()) {
            throw new YouthCenterApiException("""
                    RAG 기능이 비활성화되어 있습니다.
                    RAG_ENABLED=true 설정을 확인하세요.""");
        }
        return ApiResponse.ok("사용자 검색 API /api/policies/search 에서 Qdrant 검색을 수행합니다.");
    }

    @GetMapping("/regions/anomalies")
    public ApiResponse<java.util.List<com.themoa.youthcentersearch.admin.dto.RegionAnomalyResponse>> regionAnomalies(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(regionDiagnosticsService.anomalies());
    }

    @GetMapping("/regions/resolve")
    public ApiResponse<java.util.Map<String, Object>> resolveRegion(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                                                    @RequestParam("q") String query) {
        requireAdmin(key);
        UserRegionResolution result = userRegionTextResolver.resolve(query);
        java.util.List<java.util.Map<String, String>> externalCodes = java.util.List.of();
        Integer regionId = null;
        if (result.regionCode() != null) {
            var region = regionCodeRepository.findByRegionCode(result.regionCode()).orElse(null);
            if (region != null) {
                regionId = region.getId();
                externalCodes = externalCodeRepository.findByRegionId(region.getId()).stream()
                        .map(code -> java.util.Map.of("codeSystem", code.getCodeSystem(), "externalCode", code.getExternalCode()))
                        .toList();
            }
        }
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("query", query);
        response.put("status", result.status());
        response.put("province", result.province());
        response.put("city", result.city());
        response.put("regionId", regionId);
        response.put("internalCode", result.regionCode());
        response.put("regionName", result.regionName());
        response.put("externalCodes", externalCodes);
        response.put("candidates", result.candidates());
        return ApiResponse.ok(response);
    }

    @GetMapping("/regions/search")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> searchRegions(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestParam("name") String name) {
        requireAdmin(key);
        return ApiResponse.ok(regionCatalog.findInText(name).stream().map(region -> {
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("regionId", region.getId());
            item.put("internalCode", region.getRegionCode());
            item.put("province", region.getProvince());
            item.put("city", region.getCity());
            item.put("level", region.getRegionLevel());
            item.put("externalCodes", externalCodeRepository.findByRegionId(region.getId()).stream()
                    .map(code -> java.util.Map.of("codeSystem", code.getCodeSystem(), "externalCode", code.getExternalCode()))
                    .toList());
            return item;
        }).toList());
    }

    @GetMapping("/regions/coverage")
    public ApiResponse<java.util.Map<String, Object>> regionCoverage(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(java.util.Map.of(
                "provinceCount", regionCodeRepository.countByRegionLevel("PROVINCE"),
                "cityCount", regionCodeRepository.countByRegionLevel("CITY"),
                "sgisExternalCodeCount", externalCodeRepository.countByCodeSystem("SGIS")
        ));
    }

    @GetMapping("/regions/sync-runs/latest")
    public ApiResponse<Object> latestRegionSyncRun(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(syncRunRepository.findTopByOrderByStartedAtDesc().orElse(null));
    }

    @PostMapping("/regions/cache/refresh")
    public ApiResponse<String> refreshRegionCache(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        regionCatalog.refreshCache();
        return ApiResponse.ok("지역 카탈로그 캐시를 갱신했습니다.");
    }

    private void requireAdmin(String key) {
        if (StringUtils.hasText(adminApiKey) && !adminApiKey.equals(key)) {
            throw new YouthCenterApiException("관리자 키가 올바르지 않습니다.");
        }
    }
}
