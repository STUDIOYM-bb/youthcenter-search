package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import com.themoa.youthcentersearch.rag.dto.TopicRelevanceScore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 자격을 통과한 정책 후보의 관련도와 최종 순위를 계산하는 서비스다.
 *
 * <p>PolicyEligibilityEvaluator 이후, PolicySearchResultAssembler 이전에 호출된다. Topic relevance,
 * 제목 일치, lexical/vector 점수, domain/support intent 관련도, RecommendationTier 정렬을 담당한다.</p>
 *
 * <p>입력은 자격 평가 결과와 SearchPlan이며 출력은 finalRank가 확정된 RankedPolicyCandidate 목록이다.
 * DB, Qdrant, MySQL 후보 조회는 수행하지 않는다. 지역·나이 같은 명확한 자격 불일치는 이 단계에
 * 오기 전에 제거되어야 하며, 새 hard filter는 PolicyEligibilityEvaluator에 추가해야 한다.</p>
 */
@Service
public class PolicyRankingService {
    private final RagProperties properties;
    private final PolicyDomainClassifier domainClassifier;
    private final SearchDomainIntentPolicy domainIntentPolicy;

    public PolicyRankingService(RagProperties properties,
                                PolicyDomainClassifier domainClassifier,
                                SearchDomainIntentPolicy domainIntentPolicy) {
        this.properties = properties;
        this.domainClassifier = domainClassifier;
        this.domainIntentPolicy = domainIntentPolicy;
    }

