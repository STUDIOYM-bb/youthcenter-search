package com.themoa.youthcentersearch.youthcenter.service;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.youthcenter.client.ExternalApiResponse;
import com.themoa.youthcentersearch.youthcenter.client.YouthCenterApiClient;
import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyDetail;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyList;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.YouthPolicyItem;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyDetailRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyListRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyPaginationTestRequest;
import com.themoa.youthcentersearch.youthcenter.dto.response.PaginationTestResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyDetailResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyListResponse;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyView;
import com.themoa.youthcentersearch.youthcenter.mapper.YouthPolicyMapper;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;
import com.themoa.youthcentersearch.youthcenter.parser.YouthCenterResponseParser;
import com.themoa.youthcentersearch.youthcenter.util.RawResponseStorage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class YouthPolicyService {
    private final YouthCenterApiClient client;
    private final YouthCenterResponseParser parser;
    private final YouthCenterApiProperties properties;
    private final RawResponseStorage storage;
    private final YouthPolicyMapper mapper;

    public YouthPolicyService(YouthCenterApiClient client, YouthCenterResponseParser parser,
                              YouthCenterApiProperties properties, RawResponseStorage storage, YouthPolicyMapper mapper) {
        this.client = client;
        this.parser = parser;
        this.properties = properties;
        this.storage = storage;
        this.mapper = mapper;
    }

    public YouthPolicyListResponse search(YouthPolicyListRequest request) {
        validateReturnType(request.effectiveReturnType(properties));
        ExternalApiResponse response = client.fetchCurrentList(request);
        ResponseType type = parser.detect(response);
        ParsedPolicyList parsed = parser.parseList(response);
        String filePath = storage.save("current-list-page-" + request.effectivePageNum(properties), type, response.body());
        if (!parsed.listNodeFound() && response.bodyLength() > 0) {
            throw new YouthCenterApiException("?묐떟? ?뺤긽?곸쑝濡??섏떊?덉?留??뺤콉 紐⑸줉 ?몃뱶瑜??뺤씤?섏? 紐삵뻽?듬땲?? ?묐떟 ?ㅽ궎留?遺꾩꽍 寃곌낵瑜??뺤씤?섏꽭??");
        }
        List<YouthPolicyView> policies = parsed.policies().stream().map(mapper::toView).toList();
        return new YouthPolicyListResponse("CURRENT", response.maskedRequestUrl(), response.statusCode(), response.contentType(),
                type, response.redirected(), response.redirectLocation(), response.elapsedTimeMs(), response.bodyLength(),
                preview(response.body()), filePath, parsed.listNodeFound(), parsed.listNodePath(), parsed.policies().size(),
                parsed.totalCount(), parsed.currentPage(), parsed.pageSize(), first(policies), policies, parsed.schemaAnalysis());
    }

    public YouthPolicyDetailResponse detail(YouthPolicyDetailRequest request) {
        validateReturnType(request.effectiveReturnType(properties));
        ExternalApiResponse response = client.fetchCurrentDetail(request);
        ResponseType type = parser.detect(response);
        ParsedPolicyDetail parsed = parser.parseDetail(response);
        String filePath = storage.save("current-detail-" + request.policyNumber(), type, response.body());
        return new YouthPolicyDetailResponse("CURRENT", response.maskedRequestUrl(), response.statusCode(), response.contentType(),
                type, response.redirected(), response.redirectLocation(), response.elapsedTimeMs(), response.bodyLength(),
                preview(response.body()), filePath, mapper.toView(parsed.policy()), parsed.schemaAnalysis(), parsed.warnings());
    }

    public PaginationTestResponse paginationTest(YouthPolicyPaginationTestRequest request) {
        validateReturnType(request.effectiveReturnType(properties));
        int startPage = request.effectiveStartPage(properties);
        int maxPages = request.effectiveMaxPages(properties);
        Set<String> seenNumbers = new HashSet<>();
        List<PaginationTestResponse.PageResult> pages = new ArrayList<>();
        int parsedTotal = 0;
        int duplicates = 0;
        boolean repeatedPage = false;
        String previousSignature = null;
        String stopReason = "理쒕? ?섏씠吏 ?꾨떖";
        for (int i = 0; i < maxPages; i++) {
            int page = startPage + i;
            YouthPolicyListRequest listRequest = new YouthPolicyListRequest(page, request.effectivePageSize(properties),
                    request.effectiveReturnType(properties), request.policyKeywordName(), request.policyDescription());
            ExternalApiResponse response = client.fetchCurrentList(listRequest);
            ParsedPolicyList parsed = parser.parseList(response);
            List<String> numbers = parsed.policies().stream().map(YouthPolicyItem::policyNumber).toList();
            boolean pageHasDuplicate = false;
            for (String number : numbers) {
                if (number != null && !seenNumbers.add(number)) {
                    duplicates++;
                    pageHasDuplicate = true;
                }
            }
            String signature = String.join("|", numbers);
            if (previousSignature != null && previousSignature.equals(signature) && !signature.isBlank()) {
                repeatedPage = true;
            }
            previousSignature = signature;
            parsedTotal += parsed.policies().size();
            pages.add(new PaginationTestResponse.PageResult(page, response.statusCode(), parsed.policies().size(),
                    numbers.isEmpty() ? null : numbers.get(0), numbers.isEmpty() ? null : numbers.get(numbers.size() - 1),
                    pageHasDuplicate, response.elapsedTimeMs()));
            if (parsed.policies().isEmpty() || (parsed.totalCount() != null && seenNumbers.size() >= parsed.totalCount())) {
                stopReason = "留덉?留??섏씠吏 ?꾨떖";
                break;
            }
        }
        return new PaginationTestResponse(pages.size(), pages.size(), parsedTotal, seenNumbers.size(), duplicates,
                repeatedPage, stopReason, pages);
    }

    private void validateReturnType(String returnType) {
        String normalized = returnType == null ? "" : returnType.toLowerCase(Locale.ROOT);
        if (!"json".equals(normalized) && !"xml".equals(normalized)) {
            throw new YouthCenterApiException("returnType? json ?먮뒗 xml?댁뼱???⑸땲??");
        }
    }

    private YouthPolicyView first(List<YouthPolicyView> policies) {
        return policies.isEmpty() ? null : policies.get(0);
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        int length = Math.min(body.length(), properties.getRawResponse().getPreviewLength());
        return body.substring(0, length);
    }
}
