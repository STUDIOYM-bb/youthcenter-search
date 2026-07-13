package com.themoa.youthcentersearch.policy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.admin.service.JobProgressUpdate;
import com.themoa.youthcentersearch.policy.domain.PolicyCollectionError;
import com.themoa.youthcentersearch.policy.domain.PolicyCollectionRun;
import com.themoa.youthcentersearch.policy.domain.PolicyRawData;
import com.themoa.youthcentersearch.policy.domain.PolicySource;
import com.themoa.youthcentersearch.policy.repository.PolicyCollectionErrorRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyCollectionRunRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRawDataRepository;
import com.themoa.youthcentersearch.youthcenter.client.ExternalApiResponse;
import com.themoa.youthcentersearch.youthcenter.client.YouthCenterApiClient;
import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.ParsedPolicyList;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.YouthPolicyItem;
import com.themoa.youthcentersearch.youthcenter.dto.request.YouthPolicyListRequest;
import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;
import com.themoa.youthcentersearch.youthcenter.parser.YouthCenterResponseParser;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Service
public class YouthCenterPolicyCollectionService {
    private final YouthCenterApiClient client;
    private final YouthCenterResponseParser parser;
    private final YouthCenterApiProperties properties;
    private final PolicyPersistenceService persistenceService;
    private final PolicyRawDataRepository rawDataRepository;
    private final PolicyCollectionRunRepository runRepository;
    private final PolicyCollectionErrorRepository errorRepository;
    private final ObjectMapper objectMapper;

    public YouthCenterPolicyCollectionService(YouthCenterApiClient client, YouthCenterResponseParser parser,
                                              YouthCenterApiProperties properties,
                                              PolicyPersistenceService persistenceService,
                                              PolicyRawDataRepository rawDataRepository,
                                              PolicyCollectionRunRepository runRepository,
                                              PolicyCollectionErrorRepository errorRepository,
                                              ObjectMapper objectMapper) {
        this.client = client;
        this.parser = parser;
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.rawDataRepository = rawDataRepository;
        this.runRepository = runRepository;
        this.errorRepository = errorRepository;
        this.objectMapper = objectMapper;
    }

    public PolicyCollectionResult collectAll() {
        return collectAll(null);
    }

