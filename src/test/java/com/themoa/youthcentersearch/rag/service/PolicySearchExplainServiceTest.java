package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchResult;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchExplainServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void usesActualSourceRankAndDoesNotRepeatSemanticScoreForMissingVectorSources() {
        Policy policy = policy();
        CandidateEvidence evidence = new CandidateEvidence(1, List.of(
                new CandidateSourceEvidence(CandidateSource.VECTOR_ORIGINAL_QUERY, 4, 0.812, 0.7, "QDRANT_ORIGINAL"),
                new CandidateSourceEvidence(CandidateSource.MYSQL_TITLE, 1, 1.0, 1.0, "BM25_FIELD")
        ), 0.284, 0.9, 1.0);
        PolicyRankingEvaluation ranking = new PolicyRankingEvaluation(1.0, 0.284, 0.9, 1.0,
                0.0, 0.0, 100.0, 91.2, RecommendationTier.PRIMARY, 7, List.of());

        Map<String, Object> body = new PolicySearchExplainService(new PolicyDomainClassifier()).explain(
                plan(), policy, evidence, eligibility(), ranking, diagnostics(), true);
        Map<String, Object> vectorScores = (Map<String, Object>) body.get("vectorScores");
        Map<String, Object> original = (Map<String, Object>) vectorScores.get("originalQuery");
        Map<String, Object> intent = (Map<String, Object>) vectorScores.get("intentQuery");

        assertThat(original.get("used")).isEqualTo(true);
        assertThat(original.get("rank")).isEqualTo(4);
        assertThat(original.get("score")).isEqualTo(0.812);
        assertThat(intent.get("used")).isEqualTo(false);
        assertThat(intent.get("rank")).isNull();
        assertThat(intent.get("score")).isNull();
        assertThat(body.get("finalRank")).isEqualTo(7);
        assertThat(body.get("finalScore")).isEqualTo(91.2);
    }

    private PolicyEligibilityEvaluation eligibility() {
        return new PolicyEligibilityEvaluation(1, true,
                new RegionMatchResult(RegionCompatibility.NATIONWIDE, true, 100, "전국"),
                ConditionMatchResult.unknown("나이"), ConditionMatchResult.unknown("취업"),
                ConditionMatchResult.unknown("학생"), TargetStageMatchResult.unknown("교육"),
                new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "취업"),
                RecommendationTier.PRIMARY, List.of(), List.of(), null);
    }

    private PolicySearchPlan plan() {
        return new PolicySearchPlan(SearchQueryType.POLICY_NAME, "K-패스", "K-패스",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of(), Set.of(), new PolicySearchCondition(null, null, null, null, null, null, null, "general",
                Set.of(), Set.of(), Set.of(), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.HYBRID, 10),
                Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchDiagnostics diagnostics() {
        return PolicySearchDiagnosticsBuilder.builder().vectorCandidateCount(1).finalResultCount(1).build();
    }

    private Policy policy() {
        Policy policy = new Policy("P1");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic("K-패스", "기관", PolicyCategory.복지, "교통비 지원", null,
                null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, "교통비 지원", false));
        return policy;
    }
}
