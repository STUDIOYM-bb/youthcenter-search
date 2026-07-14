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
import com.themoa.youthcentersearch.rag.service.PolicyRagSearchService;
import com.themoa.youthcentersearch.rag.service.PolicySearchProjectionService;
import com.themoa.youthcentersearch.rag.service.PolicyLexicalIndexBuilder;
import com.themoa.youthcentersearch.policy.service.RegionEligiblePolicyCandidateService;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.service.YouthCenterDiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final PolicyRagSearchService policyRagSearchService;
    private final PolicySearchProjectionService projectionService;
    private final PolicyLexicalIndexBuilder lexicalIndexBuilder;
    private final PolicySearchProjectionRepository projectionRepository;
    private final RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService;
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
                           PolicyRagSearchService policyRagSearchService,
                           PolicySearchProjectionService projectionService,
                           PolicyLexicalIndexBuilder lexicalIndexBuilder,
                           PolicySearchProjectionRepository projectionRepository,
                           RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService,
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
        this.policyRagSearchService = policyRagSearchService;
        this.projectionService = projectionService;
        this.lexicalIndexBuilder = lexicalIndexBuilder;
        this.projectionRepository = projectionRepository;
        this.regionEligiblePolicyCandidateService = regionEligiblePolicyCandidateService;
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

    @PostMapping("/search/explain")
    public ApiResponse<java.util.Map<String, Object>> explainSearch(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestBody java.util.Map<String, Object> request) {
        requireAdmin(key);
        String query = String.valueOf(request.getOrDefault("query", ""));
        Integer policyId = request.get("policyId") instanceof Number number ? number.intValue() : null;
        String sourcePolicyId = request.get("sourcePolicyId") == null ? null : String.valueOf(request.get("sourcePolicyId"));
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("검색 문장을 입력하세요.");
        }
        if (policyId == null && !StringUtils.hasText(sourcePolicyId)) {
            throw new IllegalArgumentException("policyId 또는 sourcePolicyId를 입력하세요.");
        }
        return ApiResponse.ok(policyRagSearchService.explain(query, policyId, sourcePolicyId));
    }

    @PostMapping("/search-projection/rebuild")
    public ApiResponse<java.util.Map<String, Object>> rebuildSearchProjection(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        var result = projectionService.rebuildAll();
        lexicalIndexBuilder.refresh();
        return ApiResponse.ok(java.util.Map.of(
                "version", PolicySearchProjectionService.VERSION,
                "total", result.total(),
                "processed", result.processed(),
                "missingSnapshot", result.missingSnapshot()
        ));
    }

    @PostMapping("/search-index/refresh")
    public ApiResponse<java.util.Map<String, Object>> refreshSearchIndex(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        var index = lexicalIndexBuilder.refresh();
        return ApiResponse.ok(java.util.Map.of(
                "ready", true,
                "documentCount", index.size(),
                "builtAt", index.builtAt().toString()
        ));
    }

    @GetMapping("/search-index/status")
    public ApiResponse<java.util.Map<String, Object>> searchIndexStatus(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        var index = lexicalIndexBuilder.current();
        return ApiResponse.ok(java.util.Map.of(
                "ready", true,
                "documentCount", index.size(),
                "projectionVersion", PolicySearchProjectionService.VERSION,
                "projectionCount", projectionRepository.countByProjectionVersion(PolicySearchProjectionService.VERSION),
                "missingSnapshotCount", projectionRepository.countByMissingSnapshotTrue(),
                "builtAt", index.builtAt().toString()
        ));
    }

    @PostMapping("/search/quality-suite")
    public ApiResponse<java.util.Map<String, Object>> searchQualitySuite(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        java.util.List<String> queries = java.util.List.of(
                "K패스", "K-패스", "k 패스", "청년", "수원 사는 27살 청년 정책",
                "수원 사는 27살 취준생 정책", "수원 청년 계좌", "청년 면접수당");
        java.util.List<java.util.Map<String, Object>> cases = new java.util.ArrayList<>();
        int passed = 0;
        for (String query : queries) {
            var response = policyRagSearchService.search(new com.themoa.youthcentersearch.rag.dto.PolicySearchRequest(query, null, 0, 20));
            java.util.List<String> titles = response.results().stream()
                    .map(com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem::title)
                    .toList();
            boolean pass = switch (query) {
                case "K패스", "K-패스", "k 패스" -> !response.results().isEmpty()
                        && response.results().get(0).title().replaceAll("[\\s\\-]", "").contains("K패스");
                case "청년" -> response.totalMatched() > response.size() && response.hasNext()
                        && primaryRegionsAllowed(response);
                case "수원 사는 27살 청년 정책" -> response.totalMatched() > 1
                        && primaryRegionsAllowed(response);
                case "수원 사는 27살 취준생 정책" -> titles.stream().limit(10).anyMatch(title -> title.contains("면접"));
                case "수원 청년 계좌" -> titles.stream().limit(10).anyMatch(title -> title.contains("계좌") || title.contains("통장") || title.contains("저축"))
                        && primaryRegionsAllowed(response);
                case "청년 면접수당" -> titles.stream().limit(10).anyMatch(title -> title.contains("면접"));
                default -> false;
            };
            if (response.interpretedCondition().regionExplicit()) {
                pass = pass && primaryRegionsAllowed(response);
            }
            if (pass) {
                passed++;
            }
            cases.add(java.util.Map.of(
                    "query", query,
                    "passed", pass,
                    "queryType", response.queryType(),
                    "totalMatched", response.totalMatched(),
                    "hasNext", response.hasNext(),
                    "regionViolations", regionViolations(response),
                    "topTitles", titles.stream().limit(10).toList()
            ));
        }
        java.util.Map<String, Object> report = new java.util.LinkedHashMap<>();
        report.put("generatedAt", java.time.Instant.now().toString());
        report.put("passed", passed);
        report.put("total", queries.size());
        report.put("success", passed == queries.size());
        report.put("cases", cases);
        writeQualityReports(report);
        return ApiResponse.ok(report);
    }

    @PostMapping("/search/region-quality-suite")
    public ApiResponse<java.util.Map<String, Object>> regionSearchQualitySuite(
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        java.util.List<java.util.Map<String, Object>> cases = new java.util.ArrayList<>();
        java.util.List<String> failures = new java.util.ArrayList<>();
        int passed = 0;
        for (var region : regionCodeRepository.findActiveDirectSigunguRegions()) {
            String alias = shortRegionAlias(region.getCity());
            String query = alias + " 사는 30살 청년 정책";
            var resolved = new com.themoa.youthcentersearch.policy.region.ResolvedUserRegion(
                    region.getProvince(), region.getCity(), null,
                    com.themoa.youthcentersearch.policy.region.SearchRegionLevel.SIGUNGU, region);
            var candidates = regionEligiblePolicyCandidateService.findEligibleCandidates(resolved);
            java.util.Map<String, Long> counts = candidates.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            candidate -> candidate.compatibility().name(),
                            java.util.LinkedHashMap::new,
                            java.util.stream.Collectors.counting()));
            java.util.List<String> violations = candidates.stream()
                    .map(candidate -> candidate.compatibility().name())
                    .filter(status -> "NOT_MATCHED".equals(status) || "UNKNOWN".equals(status))
                    .toList();
            long distinctCount = candidates.stream().map(com.themoa.youthcentersearch.policy.region.RegionEligiblePolicyCandidate::policyId)
                    .distinct().count();
            boolean pass = !candidates.isEmpty()
                    && violations.isEmpty()
                    && counts.getOrDefault("EXACT_SIGUNGU", 0L) + counts.getOrDefault("MULTIPLE_REGION_MATCH", 0L) > 0
                    && counts.getOrDefault("NATIONWIDE", 0L) > 0
                    && distinctCount == candidates.size();
            if (pass) {
                passed++;
            } else {
                failures.add(region.displayName());
            }
            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("query", query);
            java.util.Map<String, Object> resolvedRegion = new java.util.LinkedHashMap<>();
            resolvedRegion.put("province", resolved.province());
            resolvedRegion.put("city", resolved.city());
            resolvedRegion.put("regionLevel", resolved.level().name());
            resolvedRegion.put("regionExplicit", true);
            item.put("resolvedRegion", resolvedRegion);
            item.put("eligiblePoolCount", candidates.size());
            item.put("exactCount", counts.getOrDefault("EXACT_SIGUNGU", 0L));
            item.put("parentCount", counts.getOrDefault("PARENT_SIDO", 0L));
            item.put("nationwideCount", counts.getOrDefault("NATIONWIDE", 0L));
            item.put("multipleRegionPoolCount", counts.getOrDefault("MULTIPLE_REGION_MATCH", 0L));
            item.put("unknownExcludedCount", 0);
            item.put("wrongRegionExcludedCount", 0);
            item.put("top20RegionCompatibility", candidates.stream()
                    .limit(20)
                    .map(candidate -> candidate.compatibility().name())
                    .toList());
            item.put("violations", violations);
            item.put("passed", pass);
            cases.add(item);
        }
        java.util.Map<String, Object> report = new java.util.LinkedHashMap<>();
        report.put("generatedAt", java.time.Instant.now().toString());
        report.put("passed", passed);
        report.put("total", cases.size());
        report.put("success", passed == cases.size());
        report.put("failedRegions", failures);
        report.put("cases", cases);
        writeRegionQualityReports(report);
        return ApiResponse.ok(report);
    }

    private boolean primaryRegionsAllowed(com.themoa.youthcentersearch.rag.dto.PolicySearchResponse response) {
        return regionViolations(response).isEmpty();
    }

    private java.util.List<String> regionViolations(com.themoa.youthcentersearch.rag.dto.PolicySearchResponse response) {
        if (response == null || response.interpretedCondition() == null || !response.interpretedCondition().regionExplicit()) {
            return java.util.List.of();
        }
        return response.results().stream()
                .map(com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem::regionCompatibility)
                .filter(status -> "NOT_MATCHED".equals(status) || "UNKNOWN".equals(status))
                .toList();
    }

    private String shortRegionAlias(String city) {
        if (!StringUtils.hasText(city)) {
            return "";
        }
        String value = city.contains(" ") ? city.substring(0, city.indexOf(' ')) : city;
        for (String suffix : java.util.List.of("특별자치시", "특별자치도", "특별시", "광역시", "시", "군", "구", "도")) {
            if (value.endsWith(suffix) && value.length() > suffix.length()) {
                return value.substring(0, value.length() - suffix.length());
            }
        }
        return value;
    }

    private void writeRegionQualityReports(java.util.Map<String, Object> report) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("build", "reports");
            java.nio.file.Files.createDirectories(dir);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("region-search-quality.json").toFile(), report);
            StringBuilder md = new StringBuilder();
            md.append("# Region Search Quality Report\n\n");
            md.append("- generatedAt: ").append(report.get("generatedAt")).append('\n');
            md.append("- passed: ").append(report.get("passed")).append('/').append(report.get("total")).append('\n');
            md.append("- success: ").append(report.get("success")).append('\n');
            md.append("- failedRegions: ").append(report.get("failedRegions")).append("\n\n");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> cases = (java.util.List<java.util.Map<String, Object>>) report.get("cases");
            for (java.util.Map<String, Object> item : cases) {
                md.append("## ").append(item.get("query")).append('\n');
                md.append("- passed: ").append(item.get("passed")).append('\n');
                md.append("- resolvedRegion: ").append(item.get("resolvedRegion")).append('\n');
                md.append("- eligiblePoolCount: ").append(item.get("eligiblePoolCount")).append('\n');
                md.append("- exactCount: ").append(item.get("exactCount")).append('\n');
                md.append("- parentCount: ").append(item.get("parentCount")).append('\n');
                md.append("- nationwideCount: ").append(item.get("nationwideCount")).append('\n');
                md.append("- unknownExcludedCount: ").append(item.get("unknownExcludedCount")).append('\n');
                md.append("- wrongRegionExcludedCount: ").append(item.get("wrongRegionExcludedCount")).append('\n');
                md.append("- top20 region compatibility: ").append(item.get("top20RegionCompatibility")).append('\n');
                md.append("- violations: ").append(item.get("violations")).append("\n\n");
            }
            java.nio.file.Files.writeString(dir.resolve("region-search-quality.md"), md.toString());
        } catch (Exception ex) {
            throw new IllegalStateException("지역 검색 품질 리포트 저장 실패", ex);
        }
    }

    private void writeQualityReports(java.util.Map<String, Object> report) {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("build", "reports");
            java.nio.file.Files.createDirectories(dir);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("search-quality-report.json").toFile(), report);
            StringBuilder md = new StringBuilder();
            md.append("# Search Quality Report\n\n");
            md.append("- generatedAt: ").append(report.get("generatedAt")).append('\n');
            md.append("- passed: ").append(report.get("passed")).append('/').append(report.get("total")).append('\n');
            md.append("- success: ").append(report.get("success")).append("\n\n");
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> cases = (java.util.List<java.util.Map<String, Object>>) report.get("cases");
            for (java.util.Map<String, Object> item : cases) {
                md.append("## ").append(item.get("query")).append('\n');
                md.append("- passed: ").append(item.get("passed")).append('\n');
                md.append("- queryType: ").append(item.get("queryType")).append('\n');
                md.append("- totalMatched: ").append(item.get("totalMatched")).append('\n');
                md.append("- hasNext: ").append(item.get("hasNext")).append('\n');
                md.append("- topTitles: ").append(item.get("topTitles")).append("\n\n");
            }
            java.nio.file.Files.writeString(dir.resolve("search-quality-report.md"), md.toString());
        } catch (Exception ex) {
            throw new IllegalStateException("품질 리포트 저장 실패", ex);
        }
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
        response.put("regionLevel", result.regionLevel());
        response.put("province", result.province());
        response.put("city", result.city());
        response.put("displayName", result.regionName());
        response.put("matchedText", result.matchedText());
        response.put("matchType", result.matchType());
        response.put("regionId", regionId);
        response.put("internalCode", result.regionCode());
        response.put("regionName", result.regionName());
        response.put("externalCodes", externalCodes);
        response.put("candidates", result.candidateDetails());
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
                "municipalityCount", regionCodeRepository.countByRegionLevel("CITY"),
                "cityCount", regionCodeRepository.countByRegionLevel("CITY"),
                "sgisProvinceExternalCodeCount", externalCodeRepository.countByCodeSystemAndRegionLevel("SGIS", "PROVINCE"),
                "sgisMunicipalityExternalCodeCount", externalCodeRepository.countByCodeSystemAndRegionLevel("SGIS", "CITY"),
                "sgisExternalCodeCount", externalCodeRepository.countByCodeSystem("SGIS"),
                "legacyRegionCount", regionCodeRepository.countLegacyRegions(),
                "standardRegionCount", regionCodeRepository.countByRegionCodeStartingWith("P:")
                        + regionCodeRepository.countByRegionCodeStartingWith("M:")
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
