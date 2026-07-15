package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchResult;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus;
import com.themoa.youthcentersearch.rag.dto.PolicyCandidateTrace;
import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResponse;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;
import com.themoa.youthcentersearch.rag.dto.TopicRelevanceScore;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatusResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyRagSearchService {
    private static final String REGION_NOT_MATCHED = "REGION_NOT_MATCHED";
    private static final String AGE_NOT_MATCHED = "AGE_NOT_MATCHED";
    private static final String EMPLOYMENT_NOT_MATCHED = "EMPLOYMENT_NOT_MATCHED";
    private static final String STUDENT_NOT_MATCHED = "STUDENT_NOT_MATCHED";

    private final PolicyRepository policyRepository;
    private final RagProperties properties;
    private final RegionMatchEvaluator regionMatchEvaluator;
    private final PolicySearchIntentBuilder intentBuilder;
    private final PolicyDomainClassifier domainClassifier;
    private final PolicySearchPlanService planService;
    private final PolicySearchCandidateRetriever candidateRetriever;
    private final PolicySearchDiagnosticsFactory diagnosticsFactory;
    private final SearchDomainIntentPolicy domainIntentPolicy;
    private final PolicyTargetAudienceClassifier targetAudienceClassifier;
    private final PolicyTargetEligibilityFilter targetEligibilityFilter;
    private final PolicyEmploymentAudienceClassifier employmentAudienceClassifier;
    private final UserEmploymentStatusDetector userEmploymentStatusDetector;
    private final UserEducationStageDetector userEducationStageDetector;
    private final RegionCoverageResultSelector regionCoverageResultSelector;

    @Autowired
    public PolicyRagSearchService(PolicyRepository policyRepository,
                                  RagProperties properties,
                                  RegionMatchEvaluator regionMatchEvaluator,
                                  PolicySearchIntentBuilder intentBuilder,
                                  PolicyDomainClassifier domainClassifier,
                                  PolicySearchPlanService planService,
                                  PolicySearchCandidateRetriever candidateRetriever,
                                  PolicySearchDiagnosticsFactory diagnosticsFactory,
                                  SearchDomainIntentPolicy domainIntentPolicy,
                                  PolicyTargetAudienceClassifier targetAudienceClassifier,
                                  PolicyTargetEligibilityFilter targetEligibilityFilter,
                                  PolicyEmploymentAudienceClassifier employmentAudienceClassifier,
                                  UserEmploymentStatusDetector userEmploymentStatusDetector,
                                  UserEducationStageDetector userEducationStageDetector,
                                  RegionCoverageResultSelector regionCoverageResultSelector) {
        this.policyRepository = policyRepository;
        this.properties = properties;
        this.regionMatchEvaluator = regionMatchEvaluator;
        this.intentBuilder = intentBuilder;
        this.domainClassifier = domainClassifier;
        this.planService = planService;
        this.candidateRetriever = candidateRetriever;
        this.diagnosticsFactory = diagnosticsFactory;
        this.domainIntentPolicy = domainIntentPolicy;
        this.targetAudienceClassifier = targetAudienceClassifier;
        this.targetEligibilityFilter = targetEligibilityFilter;
        this.employmentAudienceClassifier = employmentAudienceClassifier;
        this.userEmploymentStatusDetector = userEmploymentStatusDetector;
        this.userEducationStageDetector = userEducationStageDetector;
        this.regionCoverageResultSelector = regionCoverageResultSelector;
    }

    public PolicySearchResponse search(PolicySearchRequest request) {
        if (!properties.isEnabled()) {
            throw new YouthCenterApiException("""
                    RAG 기능이 비활성화되어 있습니다.
                    RAG_ENABLED=true 설정을 확인하세요.""");
        }
        Instant start = Instant.now();
        int page = request.resolvedPage();
        int size = request.resolvedSize(properties.getSearch().getResultSize());
        int resultSize = Math.max(size, request.resultSize() == null ? size : request.resultSize());
        PolicySearchPlanService.PlannedSearch planned = planService.build(request.query(), resultSize);
        PolicySearchConditionParser.ParsedPolicySearchCondition parsed = planned.parsed();
        PolicySearchPlan plan = planned.plan();
        PolicySearchCondition condition = plan.condition();
        PolicyQuerySemantics semantics = planned.semantics();
        PolicySearchIntent intent = intentBuilder.build(plan);
        SearchQueryType queryType = plan.queryType();
        ResolvedUserRegion userRegion = regionMatchEvaluator.resolveUserRegion(condition.province(), condition.city(), condition.district(),
                condition.regionLevel());
        String fallbackReason = null;
        PolicyCandidateCollection candidates = candidateRetriever.retrieve(plan, intent, userRegion, page, size, resultSize);
        List<Policy> policies = candidates.policies();
        Map<Integer, Double> semanticScores = candidates.semanticScores();
        Map<Integer, Double> lexicalScores = candidates.lexicalScores();
        Map<Integer, Double> titleExactScores = candidates.titleExactScores();
        Map<Integer, Set<CandidateSource>> candidateSources = candidates.candidateSources();
        CandidateCollectionMetrics candidateMetrics = candidates.metrics();

        Map<Integer, PolicyTargetAudienceClassification> targetAudienceByPolicyId = targetAudienceClassifier == null
                ? Map.of()
                : targetAudienceClassifier.classify(policies.stream().map(Policy::getId).toList());
        Map<Integer, PolicyEmploymentAudience> employmentAudienceByPolicyId = employmentAudienceClassifier == null
                ? Map.of()
                : employmentAudienceClassifier.classify(policies.stream().map(Policy::getId).toList());
        UserEmploymentStatusResult userEmploymentStatus = userEmploymentStatusDetector.detect(request.query());
        PolicySearchFilterMetrics counters = new PolicySearchFilterMetrics();
        List<PolicySearchResultItem> allResults = policies.stream()
                .map(policy -> score(policy, condition, intent, userRegion,
                        semanticScores.getOrDefault(policy.getId(), 0.0),
                        lexicalScores.getOrDefault(policy.getId(), 0.0),
                        titleExactScores.getOrDefault(policy.getId(), 0.0),
                        candidateSources.getOrDefault(policy.getId(), Set.of()),
                        counters, queryType, plan,
                        targetAudienceByPolicyId.getOrDefault(policy.getId(), PolicyTargetAudienceClassification.unknown()),
                        employmentAudienceByPolicyId.getOrDefault(policy.getId(), PolicyEmploymentAudience.unknown()),
                        userEmploymentStatus))
                .filter(item -> pass(item, condition, plan, counters))
                .sorted(resultComparator(condition, queryType))
                .toList();
        RegionCoverageResultSelector.Selection selection = regionCoverageResultSelector.select(allResults, page, size, queryType);
        List<PolicySearchResultItem> orderedResults = selection.orderedResults();
        long totalMatched = orderedResults.size();
        int from = Math.min(orderedResults.size(), page * size);
        int to = Math.min(orderedResults.size(), from + size);
        List<PolicySearchResultItem> results = orderedResults.subList(from, to);
        boolean hasNext = to < orderedResults.size();
        if (results.isEmpty()) {
            fallbackReason = determineFallbackReason(candidates.fallbackReason(), semanticScores.size(), candidateMetrics.lexicalCandidateCount(), counters, condition);
        } else {
            fallbackReason = null;
        }

        PolicySearchDiagnostics diagnostics = diagnosticsFactory.create(
                plan,
                intent,
                parsed,
                candidateMetrics,
                counters,
                selection,
                results,
                semanticScores.size(),
                candidates.retried(),
                candidates.mysqlFallbackUsed(),
                fallbackReason,
                Duration.between(start, Instant.now()).toMillis(),
                userEmploymentStatus.status().name(),
                userEmploymentStatus.explicit(),
                String.join(", ", userEmploymentStatus.evidence()),
                userEmploymentStatus.explicit() ? "RULE_EXPLICIT" : "UNKNOWN");
        return new PolicySearchResponse(answer(results, fallbackReason), condition, parsed.parserMode(), parsed.fallback(),
                condition.searchMode().name(), queryType.name(),
                candidateMetrics.vectorCandidateCount(), results.size(),
                totalMatched, page, size, hasNext, results, diagnostics);
    }

    public Map<String, Object> explain(String query, Integer policyId, String sourcePolicyId) {
        Policy policy = policyId != null
                ? policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."))
                : policyRepository.findBySourcePolicyId(sourcePolicyId).orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));
        PolicySearchResponse response = search(new PolicySearchRequest(query, 100, 0, 100));
        List<PolicySearchResultItem> results = response.results();
        PolicySearchResultItem item = null;
        int rank = 0;
        for (int i = 0; i < results.size(); i++) {
            if (policy.getId().equals(results.get(i).policyId())) {
                item = results.get(i);
                rank = i + 1;
                break;
            }
        }

        PolicySearchCondition condition = response.interpretedCondition();
        ResolvedUserRegion userRegion = regionMatchEvaluator.resolveUserRegion(condition.province(), condition.city(), condition.district(),
                condition.regionLevel());
        RegionMatchResult regionMatch = regionMatchEvaluator.evaluate(policy, userRegion);
        ConditionMatchResult age = ageMatch(policy, condition);
        ConditionMatchResult employment = employmentMatch(policy, condition);
        ConditionMatchResult student = studentMatch(policy, condition);
        TargetStageMatchResult targetStage = item == null ? explainTargetStage(query, policy) : null;
        double lexical = item == null ? 0.0 : item.topicScore();
        double title = item != null && (item.candidateSources().contains(CandidateSource.EXACT_TITLE.name())
                || item.candidateSources().contains(CandidateSource.TITLE_PHRASE.name())) ? 1.0 : 0.0;
        String disposition = item != null ? "INCLUDED" : disposition(policy, condition, regionMatch, age, employment, student);
        if (item == null && targetStage != null && targetStage.status() == ConditionMatchStatus.MISMATCH) {
            disposition = "REMOVED_BY_TARGET_STAGE";
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", policy.getId());
        body.put("policyName", policy.getTitle());
        body.put("candidateSources", item == null ? List.of() : item.candidateSources());
        body.put("vectorScores", Map.of(
                "originalQuery", item == null ? 0.0 : item.semanticScore(),
                "intentQuery", item == null ? 0.0 : item.semanticScore(),
                "expandedQuery", item == null ? 0.0 : item.semanticScore(),
                "categoryQuery", item == null ? 0.0 : item.semanticScore()
        ));
        body.put("lexicalScore", Math.round(lexical * 1000.0) / 10.0);
        body.put("titleScore", Math.round(title * 1000.0) / 10.0);
        body.put("categoryScore", null);
        body.put("region", Map.of("status", regionMatch.compatibility().name(), "eligible", regionMatch.eligible(),
                "score", regionMatch.score(), "reason", regionMatch.reason()));
        body.put("age", Map.of("status", age.status().name(), "reason", age.reason()));
        body.put("employment", Map.of("status", employment.status().name(), "reason", employment.reason()));
        body.put("student", Map.of("status", student.status().name(), "reason", student.reason()));
        body.put("targetStage", item == null
                ? Map.of("status", targetStage == null ? "UNKNOWN" : targetStage.status().name(),
                "reason", targetStage == null ? "대상 단계 trace 없음" : targetStage.reason())
                : Map.of("status", item.targetStageMatchStatus(), "reason", item.targetStageMatchReason(),
                "policyStages", item.targetEducationStages(), "evidence", item.targetEducationEvidence()));
        body.put("employmentAudience", item == null
                ? Map.of("status", "UNKNOWN_OR_FILTERED")
                : Map.of("allowedStatuses", item.allowedEmploymentStatuses(),
                "exclusive", item.employmentAudienceExclusive(),
                "evidence", item.employmentAudienceEvidence()));
        body.put("recommendationTier", item == null ? null : item.recommendationTier());
        body.put("recommendationTierReason", item == null ? null : item.recommendationTierReason());
        body.put("topicRelevance", Map.of("status", item == null ? "UNKNOWN_OR_FILTERED" : "MATCH",
                "score", item == null ? 0.0 : Math.round(item.topicScore() * 1000.0) / 10.0));
        body.put("finalDisposition", disposition);
        body.put("finalScore", item == null ? null : item.finalScore());
        body.put("finalRank", item == null ? null : rank);
        body.put("trace", item == null ? null : resultTrace(item, rank));
        body.put("diagnostics", response.diagnostics());
        return body;
    }

    private TargetStageMatchResult explainTargetStage(String query, Policy policy) {
        if (targetAudienceClassifier == null) {
            return TargetStageMatchResult.unknown("대상 단계 classifier가 구성되지 않았습니다.");
        }
        var userStage = userEducationStageDetector.detect(query);
        var target = targetAudienceClassifier.classify(List.of(policy.getId()))
                .getOrDefault(policy.getId(), PolicyTargetAudienceClassification.unknown());
        return targetEligibilityFilter.match(userStage, target);
    }

    private PolicyCandidateTrace resultTrace(PolicySearchResultItem item, int rank) {
        Set<CandidateSource> sources = item.candidateSources().stream()
                .map(CandidateSource::valueOf)
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(CandidateSource.class)));
        PolicyDomainClassification domain = new PolicyDomainClassification(
                com.themoa.youthcentersearch.rag.dto.SearchDomain.valueOf(item.primaryDomain()),
                item.secondaryDomains().stream()
                        .map(com.themoa.youthcentersearch.rag.dto.SearchDomain::valueOf)
                        .collect(java.util.stream.Collectors.toSet()),
                item.supportIntents().stream()
                        .map(com.themoa.youthcentersearch.rag.dto.SupportIntent::valueOf)
                        .collect(java.util.stream.Collectors.toSet()),
                1.0,
                item.domainEvidence()
        );
        return new PolicyCandidateTrace(
                item.policyId(),
                sources,
                sources.contains(CandidateSource.LEXICAL_INDEX) ? rank : null,
                item.topicScore(),
                sources.contains(CandidateSource.VECTOR_NORMALIZED_QUERY) ? rank : null,
                sources.contains(CandidateSource.VECTOR_NORMALIZED_QUERY) ? item.semanticScore() : null,
                sources.contains(CandidateSource.VECTOR_INTENT_QUERY) ? rank : null,
                sources.contains(CandidateSource.VECTOR_INTENT_QUERY) ? item.semanticScore() : null,
                domain,
                !item.needCheckReasons().contains(REGION_NOT_MATCHED),
                !item.needCheckReasons().contains(AGE_NOT_MATCHED),
                !item.needCheckReasons().contains(STUDENT_NOT_MATCHED),
                !item.needCheckReasons().contains(EMPLOYMENT_NOT_MATCHED),
                item.excludedDomainPassed(),
                null,
                null,
                item.finalScore(),
                rank
        );
    }

    private String disposition(Policy policy, PolicySearchCondition condition, RegionMatchResult regionMatch,
                               ConditionMatchResult age, ConditionMatchResult employment, ConditionMatchResult student) {
        if (condition.regionExplicit() && !regionMatch.eligible()) return "REMOVED_BY_REGION";
        if (condition.ageExplicit() && age.status() == ConditionMatchStatus.MISMATCH) return "REMOVED_BY_AGE";
        if (condition.employmentExplicit() && employment.status() == ConditionMatchStatus.MISMATCH) return "REMOVED_BY_EMPLOYMENT";
        if (condition.studentExplicit() && student.status() == ConditionMatchStatus.MISMATCH) return "REMOVED_BY_STUDENT";
        if ("CLOSED".equals(policy.getStatus())) return "REMOVED_BY_APPLICATION_STATUS";
        return "NOT_IN_TRACE_OR_BELOW_RESULT_LIMIT";
    }

    private PolicySearchResultItem score(Policy policy, PolicySearchCondition condition, PolicySearchIntent intent,
                                         ResolvedUserRegion userRegion, double semanticScore, double lexicalScore,
                                         double titleExactScore, Set<CandidateSource> sources, PolicySearchFilterMetrics counters,
                                         SearchQueryType queryType, PolicySearchPlan plan,
                                         PolicyTargetAudienceClassification targetAudience,
                                         PolicyEmploymentAudience employmentAudience,
                                         UserEmploymentStatusResult userEmploymentStatus) {
        List<String> matched = new ArrayList<>();
        List<String> needCheck = new ArrayList<>();
        double directTitleScore = directTitlePhraseScore(policy, condition, intent);
        if (directTitleScore >= 0.9) {
            titleExactScore = Math.max(titleExactScore, directTitleScore);
            sources = EnumSet.copyOf(sources.isEmpty() ? Set.of(CandidateSource.TITLE_PHRASE) : sources);
            sources.add(directTitleScore >= 1.0 ? CandidateSource.EXACT_TITLE : CandidateSource.TITLE_PHRASE);
        }
        RegionMatchResult regionMatch = regionMatchEvaluator.evaluate(policy, userRegion);
        countRegion(regionMatch.compatibility(), counters);
        if (regionMatch.eligible()) counters.regionEligibleCount++;
        else counters.regionIneligibleCount++;
        if (condition.regionExplicit() && regionMatch.compatibility() == RegionCompatibility.NOT_MATCHED) {
            needCheck.add(REGION_NOT_MATCHED);
        } else if (condition.regionExplicit() && regionMatch.compatibility() == RegionCompatibility.UNKNOWN) {
            needCheck.add("REGION_UNKNOWN");
        } else {
            matched.add(regionMatch.reason());
        }
        ConditionMatchResult ageMatch = ageMatch(policy, condition);
        ConditionMatchResult employmentMatch = employmentMatch(policy, condition);
        ConditionMatchResult studentMatch = studentMatch(policy, condition);
        TargetStageMatchResult targetStageMatch = targetEligibilityFilter.match(plan, targetAudience);
        applyCondition("나이", ageMatch, condition.ageExplicit(), matched, needCheck, counters);
        applyCondition("취업 상태", employmentMatch, condition.employmentExplicit(), matched, needCheck, counters);
        applyCondition("학생 상태", studentMatch, condition.studentExplicit(), matched, needCheck, counters);
        if (plan.educationStageExplicit()) {
            applyTargetStage(targetStageMatch, matched, needCheck, counters);
        }
        double applicationScore = applicationScore(policy, matched);
        TopicRelevanceScore topicScore = topicRelevance(policy, condition, semanticScore, lexicalScore, titleExactScore);
        if (passesTopicThreshold(topicScore, titleExactScore, queryType)) {
            counters.topicThresholdPassedCount++;
        } else {
            counters.topicThresholdFailedCount++;
            needCheck.add("TOPIC_NOT_RELEVANT");
        }
        double finalScore = finalScore(condition, semanticScore, lexicalScore, titleExactScore,
                ageMatch.score(), employmentMatch.score(), studentMatch.score(), applicationScore,
                topicScore.finalTopicScore());
        if (lexicalScore > 0) matched.add("키워드 일치");
        if (titleExactScore >= 0.8) matched.add("정책명 정확도 높음");
        PolicyDomainClassification domain = domainClassifier.classify(policy);
        boolean excludedDomainPassed = !domainIntentPolicy.isExcluded(domain, plan);
        EmploymentAudienceMatch employmentAudienceMatch = employmentAudienceMatch(userEmploymentStatus, employmentAudience);
        if (employmentAudienceMatch.status() == ConditionMatchStatus.MISMATCH) {
            needCheck.add("EMPLOYMENT_AUDIENCE_NOT_MATCHED");
        } else if (employmentAudienceMatch.status() == ConditionMatchStatus.UNKNOWN) {
            needCheck.add("취업 대상 확인 필요: " + employmentAudienceMatch.reason());
        } else {
            matched.add("취업 대상 일치: " + employmentAudienceMatch.reason());
        }
        Recommendation recommendation = recommendation(plan, targetAudience, targetStageMatch, employmentAudience, employmentAudienceMatch);
        countRecommendation(recommendation.tier(), counters);
        PolicyCondition pc = policy.getCondition();
        return new PolicySearchResultItem(policy.getId(), policy.getSourcePolicyId(), policy.getTitle(),
                policy.getCategory().name(), regionText(policy), regionMatch.compatibility().name(), regionMatch.label(),
                regionEvidence(policy), policy.getAgencyName(), policy.getSummary(),
                pc == null ? null : pc.getMinAge(), pc == null ? null : pc.getMaxAge(),
                pc == null ? null : pc.getEmploymentStatus(), policy.getStartDate(), policy.getDueDate(),
                policy.getStatus(), policy.getOfficialUrl(), Math.round(semanticScore * 1000.0) / 1000.0,
                Math.round(finalScore * 10.0) / 10.0, matched, needCheck,
                regionMatch.compatibility().name(), regionMatch.label(), regionMatch.reason(), regionMatch.score(),
                sources.stream().map(Enum::name).sorted().toList(),
                ageMatch.status().name(), ageMatch.reason(),
                employmentMatch.status().name(), employmentMatch.reason(),
                studentMatch.status().name(), studentMatch.reason(),
                Math.round(topicScore.finalTopicScore() * 1000.0) / 1000.0,
                domain.primaryDomain().name(),
                domain.secondaryDomains().stream().map(Enum::name).sorted().toList(),
                domain.supportIntents().stream().map(Enum::name).sorted().toList(),
                domain.evidence(),
                excludedDomainPassed,
                targetAudience.includedStages().stream().map(Enum::name).sorted().toList(),
                targetAudience.excludedStages().stream().map(Enum::name).sorted().toList(),
                targetAudience.evidence(),
                targetStageMatch.status().name(),
                targetStageMatch.reason(),
                employmentAudience.allowedStatuses().stream().map(Enum::name).sorted().toList(),
                employmentAudience.exclusive(),
                employmentAudience.evidence(),
                recommendation.tier().name(),
                recommendation.reason());
    }

    private boolean pass(PolicySearchResultItem item, PolicySearchCondition condition, PolicySearchPlan plan,
                         PolicySearchFilterMetrics counters) {
        if ("CLOSED".equals(item.applicationStatus())) {
            counters.applicationFiltered++;
            return false;
        }
        if (condition.regionExplicit() && item.needCheckReasons().contains(REGION_NOT_MATCHED)) {
            counters.regionFiltered++;
            counters.regionHardFilteredCount++;
            counters.wrongRegionExcludedCount++;
            return false;
        }
        if (condition.regionExplicit() && item.needCheckReasons().contains("REGION_UNKNOWN")
                && !properties.getSearch().isIncludeUnknownRegion()) {
            counters.regionFiltered++;
            counters.unknownExcludedCount++;
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
        if (item.needCheckReasons().contains("EMPLOYMENT_AUDIENCE_NOT_MATCHED")) {
            if (item.allowedEmploymentStatuses().contains(UserEmploymentStatus.UNEMPLOYED.name())) {
                counters.employedMismatchFiltered++;
            }
            if (item.allowedEmploymentStatuses().contains(UserEmploymentStatus.EMPLOYED.name())) {
                counters.unemployedMismatchFiltered++;
            }
            counters.employmentFiltered++;
            return false;
        }
        if (condition.studentExplicit() && item.needCheckReasons().contains(STUDENT_NOT_MATCHED)) {
            counters.studentFiltered++;
            return false;
        }
        if (item.needCheckReasons().contains("TARGET_STAGE_NOT_MATCHED")) {
            counters.targetFiltered++;
            return false;
        }
        if (item.needCheckReasons().contains("TOPIC_NOT_RELEVANT")) {
            counters.topicFilteredCount++;
            return false;
        }
        if (!item.excludedDomainPassed()) {
            counters.excludedDomainFiltered++;
            return false;
        }
        if (!desiredDomainPasses(item, plan)) {
            counters.topicFilteredCount++;
            return false;
        }
        return true;
    }

    private boolean desiredDomainPasses(PolicySearchResultItem item, PolicySearchPlan plan) {
        Set<com.themoa.youthcentersearch.rag.dto.SearchDomain> desired = plan.desiredDomains().stream()
                .filter(domain -> domain != com.themoa.youthcentersearch.rag.dto.SearchDomain.GENERAL)
                .collect(java.util.stream.Collectors.toSet());
        if (desired.isEmpty() && plan.desiredSupportIntents().isEmpty()) {
            return true;
        }
        if (desired.stream().anyMatch(domain -> domain.name().equals(item.primaryDomain()))) {
            return true;
        }
        if (item.secondaryDomains().stream().anyMatch(name -> desired.stream().anyMatch(domain -> domain.name().equals(name)))) {
            return true;
        }
        return item.supportIntents().stream().anyMatch(name -> plan.desiredSupportIntents().stream()
                .anyMatch(intent -> intent.name().equals(name)));
    }

    private Comparator<PolicySearchResultItem> resultComparator(PolicySearchCondition condition, SearchQueryType queryType) {
        return (left, right) -> {
            if (queryType == SearchQueryType.POLICY_NAME) {
                int titlePriority = Integer.compare(titlePriority(right), titlePriority(left));
                if (titlePriority != 0) {
                    return titlePriority;
                }
            }
            int tier = Integer.compare(tierPriority(left), tierPriority(right));
            if (tier != 0) {
                return tier;
            }
            if (queryType != SearchQueryType.POLICY_NAME) {
                int titlePriority = Integer.compare(titlePriority(right), titlePriority(left));
                if (titlePriority != 0) {
                    return titlePriority;
                }
            }
            double diff = right.finalScore() - left.finalScore();
            if (condition.regionExplicit() && Math.abs(diff) <= properties.getSearch().getRegionSpecificityTieWindow() * 100.0) {
                int region = Integer.compare(specificity(left.regionCompatibility()), specificity(right.regionCompatibility()));
                if (region != 0) {
                    return region;
                }
            }
            if (diff > 0) return 1;
            if (diff < 0) return -1;
            return Integer.compare(left.policyId(), right.policyId());
        };
    }

    private int tierPriority(PolicySearchResultItem item) {
        return RecommendationTier.PRIMARY.name().equals(item.recommendationTier()) ? 0 : 1;
    }

    private int titlePriority(PolicySearchResultItem item) {
        if (item.candidateSources().contains(CandidateSource.EXACT_TITLE.name())) return 2;
        if (item.candidateSources().contains(CandidateSource.TITLE_PHRASE.name())) return 1;
        return 0;
    }

    private int specificity(String compatibility) {
        return switch (RegionCompatibility.valueOf(compatibility)) {
            case EXACT_SIGUNGU -> 0;
            case PARENT_SIDO, EXACT_SIDO -> 1;
            case NATIONWIDE -> 2;
            case MULTIPLE_REGION_MATCH -> 3;
            case UNKNOWN -> 4;
            case NOT_MATCHED -> 5;
        };
    }

    private ConditionMatchResult ageMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.ageExplicit() || condition.age() == null || policy.getCondition() == null) {
            return ConditionMatchResult.unknown("나이 조건을 검색 필터로 사용하지 않았습니다.");
        }
        Integer min = policy.getCondition().getMinAge();
        Integer max = policy.getCondition().getMaxAge();
        if (min == null && max == null) {
            return ConditionMatchResult.unknown("정책 나이 조건 정보가 없습니다.");
        }
        if ((min == null || condition.age() >= min) && (max == null || condition.age() <= max)) {
            return ConditionMatchResult.match("사용자 " + condition.age() + "세가 정책 나이 범위에 포함됩니다.");
        }
        return ConditionMatchResult.mismatch("사용자 " + condition.age() + "세가 정책 나이 범위를 벗어납니다.");
    }

    private ConditionMatchResult employmentMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.employmentExplicit() || !StringUtils.hasText(condition.employmentStatus())) {
            return ConditionMatchResult.unknown("취업 상태를 검색 필터로 사용하지 않았습니다.");
        }
        String policyEmployment = policy.getCondition() == null ? null : policy.getCondition().getEmploymentStatus();
        if (!StringUtils.hasText(policyEmployment)) {
            if (employmentSoftSignal(policy)) {
                return ConditionMatchResult.unknown("구조화된 취업 조건은 없지만 정책 내용에 취업·구직 관련 표현이 있습니다.");
            }
            return ConditionMatchResult.unknown("정책 취업 상태 조건 정보가 없습니다.");
        }
        if (condition.employmentStatus().equals(policy.getCondition().getEmploymentStatus())) {
            return ConditionMatchResult.match("취업 상태 조건이 일치합니다.");
        }
        return ConditionMatchResult.mismatch("정책 취업 상태 조건이 사용자 조건과 다릅니다.");
    }

    private ConditionMatchResult studentMatch(Policy policy, PolicySearchCondition condition) {
        if (!condition.studentExplicit() || condition.studentStatus() == null || policy.getCondition() == null
                || policy.getCondition().getStudentStatus() == null) {
            return ConditionMatchResult.unknown("학생 상태 조건 정보가 없거나 검색 필터로 사용하지 않았습니다.");
        }
        if (condition.studentStatus().equals(policy.getCondition().getStudentStatus())) {
            return ConditionMatchResult.match("학생 상태 조건이 일치합니다.");
        }
        return ConditionMatchResult.mismatch("학생 상태 조건이 사용자 조건과 다릅니다.");
    }

    private void applyCondition(String label, ConditionMatchResult result, boolean explicit,
                                List<String> matched, List<String> needCheck, PolicySearchFilterMetrics counters) {
        if (!explicit) {
            return;
        }
        if ("나이".equals(label)) countAge(result.status(), counters);
        if ("취업 상태".equals(label)) countEmployment(result.status(), counters);
        if (result.status() == ConditionMatchStatus.MATCH) {
            matched.add(label + " 조건 일치: " + result.reason());
        } else if (result.status() == ConditionMatchStatus.UNKNOWN) {
            needCheck.add(label + " 확인 필요: " + result.reason());
        } else if ("나이".equals(label)) {
            needCheck.add(AGE_NOT_MATCHED);
        } else if ("취업 상태".equals(label)) {
            needCheck.add(EMPLOYMENT_NOT_MATCHED);
        } else if ("학생 상태".equals(label)) {
            needCheck.add(STUDENT_NOT_MATCHED);
        }
    }

    private void applyTargetStage(TargetStageMatchResult result, List<String> matched,
                                  List<String> needCheck, PolicySearchFilterMetrics counters) {
        if (result.status() == ConditionMatchStatus.MATCH) {
            matched.add("대상 교육 단계 일치: " + result.reason());
        } else if (result.status() == ConditionMatchStatus.MISMATCH) {
            needCheck.add("TARGET_STAGE_NOT_MATCHED");
        } else {
            counters.targetUnknownCount++;
            needCheck.add("대상 교육 단계 확인 필요: " + result.reason());
        }
    }

    private EmploymentAudienceMatch employmentAudienceMatch(UserEmploymentStatusResult user, PolicyEmploymentAudience audience) {
        if (user == null || !user.explicit() || user.status() == UserEmploymentStatus.UNKNOWN) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "사용자 취업 상태가 명시되지 않았습니다.");
        }
        if (audience == null || audience.allowedStatuses().contains(UserEmploymentStatus.UNKNOWN)) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "정책 취업 대상 상태 확인 필요");
        }
        if (audience.allowedStatuses().contains(user.status())) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.MATCH, "사용자 취업 상태가 정책 대상에 포함됩니다.");
        }
        if (audience.exclusive()) {
            return new EmploymentAudienceMatch(ConditionMatchStatus.MISMATCH, "정책 취업 대상 상태가 사용자 상태와 다릅니다.");
        }
        return new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "정책 취업 대상 상태가 배타적인지 확인 필요");
    }

    private Recommendation recommendation(PolicySearchPlan plan,
                                          PolicyTargetAudienceClassification targetAudience,
                                          TargetStageMatchResult targetStageMatch,
                                          PolicyEmploymentAudience employmentAudience,
                                          EmploymentAudienceMatch employmentAudienceMatch) {
        if (targetStageMatch.status() == ConditionMatchStatus.MISMATCH
                || employmentAudienceMatch.status() == ConditionMatchStatus.MISMATCH) {
            return new Recommendation(RecommendationTier.MISMATCH, "명시 조건과 정책 대상이 불일치합니다.");
        }
        if (!plan.educationStageExplicit()
                && targetAudience.stageExclusive()
                && targetAudience.includedStages().stream().anyMatch(stage -> switch (stage) {
            case ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, UNIVERSITY, GRADUATE_SCHOOL -> true;
            default -> false;
        })) {
            return new Recommendation(RecommendationTier.NEEDS_CONFIRMATION, "사용자가 해당 교육 단계 소속을 밝히지 않았습니다.");
        }
        if (employmentAudienceMatch.status() == ConditionMatchStatus.UNKNOWN
                && employmentAudience.exclusive()) {
            return new Recommendation(RecommendationTier.NEEDS_CONFIRMATION, "정책 취업 대상 상태 확인이 필요합니다.");
        }
        return new Recommendation(RecommendationTier.PRIMARY, "명시 조건과 충돌하지 않는 기본 추천 후보입니다.");
    }

    private void countRecommendation(RecommendationTier tier, PolicySearchFilterMetrics counters) {
        if (tier == RecommendationTier.PRIMARY) {
            counters.primaryCandidateCount++;
        } else if (tier == RecommendationTier.NEEDS_CONFIRMATION) {
            counters.needsConfirmationCandidateCount++;
        }
    }

    private TopicRelevanceScore topicRelevance(Policy policy, PolicySearchCondition condition,
                                               double semanticScore, double lexicalScore, double titleExactScore) {
        double categoryScore = 0;
        if (StringUtils.hasText(condition.category()) && policy.getCategory() != null
                && policy.getCategory().name().contains(condition.category())) {
            categoryScore = 1.0;
        }
        double intentScore = 0.0;
        double finalTopic = Math.max(titleExactScore, semanticScore * 0.35 + lexicalScore * 0.25
                + categoryScore * 0.15);
        return new TopicRelevanceScore(semanticScore, lexicalScore, titleExactScore, categoryScore, intentScore, finalTopic);
    }

    private boolean passesTopicThreshold(TopicRelevanceScore topicScore, double titleExactScore, SearchQueryType queryType) {
        if (queryType == SearchQueryType.BROAD_DISCOVERY) {
            return true;
        }
        if (queryType == SearchQueryType.POLICY_NAME && titleExactScore >= 0.75) {
            return true;
        }
        double threshold = queryType == SearchQueryType.ELIGIBILITY_SEARCH
                ? properties.getSearch().getMinimumTopicRelevance() * 0.75
                : properties.getSearch().getMinimumTopicRelevance();
        return topicScore.finalTopicScore() >= threshold;
    }

    private double finalScore(PolicySearchCondition condition, double semanticScore, double lexicalScore, double titleExactScore,
                              double ageScore, double employmentScore, double studentScore,
                              double applicationScore, double topicScore) {
        Map<String, Double> weights = new LinkedHashMap<>();
        if (condition.searchMode() == PolicySearchMode.KEYWORD) {
            weights.put("topic", 35.0);
            weights.put("lexical", 45.0);
            weights.put("semantic", 35.0);
            weights.put("title", 35.0);
            weights.put("application", 5.0);
        } else if (condition.searchMode() == PolicySearchMode.CONDITION) {
            weights.put("topic", 35.0);
            weights.put("semantic", 30.0);
            if (condition.ageExplicit()) weights.put("age", 15.0);
            if (condition.employmentExplicit()) weights.put("employment", 10.0);
            if (condition.studentExplicit()) weights.put("student", 5.0);
            weights.put("application", 5.0);
        } else {
            weights.put("topic", 35.0);
            weights.put("semantic", 25.0);
            weights.put("lexical", 20.0);
            weights.put("title", 15.0);
            if (condition.ageExplicit()) weights.put("age", 5.0);
            if (condition.employmentExplicit()) weights.put("employment", 5.0);
            weights.put("application", 5.0);
        }
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) return 0;
        double weighted = 0;
        weighted += weights.getOrDefault("topic", 0.0) * topicScore;
        weighted += weights.getOrDefault("semantic", 0.0) * semanticScore;
        weighted += weights.getOrDefault("lexical", 0.0) * lexicalScore;
        weighted += weights.getOrDefault("title", 0.0) * titleExactScore;
        weighted += weights.getOrDefault("age", 0.0) * ageScore;
        weighted += weights.getOrDefault("employment", 0.0) * employmentScore;
        weighted += weights.getOrDefault("student", 0.0) * studentScore;
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

    private String answer(List<PolicySearchResultItem> results, String fallbackReason) {
        if (results.isEmpty()) {
            return switch (fallbackReason == null ? "" : fallbackReason) {
                case "ALL_REMOVED_BY_REGION" -> "검색 후보는 있었지만 입력한 지역과 호환되는 정책이 최종 결과에 남지 않았습니다. 지역 데이터 또는 정책 지역 판정 상태를 확인하세요.";
                case "NO_TOPIC_RELEVANT_CANDIDATES" -> "지역 또는 조건 후보는 있었지만 검색 의도와 충분히 관련 있는 정책이 남지 않았습니다. 키워드를 조금 더 구체화해 보세요.";
                case "ALL_REMOVED_BY_AGE" -> "검색 후보는 있었지만 입력한 나이 조건과 명확히 맞지 않아 제외되었습니다.";
                case "ALL_REMOVED_BY_EMPLOYMENT" -> "검색 후보는 있었지만 입력한 취업 상태 조건과 명확히 맞지 않아 제외되었습니다.";
                case "ALL_REMOVED_BY_APPLICATION_STATUS" -> "검색 후보는 있었지만 신청 마감 상태로 제외되었습니다.";
                case "NO_CANDIDATES" -> "검색 후보를 찾지 못했습니다. 키워드를 넓혀 다시 검색해 보세요.";
                default -> "검색 후보는 있었지만 최종 표시 기준을 통과한 정책이 없습니다. 검색 진단의 제외 사유를 확인하세요.";
            };
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

    private void countRegion(RegionCompatibility compatibility, PolicySearchFilterMetrics counters) {
        switch (compatibility) {
            case EXACT_SIGUNGU -> {
                counters.exactSigunguMatchedCount++;
                counters.cityMatchedCount++;
            }
            case EXACT_SIDO -> {
                counters.exactSidoMatchedCount++;
                counters.provinceMatchedCount++;
            }
            case PARENT_SIDO -> {
                counters.parentSidoMatchedCount++;
                counters.provinceMatchedCount++;
            }
            case NATIONWIDE -> {
                counters.nationwideMatchedCount++;
                counters.nationwideCandidateCount++;
            }
            case MULTIPLE_REGION_MATCH -> counters.multipleRegionMatchedCount++;
            case UNKNOWN -> counters.regionUnknownCount++;
            case NOT_MATCHED -> counters.regionNotMatchedCount++;
        }
    }

    private double directTitlePhraseScore(Policy policy, PolicySearchCondition condition, PolicySearchIntent intent) {
        String title = normalize(policy.getTitle());
        String query = normalize(intent.originalQuery());
        if (!StringUtils.hasText(title) || !StringUtils.hasText(query)) {
            return 0;
        }
        for (String term : List.of("청년", "정책", "지원", "받을수있는", "알려줘", "추천",
                normalize(condition.province()), normalize(condition.city()), normalize(condition.district()))) {
            if (StringUtils.hasText(term)) {
                query = query.replace(term, "");
            }
        }
        if (query.length() >= 3 && title.contains(query)) {
            return title.equals(query) ? 1.0 : 0.98;
        }
        return 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\s\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }

    private boolean employmentSoftSignal(Policy policy) {
        String text = String.join(" ",
                nullToEmpty(policy.getTitle()),
                nullToEmpty(policy.getSummary()),
                policy.getCondition() == null ? "" : nullToEmpty(policy.getCondition().getConditionSummary()));
        return containsAny(text, "구직자", "미취업 청년", "취업 준비", "취업준비", "면접", "구직활동", "취업지원", "일자리");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void countAge(ConditionMatchStatus status, PolicySearchFilterMetrics counters) {
        switch (status) {
            case MATCH -> counters.ageMatchedCount++;
            case UNKNOWN -> counters.ageUnknownCount++;
            case MISMATCH -> counters.ageMismatchedCount++;
        }
    }

    private void countEmployment(ConditionMatchStatus status, PolicySearchFilterMetrics counters) {
        switch (status) {
            case MATCH -> counters.employmentMatchedCount++;
            case UNKNOWN -> counters.employmentUnknownCount++;
            case MISMATCH -> counters.employmentMismatchedCount++;
        }
    }

    private String determineFallbackReason(String current, int vectorCount, int lexicalCount, PolicySearchFilterMetrics counters,
                                           PolicySearchCondition condition) {
        if (vectorCount == 0 && lexicalCount == 0) return "NO_CANDIDATES";
        if (lexicalCount == 0 && condition.searchMode() == PolicySearchMode.KEYWORD) return "NO_LEXICAL_MATCH";
        if (condition.regionExplicit() && counters.regionEligibleCount == 0 && counters.regionFiltered > 0) return "ALL_REMOVED_BY_REGION";
        if (counters.hasNoTopicRelevantCandidate()) {
            return "NO_TOPIC_RELEVANT_CANDIDATES";
        }
        if (condition.regionExplicit() && counters.regionFiltered > 0 && counters.regionFiltered >= counters.ageFiltered
                && counters.regionFiltered >= counters.employmentFiltered) return "ALL_REMOVED_BY_REGION";
        if (condition.ageExplicit() && counters.ageFiltered > 0) return "ALL_REMOVED_BY_AGE";
        if (condition.employmentExplicit() && counters.employmentFiltered > 0) return "ALL_REMOVED_BY_EMPLOYMENT";
        if (counters.applicationFiltered > 0) return "ALL_REMOVED_BY_APPLICATION_STATUS";
        return current == null ? "INSUFFICIENT_FINAL_RESULTS" : current;
    }

    private record EmploymentAudienceMatch(ConditionMatchStatus status, String reason) {
    }

    private record Recommendation(RecommendationTier tier, String reason) {
    }

}
