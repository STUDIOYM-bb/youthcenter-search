package com.themoa.youthcentersearch.youthcenter.controller;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.common.response.ApiResponse;
import com.themoa.youthcentersearch.youthcenter.dto.request.NaturalLanguageSearchRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthCenterFilterProbeRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyDetailRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyListRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyPaginationTestRequest;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageSearchResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.PaginationTestResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterFilterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterStatusResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyDetailResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyListResponse;
import com.themoa.youthcentersearch.youthcenter.service.NaturalLanguagePolicySearchService;
import com.themoa.youthcentersearch.youthcenter.service.YouthCenterDiagnosticService;
import com.themoa.youthcentersearch.youthcenter.service.YouthCenterFilterProbeService;
import com.themoa.youthcentersearch.youthcenter.service.YouthPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youth-center")
public class YouthCenterApiController {
    private final YouthCenterDiagnosticService diagnosticService;
    private final YouthPolicyService policyService;
    private final NaturalLanguagePolicySearchService naturalLanguagePolicySearchService;
    private final YouthCenterFilterProbeService filterProbeService;
    private final String adminApiKey;

    public YouthCenterApiController(YouthCenterDiagnosticService diagnosticService,
                                    YouthPolicyService policyService,
                                    NaturalLanguagePolicySearchService naturalLanguagePolicySearchService,
                                    YouthCenterFilterProbeService filterProbeService,
                                    @Value("${app.admin-api-key:}") String adminApiKey) {
        this.diagnosticService = diagnosticService;
        this.policyService = policyService;
        this.naturalLanguagePolicySearchService = naturalLanguagePolicySearchService;
        this.filterProbeService = filterProbeService;
        this.adminApiKey = adminApiKey;
    }

    @GetMapping("/status")
    public ApiResponse<YouthCenterStatusResponse> status() {
        return ApiResponse.ok(diagnosticService.status());
    }

    @PostMapping("/probe")
    public ApiResponse<YouthCenterProbeResponse> probe(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(diagnosticService.probe());
    }

    @PostMapping("/policies/search")
    public ApiResponse<YouthPolicyListResponse> search(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                                       @Valid @RequestBody YouthPolicyListRequest request) {
        requireAdmin(key);
        return ApiResponse.ok(policyService.search(request));
    }

    @PostMapping("/policies/detail")
    public ApiResponse<YouthPolicyDetailResponse> detail(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                                         @Valid @RequestBody YouthPolicyDetailRequest request) {
        requireAdmin(key);
        return ApiResponse.ok(policyService.detail(request));
    }

    @PostMapping("/pagination-test")
    public ApiResponse<PaginationTestResponse> paginationTest(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                                              @Valid @RequestBody YouthPolicyPaginationTestRequest request) {
        requireAdmin(key);
        return ApiResponse.ok(policyService.paginationTest(request));
    }

    @PostMapping("/natural-language/search")
    public ApiResponse<NaturalLanguageSearchResponse> naturalLanguageSearch(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @Valid @RequestBody NaturalLanguageSearchRequest request) {
        requireAdmin(key);
        return ApiResponse.ok(naturalLanguagePolicySearchService.search(request));
    }

    @PostMapping("/filter-probe")
    public ApiResponse<YouthCenterFilterProbeResponse> filterProbe(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @Valid @RequestBody YouthCenterFilterProbeRequest request) {
        requireAdmin(key);
        return ApiResponse.ok(filterProbeService.probe(request));
    }

    @PostMapping("/legacy/probe")
    public ApiResponse<YouthCenterProbeResponse> legacyProbe(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ApiResponse.ok(diagnosticService.legacyProbe());
    }

    private void requireAdmin(String key) {
        if (StringUtils.hasText(adminApiKey) && !adminApiKey.equals(key)) {
            throw new YouthCenterApiException("愿由ъ옄 ?ㅺ? ?щ컮瑜댁? ?딆뒿?덈떎.");
        }
    }
}