    public PolicyRankingResult rank(PolicySearchExecutionContext context, PolicyEvaluationResult evaluated) {
        PolicySearchPlan plan = context.plan();
        List<RankedPolicyCandidate> preliminary = new ArrayList<>();
        for (EvaluatedPolicyCandidate candidate : evaluated.passedCandidates()) {
            PolicyRankingEvaluation ranking = score(candidate, plan, evaluated.metrics());
            if (ranking != null) {
                preliminary.add(new RankedPolicyCandidate(candidate, ranking));
            }
        }
        List<RankedPolicyCandidate> ordered = preliminary.stream()
                .sorted(resultComparator(plan.condition(), plan.queryType()))
                .toList();
        List<RankedPolicyCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            RankedPolicyCandidate current = ordered.get(i);
            PolicyRankingEvaluation r = current.ranking();
            // 페이지네이션 전에 전체 순위를 확정해야 목록과 Explain이 서로 다른 rank를 말하지 않는다.
            ranked.add(new RankedPolicyCandidate(current.candidate(), new PolicyRankingEvaluation(
                    r.topicScore(), r.semanticScore(), r.lexicalScore(), r.titleScore(), r.domainScore(),
                    r.supportIntentScore(), r.regionScore(), r.finalScore(), r.recommendationTier(), i + 1,
                    r.rankingReasons())));
        }
        return new PolicyRankingResult(ranked, evaluated.metrics());
    }

    private PolicyRankingEvaluation score(EvaluatedPolicyCandidate candidate,
                                          PolicySearchPlan plan,
                                          PolicySearchFilterMetrics metrics) {
        Policy policy = candidate.policy();
        CandidateEvidence evidence = candidate.candidateEvidence();
        PolicySearchCondition condition = plan.condition();
        List<String> reasons = new ArrayList<>();
        double titleScore = Math.max(evidence.titleExactScore(), directTitlePhraseScore(policy, condition, plan));
        if (titleScore >= 0.8) reasons.add("정책명 정확도 높음");
        if (evidence.lexicalScore() > 0) reasons.add("키워드 일치");
        double applicationScore = applicationScore(policy, reasons);
        TopicRelevanceScore topicScore = topicRelevance(policy, condition, evidence.semanticScore(), evidence.lexicalScore(), titleScore);
        if (passesTopicThreshold(topicScore, titleScore, plan.queryType())) {
            metrics.topicThresholdPassedCount++;
        } else {
            metrics.topicThresholdFailedCount++;
            metrics.topicFilteredCount++;
            return null;
        }
        PolicyDomainClassification domain = domainClassifier.classify(policy);
        if (domainIntentPolicy.isExcluded(domain, plan)) {
            metrics.excludedDomainFiltered++;
            return null;
        }
        if (!desiredDomainPasses(domain, plan)) {
            metrics.topicFilteredCount++;
            return null;
        }
        double domainScore = domainScore(domain, plan);
        double supportIntentScore = supportIntentScore(domain, plan);
        double finalScore = finalScore(condition, evidence.semanticScore(), evidence.lexicalScore(), titleScore,
                candidate.eligibility().ageMatch().score(), candidate.eligibility().employmentMatch().score(),
                candidate.eligibility().studentMatch().score(), applicationScore, topicScore.finalTopicScore());
        return new PolicyRankingEvaluation(round(topicScore.finalTopicScore(), 3), round(evidence.semanticScore(), 3),
                round(evidence.lexicalScore(), 3), round(titleScore, 3), round(domainScore, 3),
                round(supportIntentScore, 3), candidate.eligibility().regionMatch().score(),
                round(finalScore, 1), candidate.eligibility().preliminaryTier(), 0, reasons);
    }

    private TopicRelevanceScore topicRelevance(Policy policy, PolicySearchCondition condition,
                                               double semanticScore, double lexicalScore, double titleExactScore) {
        double categoryScore = 0;
        if (StringUtils.hasText(condition.category()) && policy.getCategory() != null
                && policy.getCategory().name().contains(condition.category())) {
            categoryScore = 1.0;
        }
        double finalTopic = Math.max(titleExactScore, semanticScore * 0.35 + lexicalScore * 0.25 + categoryScore * 0.15);
        return new TopicRelevanceScore(semanticScore, lexicalScore, titleExactScore, categoryScore, 0.0, finalTopic);
    }

    private boolean passesTopicThreshold(TopicRelevanceScore topicScore, double titleExactScore, SearchQueryType queryType) {
        if (queryType == SearchQueryType.BROAD_DISCOVERY) return true;
        if (queryType == SearchQueryType.POLICY_NAME && titleExactScore >= 0.75) return true;
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

    private Comparator<RankedPolicyCandidate> resultComparator(PolicySearchCondition condition, SearchQueryType queryType) {
        return (left, right) -> {
            if (queryType == SearchQueryType.POLICY_NAME) {
                // 정책명 검색은 사용자가 특정 명칭을 찾는 상황이므로 exact title을 tier보다 먼저 둔다.
                int titlePriority = Integer.compare(titlePriority(right), titlePriority(left));
                if (titlePriority != 0) return titlePriority;
            }
            int tier = Integer.compare(tierPriority(left), tierPriority(right));
            if (tier != 0) return tier;
            if (queryType != SearchQueryType.POLICY_NAME) {
                int titlePriority = Integer.compare(titlePriority(right), titlePriority(left));
                if (titlePriority != 0) return titlePriority;
            }
            double diff = right.ranking().finalScore() - left.ranking().finalScore();
            if (condition.regionExplicit() && Math.abs(diff) <= properties.getSearch().getRegionSpecificityTieWindow() * 100.0) {
                int region = Integer.compare(specificity(left.candidate().eligibility().regionMatch().compatibility()),
                        specificity(right.candidate().eligibility().regionMatch().compatibility()));
                if (region != 0) return region;
            }
            if (diff > 0) return 1;
            if (diff < 0) return -1;
            return Integer.compare(left.candidate().policy().getId(), right.candidate().policy().getId());
        };
    }

    private boolean desiredDomainPasses(PolicyDomainClassification domain, PolicySearchPlan plan) {
        Set<SearchDomain> desired = plan.desiredDomains().stream()
                .filter(item -> item != SearchDomain.GENERAL)
                .collect(Collectors.toSet());
        if (desired.isEmpty() && plan.desiredSupportIntents().isEmpty()) return true;
        if (desired.contains(domain.primaryDomain())) return true;
        if (domain.secondaryDomains().stream().anyMatch(desired::contains)) return true;
        return domain.supportIntents().stream().anyMatch(plan.desiredSupportIntents()::contains);
    }

    private double domainScore(PolicyDomainClassification domain, PolicySearchPlan plan) {
        Set<SearchDomain> desired = plan.desiredDomains().stream()
                .filter(item -> item != SearchDomain.GENERAL)
                .collect(Collectors.toSet());
        if (desired.isEmpty()) return 0.0;
        if (desired.contains(domain.primaryDomain())) return 1.0;
        if (domain.secondaryDomains().stream().anyMatch(desired::contains)) return 0.75;
        return 0.0;
    }

    private double supportIntentScore(PolicyDomainClassification domain, PolicySearchPlan plan) {
        Set<SupportIntent> desired = plan.desiredSupportIntents();
        if (desired.isEmpty() || desired.contains(SupportIntent.GENERAL)) return 0.0;
        return domain.supportIntents().stream().anyMatch(desired::contains) ? 1.0 : 0.0;
    }

    private int tierPriority(RankedPolicyCandidate item) {
        return item.ranking().recommendationTier() == RecommendationTier.PRIMARY ? 0 : 1;
    }

    private int titlePriority(RankedPolicyCandidate item) {
        Set<CandidateSource> sources = sources(item.candidate().candidateEvidence());
        if (sources.contains(CandidateSource.EXACT_TITLE)) return 2;
        if (sources.contains(CandidateSource.TITLE_PHRASE)) return 1;
        if (item.ranking().titleScore() >= 1.0) return 2;
        if (item.ranking().titleScore() >= 0.75) return 1;
        return 0;
    }

    private int specificity(RegionCompatibility compatibility) {
        return switch (compatibility) {
            case EXACT_SIGUNGU -> 0;
            case PARENT_SIDO, EXACT_SIDO -> 1;
            case NATIONWIDE -> 2;
            case MULTIPLE_REGION_MATCH -> 3;
            case UNKNOWN -> 4;
            case NOT_MATCHED -> 5;
        };
    }

    private Set<CandidateSource> sources(CandidateEvidence evidence) {
        Set<CandidateSource> sources = evidence.sourceEvidence().stream()
                .map(CandidateSourceEvidence::source)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CandidateSource.class)));
        if (evidence.titleExactScore() >= 1.0) sources.add(CandidateSource.EXACT_TITLE);
        if (evidence.titleExactScore() >= 0.75) sources.add(CandidateSource.TITLE_PHRASE);
        return sources;
    }

    private double directTitlePhraseScore(Policy policy, PolicySearchCondition condition, PolicySearchPlan plan) {
        String title = normalize(policy.getTitle());
        String query = normalize(plan.originalQuery());
        if (!StringUtils.hasText(title) || !StringUtils.hasText(query)) return 0;
        for (String term : List.of("청년", "정책", "지원", "받을수있는", "알려줘", "추천",
                normalize(condition.province()), normalize(condition.city()), normalize(condition.district()))) {
            if (StringUtils.hasText(term)) query = query.replace(term, "");
        }
        if (query.length() >= 3 && title.contains(query)) return title.equals(query) ? 1.0 : 0.98;
        return 0;
    }

    private double applicationScore(Policy policy, List<String> matched) {
        if (policy.isAlwaysOpen() || policy.getDueDate() == null || !policy.getDueDate().isBefore(LocalDate.now())) {
            matched.add("신청 가능 상태 확인");
            return 1.0;
        }
        return 0;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase()
                .replaceAll("[\\s\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
