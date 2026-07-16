package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchResult;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRankingServiceTest {
    private final PolicyRankingService rankingService = new PolicyRankingService(
            new RagProperties(), new PolicyDomainClassifier(), new SearchDomainIntentPolicy());

    @Test
    void policyNameSearchKeepsExactTitleFirst() {
        Policy kpass = policy(1, "K-패스", PolicyCategory.복지);
        Policy other = policy(2, "청년 교통비 지원", PolicyCategory.복지);
        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.POLICY_NAME, "K-패스", Set.of(SearchDomain.GENERAL),
                        Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(candidate(other, 0.9, 0.8, 0.0, RecommendationTier.PRIMARY),
                        candidate(kpass, 0.4, 0.2, 1.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates().get(0).candidate().policy().getTitle()).isEqualTo("K-패스");
        assertThat(result.rankedCandidates().get(0).ranking().finalRank()).isEqualTo(1);
    }

    @Test
    void generalSearchOrdersPrimaryBeforeNeedsConfirmationAndThenFinalScore() {
        Policy primaryLow = policy(1, "일반 청년 지원", PolicyCategory.복지);
        Policy needsHigh = policy(2, "확인 필요 지원", PolicyCategory.복지);
        Policy primaryHigh = policy(3, "높은 점수 지원", PolicyCategory.복지);
        PolicyRankingResult result = rankingService.rank(context(plan(SearchQueryType.BROAD_DISCOVERY, "청년 지원",
                        Set.of(SearchDomain.GENERAL), Set.of(SupportIntent.GENERAL))),
                new PolicyEvaluationResult(List.of(candidate(primaryLow, 0.5, 0.5, 0.0, RecommendationTier.PRIMARY),
                        candidate(needsHigh, 0.95, 0.95, 0.0, RecommendationTier.NEEDS_CONFIRMATION),
                        candidate(primaryHigh, 0.9, 0.9, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics()));

        assertThat(result.rankedCandidates()).extracting(item -> item.candidate().policy().getId())
                .containsExactly(3, 1, 2);
    }

    @Test
    void exactSupportIntentDoesNotLowerScoreComparedWithNoSupportIntent() {
        Policy finance = policy(1, "청년 자산형성 지원금", PolicyCategory.금융);
        PolicyEvaluationResult candidates = new PolicyEvaluationResult(
                List.of(candidate(finance, 0.7, 0.7, 0.0, RecommendationTier.PRIMARY)), new PolicySearchFilterMetrics());

        double withIntent = rankingService.rank(context(plan(SearchQueryType.TOPIC_SEARCH, "금융 지원",
                        Set.of(SearchDomain.FINANCE), Set.of(SupportIntent.CASH_ASSISTANCE))), candidates)
                .rankedCandidates().get(0).ranking().finalScore();
        double withoutIntent = rankingService.rank(context(plan(SearchQueryType.TOPIC_SEARCH, "금융 지원",
                        Set.of(SearchDomain.FINANCE), Set.of())), candidates)
                .rankedCandidates().get(0).ranking().finalScore();

        assertThat(withIntent).isGreaterThanOrEqualTo(withoutIntent);
    }

    private PolicySearchExecutionContext context(PolicySearchPlan plan) {
        return new PolicySearchExecutionContext(new PolicySearchRequest(plan.originalQuery(), 10), plan, 1L);
    }

    private PolicySearchPlan plan(SearchQueryType type, String query, Set<SearchDomain> domains, Set<SupportIntent> intents) {
        return new PolicySearchPlan(type, query, query, domains, Set.of(), intents, Set.of(),
                Set.of("청년"), Set.of(), condition(), Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchCondition condition() {
        return new PolicySearchCondition(null, null, null, null, null, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private EvaluatedPolicyCandidate candidate(Policy policy, double semantic, double lexical, double title,
                                               RecommendationTier tier) {
        CandidateEvidence evidence = new CandidateEvidence(policy.getId(),
                title >= 1.0 ? List.of(new CandidateSourceEvidence(CandidateSource.EXACT_TITLE, 1, title, title, "TITLE")) : List.of(),
                semantic, lexical, title);
        PolicyEligibilityEvaluation eligibility = new PolicyEligibilityEvaluation(policy.getId(), true,
                new RegionMatchResult(RegionCompatibility.NATIONWIDE, true, 100, "전국"),
                ConditionMatchResult.unknown("나이 미입력"), ConditionMatchResult.unknown("취업 미입력"),
                ConditionMatchResult.unknown("학생 미입력"), TargetStageMatchResult.unknown("교육 미입력"),
                new EmploymentAudienceMatch(com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus.UNKNOWN, "취업 미입력"),
                tier, List.of(), List.of(), null);
        return new EvaluatedPolicyCandidate(policy, evidence, eligibility);
    }

    private Policy policy(int id, String title, PolicyCategory category) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", category, title, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, title, false));
        return policy;
    }
}
