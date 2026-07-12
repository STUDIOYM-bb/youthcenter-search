package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResponse;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyRagSearchService {
    private static final String REGION_NOT_MATCHED = "REGION_NOT_MATCHED";
    private static final String AGE_NOT_MATCHED = "AGE_NOT_MATCHED";
    private static final String EMPLOYMENT_NOT_MATCHED = "EMPLOYMENT_NOT_MATCHED";

    private final CompositePolicySearchConditionParser conditionParser;
    private final PolicyRepository policyRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;

    public PolicyRagSearchService(CompositePolicySearchConditionParser conditionParser,
                                  PolicyRepository policyRepository,
                                  ObjectProvider<VectorStore> vectorStoreProvider,
                                  RagProperties properties) {
        this.conditionParser = conditionParser;
        this.policyRepository = policyRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        Instant start = Instant.now();
        int resultSize = request.resultSize() == null ? properties.getSearch().getResultSize() : request.resultSize();
        PolicySearchConditionParser.ParsedPolicySearchCondition parsed = conditionParser.parse(request.query(), resultSize);
        PolicySearchCondition condition = parsed.condition();
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        boolean mysqlFallbackUsed = false;
        boolean retried = false;
        String fallbackReason = null;
        List<Document> vectorCandidates = List.of();

        if (properties.isEnabled() && vectorStore != null) {
            vectorCandidates = vectorSearch(vectorStore, request.query(), condition, properties.getSearch().getTopK(), true);
            if (vectorCandidates.size() < resultSize) {
                retried = true;
                vectorCandidates = vectorSearch(vectorStore, request.query(), condition, properties.getSearch().getRetryTopK(), true);
            }
            if (vectorCandidates.isEmpty()) {
                retried = true;
                fallbackReason = "VECTOR_THRESHOLD_EMPTY";
                vectorCandidates = vectorSearch(vectorStore, request.query(), condition, properties.getSearch().getRetryTopK(), false);
            }
        } else {
            fallbackReason = "VECTOR_SEARCH_DISABLED";
        }

        Map<Integer, Double> semanticScores = semanticScores(vectorCandidates);
        List<Policy> policies = semanticScores.isEmpty()
                ? List.of()
                : policyRepository.findWithRelationsByIdIn(new ArrayList<>(semanticScores.keySet()));
        if ((policies.size() < resultSize || policies.isEmpty()) && properties.getSearch().isMysqlFallbackEnabled()) {
            mysqlFallbackUsed = true;
            fallbackReason = fallbackReason == null ? "INSUFFICIENT_VECTOR_RESULTS" : fallbackReason;
            List<Integer> fallbackIds = policyRepository.findActivePolicyIds(PageRequest.of(0, properties.getSearch().getRetryTopK()));
            List<Policy> fallbackPolicies = policyRepository.findWithRelationsByIdIn(fallbackIds);
            Map<Integer, Policy> merged = new LinkedHashMap<>();
            policies.forEach(policy -> merged.put(policy.getId(), policy));
            fallbackPolicies.forEach(policy -> merged.putIfAbsent(policy.getId(), policy));
            policies = new ArrayList<>(merged.values());
        }

        FilterCounters counters = new FilterCounters();
        List<PolicySearchResultItem> results = policies.stream()
                .map(policy -> score(policy, condition, semanticScores.getOrDefault(policy.getId(), 0.0)))
                .filter(item -> pass(item, counters))
                .sorted(Comparator.comparingDouble(PolicySearchResultItem::finalScore).reversed())
                .limit(resultSize)
                .toList();

        PolicySearchDiagnostics diagnostics = new PolicySearchDiagnostics(
                vectorCandidates.size(),
                semanticScores.size(),
                policies.size(),
                counters.regionFiltered,
                counters.ageFiltered,
                counters.employmentFiltered,
                counters.studentFiltered,
                counters.targetFiltered,
                counters.applicationFiltered,
                mysqlFallbackUsed ? Math.max(0, policies.size() - semanticScores.size()) : 0,
                results.size(),
                retried,
                mysqlFallbackUsed,
                fallbackReason,
                Duration.between(start, Instant.now()).toMillis()
        );
        return new PolicySearchResponse(answer(results), condition, parsed.parserMode(), parsed.fallback(),
                mysqlFallbackUsed ? (semanticScores.isEmpty() ? "MYSQL_FALLBACK" : "RAG_WITH_MYSQL_FALLBACK") : "RAG",
                vectorCandidates.size(), results.size(), results, diagnostics);
    }

    private List<Document> vectorSearch(VectorStore vectorStore, String query, PolicySearchCondition condition,
                                        int topK, boolean applyThreshold) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(rewriteQuery(query, condition))
                .topK(topK);
        if (applyThreshold) {
            builder.similarityThreshold(properties.getSearch().getMinimumSimilarity());
        }
        return vectorStore.similaritySearch(builder.build());
    }

    private Map<Integer, Double> semanticScores(List<Document> documents) {
        Map<Integer, Double> scores = new LinkedHashMap<>();
        for (Document document : documents) {
            Object value = document.getMetadata().get("policyId");
            if (value instanceof Number number) {
                scores.put(number.intValue(), document.getScore() == null ? 0.0 : document.getScore());
            } else if (value instanceof String text && text.matches("\\d+")) {
                scores.put(Integer.parseInt(text), document.getScore() == null ? 0.0 : document.getScore());
            }
        }
        return scores;
    }

    private PolicySearchResultItem score(Policy policy, PolicySearchCondition condition, double semanticScore) {
        List<String> matched = new ArrayList<>();
        List<String> needCheck = new ArrayList<>();
        double regionScore = regionScore(policy, condition, matched, needCheck);
        double ageScore = ageScore(policy, condition, matched, needCheck);
        double employmentScore = employmentScore(policy, condition, matched, needCheck);
        double keywordScore = keywordScore(policy, condition, matched);
        double applicationScore = applicationScore(policy, matched);
        double finalScore = semanticScore * 40 + regionScore * 15 + ageScore * 10 + employmentScore * 10
                + keywordScore * 20 + applicationScore * 5;
        PolicyCondition pc = policy.getCondition();
        return new PolicySearchResultItem(policy.getId(), policy.getSourcePolicyId(), policy.getTitle(),
                policy.getCategory().name(), regionText(policy), policy.getAgencyName(), policy.getSummary(),
                pc == null ? null : pc.getMinAge(), pc == null ? null : pc.getMaxAge(),
                pc == null ? null : pc.getEmploymentStatus(), policy.getStartDate(), policy.getDueDate(),
                policy.getStatus(), policy.getOfficialUrl(), Math.round(semanticScore * 1000.0) / 1000.0,
                Math.round(finalScore * 10.0) / 10.0, matched, needCheck);
    }

    private boolean pass(PolicySearchResultItem item, FilterCounters counters) {
        if ("CLOSED".equals(item.applicationStatus())) {
            counters.applicationFiltered++;
            return false;
        }
        if (item.needCheckReasons().contains(REGION_NOT_MATCHED)) {
            counters.regionFiltered++;
            return false;
        }
        if (item.needCheckReasons().contains(AGE_NOT_MATCHED)) {
            counters.ageFiltered++;
            return false;
        }
        if (item.needCheckReasons().contains(EMPLOYMENT_NOT_MATCHED)) {
            counters.employmentFiltered++;
            return false;
        }
        return true;
    }

    private double regionScore(Policy policy, PolicySearchCondition condition, List<String> matched, List<String> needCheck) {
        if (!StringUtils.hasText(condition.province()) && !StringUtils.hasText(condition.city())) {
            return 0.5;
        }
        Set<String> regions = new LinkedHashSet<>(policy.getRegions().stream().map(region -> region.getRegion().displayName()).toList());
        if (regions.stream().anyMatch(region -> region.contains("전국"))) {
            matched.add("전국 정책");
            return 1.0;
        }
        String targetCity = condition.city();
        String targetProvince = condition.province();
        if (StringUtils.hasText(targetCity) && regions.stream().anyMatch(region -> region.contains(targetCity))) {
            matched.add("지역 일치: " + targetCity);
            return 1.0;
        }
        if (StringUtils.hasText(targetProvince) && regions.stream().anyMatch(region -> region.contains(targetProvince))) {
            matched.add("광역 지역 일치: " + targetProvince);
            return 0.8;
        }
        if (!regions.isEmpty()) {
            needCheck.add(REGION_NOT_MATCHED);
            return 0;
        }
        needCheck.add("REGION_UNKNOWN");
        return 0.2;
    }

    private double ageScore(Policy policy, PolicySearchCondition condition, List<String> matched, List<String> needCheck) {
        if (condition.age() == null || policy.getCondition() == null) {
            return 0.5;
        }
        Integer min = policy.getCondition().getMinAge();
        Integer max = policy.getCondition().getMaxAge();
        if (min == null && max == null) {
            needCheck.add("AGE_UNKNOWN");
            return 0.4;
        }
        if ((min == null || condition.age() >= min) && (max == null || condition.age() <= max)) {
            matched.add("나이 조건 일치");
            return 1.0;
        }
        needCheck.add(AGE_NOT_MATCHED);
        return 0;
    }

    private double employmentScore(Policy policy, PolicySearchCondition condition, List<String> matched, List<String> needCheck) {
        if (!StringUtils.hasText(condition.employmentStatus()) || policy.getCondition() == null
                || !StringUtils.hasText(policy.getCondition().getEmploymentStatus())) {
            return 0.5;
        }
        if (condition.employmentStatus().equals(policy.getCondition().getEmploymentStatus())) {
            matched.add("취업 상태 일치");
            return 1.0;
        }
        needCheck.add(EMPLOYMENT_NOT_MATCHED);
        return 0;
    }

    private double keywordScore(Policy policy, PolicySearchCondition condition, List<String> matched) {
        double score = 0;
        for (String keyword : condition.keywords()) {
            if (contains(policy.getTitle(), keyword)) {
                score += 0.5;
                matched.add("정책명 키워드: " + keyword);
            }
            if (contains(policy.getSummary(), keyword)) {
                score += 0.25;
                matched.add("내용 키워드: " + keyword);
            }
        }
        return Math.min(1.0, score);
    }

    private double applicationScore(Policy policy, List<String> matched) {
        if (policy.isAlwaysOpen() || policy.getDueDate() == null || !policy.getDueDate().isBefore(LocalDate.now())) {
            matched.add("신청 가능 상태 확인");
            return 1.0;
        }
        return 0;
    }

    private String rewriteQuery(String query, PolicySearchCondition condition) {
        return String.join(" ", query, String.join(" ", condition.keywords()));
    }

    private String answer(List<PolicySearchResultItem> results) {
        if (results.isEmpty()) {
            return "조건에 명확히 맞는 정책을 찾지 못했습니다. 지역, 나이, 취업 상태 조건을 조금 넓혀 다시 검색해 보세요.";
        }
        return "검색된 실제 온통청년 정책 중 관련도가 높은 정책을 우선 표시합니다. 최종 신청 자격은 정책 상세와 공식 기관에서 확인해야 합니다.";
    }

    private String regionText(Policy policy) {
        return policy.getRegions().stream()
                .map(region -> region.getRegion().displayName())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("확인 필요");
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && StringUtils.hasText(keyword) && value.contains(keyword);
    }

    private static class FilterCounters {
        int regionFiltered;
        int ageFiltered;
        int employmentFiltered;
        int studentFiltered;
        int targetFiltered;
        int applicationFiltered;
    }
}
