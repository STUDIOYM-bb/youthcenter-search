package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchResult;
import com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchResultAssemblerTest {
    @Test
    void assemblesEligibilityRankingAndCandidateSourcesWithoutChangingResultFields() {
        PolicySearchResultAssembler assembler = new PolicySearchResultAssembler(new PolicyDomainClassifier());
        Policy policy = policy();
        CandidateEvidence evidence = new CandidateEvidence(1,
                List.of(new CandidateSourceEvidence(CandidateSource.MYSQL_TITLE, 1, 1.0, 1.0, "BM25_FIELD")),
                0.72, 0.81, 1.0);
        PolicyEligibilityEvaluation eligibility = new PolicyEligibilityEvaluation(1, true,
                new RegionMatchResult(RegionCompatibility.NATIONWIDE, true, 100, "전국 정책"),
                ConditionMatchResult.match("나이 일치"), ConditionMatchResult.unknown("취업 확인"),
                ConditionMatchResult.unknown("학생 확인"), TargetStageMatchResult.unknown("교육 확인"),
                new EmploymentAudienceMatch(ConditionMatchStatus.UNKNOWN, "취업 확인"),
                RecommendationTier.PRIMARY, List.of("전국 정책"), List.of("취업 확인 필요"), null);
        PolicyRankingEvaluation ranking = new PolicyRankingEvaluation(0.9, 0.72, 0.81, 1.0,
                0.0, 0.0, 100.0, 88.4, RecommendationTier.PRIMARY, 1, List.of("정책명 정확도 높음"));

        PolicySearchResultItem item = assembler.assemble(new RankedPolicyCandidate(
                new EvaluatedPolicyCandidate(policy, evidence, eligibility), ranking), Map.of(), Map.of());

        assertThat(item.policyId()).isEqualTo(1);
        assertThat(item.semanticScore()).isEqualTo(0.72);
        assertThat(item.finalScore()).isEqualTo(88.4);
        assertThat(item.matchedReasons()).contains("전국 정책", "정책명 정확도 높음");
        assertThat(item.needCheckReasons()).contains("취업 확인 필요");
        assertThat(item.candidateSources()).contains(CandidateSource.MYSQL_TITLE.name(), CandidateSource.EXACT_TITLE.name());
        assertThat(item.officialUrl()).isNull();
    }

    private Policy policy() {
        Policy policy = new Policy("P1");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic("K-패스", "기관", PolicyCategory.복지, "교통비 지원", null,
                null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, null, null, null, "청년 교통비 지원", false));
        return policy;
    }
}
