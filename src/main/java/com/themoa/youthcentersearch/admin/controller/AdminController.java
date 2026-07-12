package com.themoa.youthcentersearch.admin.controller;

import com.themoa.youthcentersearch.admin.dto.AdminJobStatus;
import com.themoa.youthcentersearch.admin.dto.AdminStatusResponse;
import com.themoa.youthcentersearch.admin.service.AdminJobService;
import com.themoa.youthcentersearch.admin.service.AdminStatusService;
import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.common.response.ApiResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.service.YouthCenterDiagnosticService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminStatusService statusService;
    private final AdminJobService jobService;
    private final YouthCenterDiagnosticService diagnosticService;
    private final String adminApiKey;

    public AdminController(AdminStatusService statusService, AdminJobService jobService,
                           YouthCenterDiagnosticService diagnosticService,
                           @Value("${app.admin-api-key:}") String adminApiKey) {
        this.statusService = statusService;
        this.jobService = jobService;
        this.diagnosticService = diagnosticService;
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
        return ApiResponse.ok("사용자 검색 API /api/policies/search 에서 Qdrant 검색을 수행합니다.");
    }

    private void requireAdmin(String key) {
        if (StringUtils.hasText(adminApiKey) && !adminApiKey.equals(key)) {
            throw new YouthCenterApiException("관리자 키가 올바르지 않습니다.");
        }
    }
}