    public PolicyCollectionResult collectAll(Consumer<JobProgressUpdate> progressConsumer) {
        PolicyCollectionRun run = runRepository.save(new PolicyCollectionRun(PolicySource.YOUTH_CENTER.name(), "MANUAL"));
        int pageSize = properties.getCollection().getPageSize();
        int maxPages = properties.getCollection().getMaxPages();
        int totalCount = 0;
        int apiRequests = 0;
        int requestedPages = 0;
        Set<String> pageHashes = new HashSet<>();
        Set<String> firstPolicyNumbers = new HashSet<>();

        try {
            notify(progressConsumer, new JobProgressUpdate("CONNECTING", "온통청년 연결 중", 0, 0, 0, 0, 0, 0, 0, 0, null, apiRequests, 0, "온통청년 API 연결 중"));
            for (int page = 1; page <= maxPages; page++) {
                notify(progressConsumer, new JobProgressUpdate("FETCHING_PAGE", "페이지 요청 중", totalCount, run.getReceivedCount(), run.getInsertedCount() + run.getUpdatedCount(), run.getFailedCount(), page, totalCount <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize), 0, 0, null, apiRequests, 0, page + "페이지 요청 중"));
                PageFetchResult pageResult = fetchPageWithRetries(page, pageSize);
                apiRequests += pageResult.apiRequestCount();
                PolicyRawData rawData = pageResult.rawData();
                ExternalApiResponse response = pageResult.response();
                ParsedPolicyList parsed = pageResult.parsed();
                requestedPages++;
                run.addPage(parsed.policies().size());
                if (page == 1 && parsed.totalCount() != null) {
                    totalCount = parsed.totalCount();
                }
                int totalPages = totalCount <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize);
                if (isRepeatedPage(response.body(), parsed, pageHashes, firstPolicyNumbers)) {
                    run.complete("STOPPED_REPEATED_PAGE");
                    break;
                }
                for (YouthPolicyItem item : parsed.policies()) {
                    try {
                        notify(progressConsumer, new JobProgressUpdate("PERSISTING_PAGE", "정책 저장 중", totalCount, run.getReceivedCount(), run.getInsertedCount() + run.getUpdatedCount(), run.getFailedCount(), page, totalPages, 0, 0, item.policyName(), apiRequests, 0, "정책을 저장하고 있습니다."));
                        PolicyUpsertResult result = persistenceService.upsert(item, rawData);
                        if (result.inserted()) {
                            run.inserted();
                        } else {
                            run.updated();
                        }
                    } catch (RuntimeException ex) {
                        run.failed();
                        errorRepository.save(new PolicyCollectionError(run, rawData, PolicySource.YOUTH_CENTER.name(),
                                page, item.policyNumber(), ex.getClass().getSimpleName(), ex.getMessage()));
                    }
                }
                int lastPage = totalCount <= 0 ? page : (int) Math.ceil((double) totalCount / pageSize);
                notify(progressConsumer, new JobProgressUpdate("PERSISTING_PAGE", "정책 저장 중", totalCount, run.getReceivedCount(), run.getInsertedCount() + run.getUpdatedCount(), run.getFailedCount(), page, lastPage, 0, 0, null, apiRequests, 0, page + "페이지 저장 완료"));
                if (parsed.policies().isEmpty() || page >= lastPage) {
                    run.complete("COMPLETED");
                    break;
                }
                sleepQuietly(properties.getCollection().getRequestDelay().toMillis());
            }
            if ("RUNNING".equals(run.getStatus())) {
                run.complete("COMPLETED_MAX_PAGES");
            }
        } catch (Exception ex) {
            run.fail(requestedPages + 1, ex.getMessage());
        }
        runRepository.save(run);
        return new PolicyCollectionResult(run.getId(), totalCount, requestedPages, apiRequests, run.getReceivedCount(),
                run.getInsertedCount(), run.getUpdatedCount(), run.getFailedCount(), run.getStatus(), null);
    }

    private void notify(Consumer<JobProgressUpdate> consumer, JobProgressUpdate update) {
        if (consumer != null) {
            consumer.accept(update);
        }
    }

    private PageFetchResult fetchPageWithRetries(int page, int pageSize) throws Exception {
        int maxRetries = Math.max(0, properties.getCollection().getMaxRetries());
        RuntimeException lastRuntimeException = null;
        Exception lastException = null;
        int apiRequestCount = 0;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                YouthPolicyListRequest request = new YouthPolicyListRequest(page, pageSize, "json", null, null);
                ExternalApiResponse response = client.fetchCurrentList(request);
                apiRequestCount++;
                ResponseType responseType = parser.detect(response);
                PolicyRawData rawData = rawDataRepository.save(new PolicyRawData(
                        PolicySource.YOUTH_CENTER.name(),
                        null,
                        response.maskedRequestUrl(),
                        objectMapper.writeValueAsString(Map.of("pageNum", page, "pageSize", pageSize, "pageType", 1, "rtnType", "json", "attempt", attempt + 1)),
                        response.body(),
                        responseType.name(),
                        "RECEIVED",
                        null
                ));
                ParsedPolicyList parsed = parser.parseList(response);
                return new PageFetchResult(response, rawData, parsed, apiRequestCount);
            } catch (RuntimeException ex) {
                lastRuntimeException = ex;
                if (attempt >= maxRetries) {
                    throw ex;
                }
                sleepQuietly(retryDelayMillis(attempt));
            } catch (Exception ex) {
                lastException = ex;
                if (attempt >= maxRetries) {
                    throw ex;
                }
                sleepQuietly(retryDelayMillis(attempt));
            }
        }
        if (lastRuntimeException != null) {
            throw lastRuntimeException;
        }
        throw lastException == null ? new IllegalStateException("페이지 수집에 실패했습니다.") : lastException;
    }

    private long retryDelayMillis(int attempt) {
        long base = Math.max(100L, properties.getCollection().getRequestDelay().toMillis());
        return base * (attempt + 1L);
    }

    private boolean isRepeatedPage(String body, ParsedPolicyList parsed, Set<String> pageHashes, Set<String> firstPolicyNumbers) {
        String hash = DigestUtils.md5DigestAsHex(body.getBytes(StandardCharsets.UTF_8));
        if (!pageHashes.add(hash)) {
            return true;
        }
        if (!parsed.policies().isEmpty()) {
            String first = parsed.policies().get(0).policyNumber();
            return first != null && !firstPolicyNumbers.add(first);
        }
        return false;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record PageFetchResult(
            ExternalApiResponse response,
            PolicyRawData rawData,
            ParsedPolicyList parsed,
            int apiRequestCount
    ) {
    }
}
