package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.policy.region.RegionMatchStatus;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
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
    private static final String STUDENT_NOT_MATCHED = "STUDENT_NOT_MATCHED";

    private final CompositePolicySearchConditionParser conditionParser;
    private final PolicyRepository policyRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;
    private final RegionMatchEvaluator regionMatchEvaluator;
    private final PolicyLexicalSearchService lexicalSearchService;

    public PolicyRagSearchService(CompositePolicySearchConditionParser conditionParser,
                                  PolicyRepository policyRepository,
                                  ObjectProvider<VectorStore> vectorStoreProvider,
                                  RagProperties properties,
                                  RegionMatchEvaluator regionMatchEvaluator,
                                  PolicyLexicalSearchService lexicalSearchService) {
        this.conditionParser = conditionParser;
        this.policyRepository = policyRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
        this.regionMatchEvaluator = regionMatchEvaluator;
        this.lexicalSearchService = lexicalSearchService;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        if (!properties.isEnabled()) {
            throw new YouthCenterApiException("""
                    RAG 기능이 비활성화되어 있습니다.
                    RAG_ENABLED=true 설정을 확인하세요.""");
        }
        Instant start = Instant.now();
        int resultSize = request.resultSize() == null ? properties.getSearch().getResultSize() : request.resultSize();
        PolicySearchConditionParser.ParsedPolicySearchCondition parsed = conditionParser.parse(request.query(), resultSize);
        PolicySearchCondition condition = parsed.condition();
        ResolvedUserRegion userRegion = regionMatchEvaluator.resolveUserRegion(condition.province(), condition.city(), condition.district());
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
        var lexical = lexicalSearchService.search(condition, properties.getSearch().getRetryTopK());
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>(lexical.lexicalScores());
        Map<Integer, Double> titleExactScores = new LinkedHashMap<>(lexical.titleExactScores());
        LinkedHashSet<Integer> mergedIds = new LinkedHashSet<>(semanticScores.keySet());
        int beforeLexicalMerge = mergedIds.size();
        mergedIds.addAll(lexical.policyIds());
        int duplicateCandidateCount = semanticScores.size() + lexical.policyIds().size() - mergedIds.size();
        List<Policy> policies = mergedIds.isEmpty()
                ? List.of()
                : policyRepository.findWithRelationsByIdIn(new ArrayList<>(mergedIds));
        if (policies.isEmpty() && properties.getSearch().isMysqlFallbackEnabled()) {
            mysqlFallbackUsed = true;
            fallbackReason = fallbackReason == null ? "NO_VECTOR_CANDIDATES" : fallbackReason;
            List<Integer> fallbackIds = policyRepository.findActivePolicyIds(PageRequest.of(0, properties.getSearch().getRetryTopK()));
            policies = policyRepository.findWithRelationsByIdIn(fallbackIds);
        }

        FilterCounters counters = new FilterCounters();
        List<PolicySearchResultItem> results = policies.stream()
                .map(policy -> score(policy, condition, userRegion,
                        semanticScores.getOrDefault(policy.getId(), 0.0),
                        lexicalScores.computeIfAbsent(policy.getId(), id -> lexicalSearchService.lexicalScore(policy, condition)),
                        titleExactScores.computeIfAbsent(policy.getId(), id -> lexicalSearchService.titleExactScore(policy, condition)),
                        counters))
                .filter(item -> pass(item, condition, counters))
                .sorted(Comparator.comparingInt((PolicySearchResultItem item) -> condition.regionExplicit() ? regionSortRank(item) : 0)
                        .thenComparing(Comparator.comparingDouble(PolicySearchResultItem::finalScore).reversed()))
                .limit(resultSize)
                .toList();
        if (results.isEmpty()) {
            fallbackReason = determineFallbackReason(fallbackReason, semanticScores.size(), lexical.policyIds().size(), counters, condition);
        } else {
            fallbackReason = null;
        }

        PolicySearchDiagnostics diagnostics = new PolicySearchDiagnostics(
                vectorCandidates.size(),
                lexical.policyIds().size(),
                policies.size(),
                duplicateCandidateCount,
                counters.nationwideCandidateCount,
                counters.provinceMatchedCount,
                counters.cityMatchedCount,
                counters.districtMatchedCount,
                counters.multipleRegionMatchedCount,
                counters.regionUnknownCount,
                counters.regionNotMatchedCount,
                counters.regionHardFilteredCount,
                semanticScores.size(),
                policies.size(),
                counters.regionFiltered,
                counters.ageFiltered,
                counters.employmentFiltered,
                counters.studentFiltered,
                counters.targetFiltered,
                counters.applicationFiltered,
                mysqlFallbackUsed ? Math.max(0, policies.size() - beforeLexicalMerge) : 0,
                results.size(),
                retried,
                mysqlFallbackUsed,
                condition.searchMode().name(),
                condition.regionExplicit(),
                condition.ageExplicit(),
                condition.employmentExplicit(),
                condition.studentExplicit(),
                condition.regionExplicit(),
                condition.ageExplicit(),
                condition.employmentExplicit(),
                condition.studentExplicit(),
                String.join(", ", condition.keywords()),
                String.join(", ", condition.expandedKeywords()),
                fallbackReason,
                Duration.between(start, Instant.now()).toMillis()
        );
        return new PolicySearchResponse(answer(results), condition, parsed.parserMode(), parsed.fallback(),
                condition.searchMode().name(),
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

    private PolicySearchResultItem score(Policy policy, PolicySearchCondition condition, ResolvedUserRegion userRegion,
                                         double semanticScore, double lexicalScore, double titleExactScore, FilterCounters counters) {
        List<String> matched = new ArrayList<>();
        List<String> needCheck = new ArrayList<>();
        RegionMatchResult regionMatch = regionMatchEvaluator.evaluate(policy, userRegion);
        countRegion(regionMatch.status(), counters);
        double regionScore = regionMatch.score() / 100.0;
        if (condition.regionExplicit() && regionMatch.status() == RegionMatchStatus.NOT_MATCHED) {
            needCheck.add(REGION_NOT_MATCHED);
        } else if (condition.regionExplicit() && regionMatch.status() == RegionMatchStatus.UNKNOWN) {
            needCheck.add("REGION_UNKNOWN");
        } else {
            matched.add(regionMatch.description());
        }
        double ageScore = ageScore(policy, condition, matched, needCheck);
        double employmentScore = employmentScore(policy, condition, matched, needCheck);
        double studentScore = studentScore(policy, condition, matched, needCheck);
        double keywordScore = keywordScore(policy, condition, matched);
        double applicationScore = applicationScore(policy, matched);
        double finalScore = finalScore(condition, semanticScore, lexicalScore, titleExactScore, regionScore,
                ageScore, employmentScore, studentScore, keywordScore, applicationScore);
        if (lexicalScore > 0) matched.add("키워드 일치");
        if (titleExactScore >= 0.8) matched.add("정책명 정확도 높음");
        PolicyCondition pc = policy.getCondition();
        return new PolicySearchResultItem(policy.getId(), policy.getSourcePolicyId(), policy.getTitle(),
                policy.getCategory().name(), regionText(policy), regionMatch.status().name(), regionMatch.description(),
                regionEvidence(policy), policy.getAgencyName(), policy.getSummary(),
                pc == null ? null : pc.getMinAge(), pc == null ? null : pc.getMaxAge(),
                pc == null ? null : pc.getEmploymentStatus(), policy.getStartDate(), policy.getDueDate(),
                policy.getStatus(), policy.getOfficialUrl(), Math.round(semanticScore * 1000.0) / 1000.0,
                Math.round(finalScore * 10.0) / 10.0, matched, needCheck);
    }

    private boolean pass(PolicySearchResultItem item, PolicySearchCondition condition, FilterCounters counters) {
        if ("CLOSED".equals(item.applicationStatus())) {
            counters.applicationFiltered++;
            return false;
        }
        if (condition.regionExplicit() && item.needCheckReasons().contains(REGION_NOT_MATCHED)) {
            counters.regionFiltered++;
            counters.regionHardFilteredCount++;
            return false;
        }
        if (condition.regionExplicit() && item.needCheckReasons().contains("REGION_UNKNOWN") && !properties.getSearch().isIncludeUnknownRegion()) {
            counters.regionFiltered++;
            counters.regionHardFilteredCount++;
            return false;
        }
        if (condition.ageExplicit() && item.needCheckReasons().contains(AGE_NOT_MATCHED)) {
            counters.ageFiltered++;
            return false;
        }
        if (condition.employmentExplicit() && item.needCheckReasons().contains(EMPLOYMENT_NOT_MATCHED)) {
            counters.employmentFiltered++;
            return false;
        }
        if (condition.studentExplicit() && item.needCheckReasons().contains(STUDENT_NOT_MATCHED)) {
            counters.studentFiltered++;
            return false;
        }
        return true;
    }

    private double ageScore(Policy policy, PolicySearchCondition condition, List<String> matched, List<String> needCheck) {
        if (!condition.ageExplicit() || condition.age() == null || policy.getCondition() == null) {
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
        if (!condition.employmentExplicit() || !StringUtils.hasText(condition.employmentStatus()) || policy.getCondition() == null
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

    private double studentScore(Policy policy, PolicySearchCondition condition, List<String> matched, List<String> needCheck) {
        if (!condition.studentExplicit() || condition.studentStatus() == null || policy.getCondition() == null
                || policy.getCondition().getStudentStatus() == null) {
            return 0.5;
        }
        if (condition.studentStatus().equals(policy.getCondition().getStudentStatus())) {
            matched.add("학생 조건 일치");
            return 1.0;
        }
        needCheck.add(STUDENT_NOT_MATCHED);
        return 0;
    }

    private double keywordScore(Policy policy, PolicySearchCondition condition, List<String> matched) {
        double score = 0;
        for (String keyword : condition.expandedKeywords()) {
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

    private double finalScore(PolicySearchCondition condition, double semanticScore, double lexicalScore, double titleExactScore,
                              double regionScore, double ageScore, double employmentScore, double studentScore,
                              double keywordScore, double applicationScore) {
        Map<String, Double> weights = new LinkedHashMap<>();
        if (condition.searchMode() == PolicySearchMode.KEYWORD) {
            weights.put("lexical", 45.0);
            weights.put("semantic", 35.0);
            weights.put("title", 15.0);
            weights.put("application", 5.0);
        } else if (condition.searchMode() == PolicySearchMode.CONDITION) {
            weights.put("semantic", 30.0);
            if (condition.regionExplicit()) weights.put("region", 25.0);
            if (condition.ageExplicit()) weights.put("age", 15.0);
            if (condition.employmentExplicit()) weights.put("employment", 10.0);
            if (condition.studentExplicit()) weights.put("student", 5.0);
            if (condition.supportTypeExplicit()) weights.put("support", 10.0);
            weights.put("application", 5.0);
        } else {
            weights.put("semantic", 25.0);
            weights.put("lexical", 20.0);
            weights.put("title", 15.0);
            if (condition.regionExplicit()) weights.put("region", 20.0);
            if (condition.ageExplicit()) weights.put("age", 5.0);
            if (condition.employmentExplicit()) weights.put("employment", 5.0);
            if (condition.supportTypeExplicit()) weights.put("support", 5.0);
            weights.put("application", 5.0);
        }
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) return 0;
        double weighted = 0;
        weighted += weights.getOrDefault("semantic", 0.0) * semanticScore;
        weighted += weights.getOrDefault("lexical", 0.0) * Math.max(lexicalScore, keywordScore);
        weighted += weights.getOrDefault("title", 0.0) * titleExactScore;
        weighted += weights.getOrDefault("region", 0.0) * regionScore;
        weighted += weights.getOrDefault("age", 0.0) * ageScore;
        weighted += weights.getOrDefault("employment", 0.0) * employmentScore;
        weighted += weights.getOrDefault("student", 0.0) * studentScore;
        weighted += weights.getOrDefault("support", 0.0) * keywordScore;
        weighted += weights.getOrDefault("application", 0.0) * applicationScore;
        return weighted / totalWeight * 100.0;
    }

    private double applicationScore(Policy policy, List<String> matched) {
        if (policy.isAlwaysOpen() || policy.getDueDate() == null || !policy.getDueDate().isBefore(LocalDate.now())) {
            matched.add("신청 가능 상태 확인");
            return 1.0;
        }
        return 0;
    }

    private String rewriteQuery(String query, PolicySearchCondition condition) {
        return String.join(" ", query, String.join(" ", condition.expandedKeywords()));
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

    private List<String> regionEvidence(Policy policy) {
        return policy.getRegions().stream()
                .map(PolicyRegion::getRegion)
                .map(region -> region.getRegionLevel() + ": " + region.displayName())
                .distinct()
                .toList();
    }

    private void countRegion(RegionMatchStatus status, FilterCounters counters) {
        switch (status) {
            case EXACT_DISTRICT -> counters.districtMatchedCount++;
            case EXACT_CITY -> counters.cityMatchedCount++;
            case PROVINCE_MATCH -> counters.provinceMatchedCount++;
            case NATIONWIDE -> counters.nationwideCandidateCount++;
            case MULTIPLE_REGION_MATCH -> counters.multipleRegionMatchedCount++;
            case UNKNOWN -> counters.regionUnknownCount++;
            case NOT_MATCHED -> counters.regionNotMatchedCount++;
        }
    }

    private int regionSortRank(PolicySearchResultItem item) {
        return switch (item.regionMatchStatus()) {
            case "EXACT_DISTRICT", "EXACT_CITY", "MULTIPLE_REGION_MATCH" -> 0;
            case "PROVINCE_MATCH" -> 1;
            case "NATIONWIDE" -> 2;
            case "UNKNOWN" -> 3;
            default -> 4;
        };
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && StringUtils.hasText(keyword) && value.contains(keyword);
    }

    private String determineFallbackReason(String current, int vectorCount, int lexicalCount, FilterCounters counters,
                                           PolicySearchCondition condition) {
        if (vectorCount == 0 && lexicalCount == 0) return "NO_VECTOR_CANDIDATES";
        if (lexicalCount == 0 && condition.searchMode() == PolicySearchMode.KEYWORD) return "NO_LEXICAL_MATCH";
        if (condition.regionExplicit() && counters.regionFiltered > 0 && counters.regionFiltered >= counters.ageFiltered
                && counters.regionFiltered >= counters.employmentFiltered) return "ALL_REMOVED_BY_REGION";
        if (condition.ageExplicit() && counters.ageFiltered > 0) return "ALL_REMOVED_BY_AGE";
        if (condition.employmentExplicit() && counters.employmentFiltered > 0) return "ALL_REMOVED_BY_EMPLOYMENT";
        if (counters.applicationFiltered > 0) return "ALL_REMOVED_BY_APPLICATION_STATUS";
        return current == null ? "INSUFFICIENT_FINAL_RESULTS" : current;
    }

    private static class FilterCounters {
        int regionFiltered;
        int nationwideCandidateCount;
        int provinceMatchedCount;
        int cityMatchedCount;
        int districtMatchedCount;
        int multipleRegionMatchedCount;
        int regionUnknownCount;
        int regionNotMatchedCount;
        int regionHardFilteredCount;
        int ageFiltered;
        int employmentFiltered;
        int studentFiltered;
        int targetFiltered;
        int applicationFiltered;
    }
}
