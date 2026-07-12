package com.themoa.youthcentersearch.youthcenter.service;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiResponseException;
import com.themoa.youthcentersearch.youthcenter.client.ExternalApiResponse;
import com.themoa.youthcentersearch.youthcenter.client.YouthCenterApiClient;
import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyList;
import com.themoa.youthcentersearch.youthcenter.dto.request.FilterProbeType;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthCenterFilterProbeRequest;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyListRequest;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthCenterFilterProbeResponse;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;
import com.themoa.youthcentersearch.youthcenter.parser.YouthCenterResponseParser;
import org.springframework.stereotype.Service;

@Service
public class YouthCenterFilterProbeService {
    private final YouthCenterApiClient client;
    private final YouthCenterResponseParser parser;
    private final YouthCenterApiProperties properties;

    public YouthCenterFilterProbeService(YouthCenterApiClient client, YouthCenterResponseParser parser,
                                         YouthCenterApiProperties properties) {
        this.client = client;
        this.parser = parser;
        this.properties = properties;
    }

    public YouthCenterFilterProbeResponse probe(YouthCenterFilterProbeRequest request) {
        YouthPolicyListRequest listRequest = toListRequest(request);
        ExternalApiResponse response = client.fetchCurrentList(listRequest);
        ResponseType type = parser.detect(response);
        try {
            ParsedPolicyList parsed = parser.parseList(response);
            return new YouthCenterFilterProbeResponse(request.filterType().name(), request.value(),
                    response.maskedRequestUrl(), response.statusCode(), response.contentType(), type,
                    parsed.policies().size(), parsed.totalCount(), response.elapsedTimeMs(), null);
        } catch (YouthCenterApiResponseException ex) {
            return new YouthCenterFilterProbeResponse(request.filterType().name(), request.value(),
                    response.maskedRequestUrl(), response.statusCode(), response.contentType(), type,
                    0, null, response.elapsedTimeMs(), ex.getMessage());
        }
    }

    private YouthPolicyListRequest toListRequest(YouthCenterFilterProbeRequest request) {
        String keyword = null;
        String description = null;
        if (request.filterType() == FilterProbeType.POLICY_KEYWORD) {
            keyword = request.value();
        }
        if (request.filterType() == FilterProbeType.POLICY_DESCRIPTION) {
            description = request.value();
        }
        return new YouthPolicyListRequest(1, Math.min(request.effectivePageSize(), properties.getMaximumPageSize()),
                properties.getDefaultReturnType(), keyword, description);
    }
}
