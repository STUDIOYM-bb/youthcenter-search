package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.policy.region.RegionMatchResult;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatusResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyEligibilityEvaluatorTest {
    private final RegionMatchEvaluator regionMatchEvaluator = mock(RegionMatchEvaluator.class);
    private final ResolvedUserRegion userRegion = mock(ResolvedUserRegion.class);
    private final PolicyEligibilityEvaluator evaluator = new PolicyEligibilityEvaluator(
            new RagProperties(), regionMatchEvaluator, new PolicyTargetEligibilityFilter());

    @Test
    void removesExplicitOtherRegionAndKeepsParentSidoAndNationwide() {
        Policy seoul = policy(1, "서울 정책", null);
        Policy gyeonggi = policy(2, "경기 정책", null);
        Policy nationwide = policy(3, "전국 정책", null);
        when(regionMatchEvaluator.evaluate(eq(seoul), any())).thenReturn(region(RegionCompatibility.NOT_MATCHED, false));
        when(regionMatchEvaluator.evaluate(eq(gyeonggi), any())).thenReturn(region(RegionCompatibility.PARENT_SIDO, true));
        when(regionMatchEvaluator.evaluate(eq(nationwide), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicyEvaluationResult result = evaluator.evaluate(context(plan(condition("경기도", "수원시", 27, null, true, false),
                        Set.of(EducationStage.UNKNOWN), false)),
                collection(seoul, gyeonggi, nationwide), userRegion, Map.of(), Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(result.passedCandidates()).extracting(item -> item.policy().getId()).containsExactly(2, 3);
        assertThat(result.metrics().regionFiltered).isEqualTo(1);
    }

    @Test
    void removesEmploymentAndEducationStageHardMismatchButKeepsUnknownAsConfirmation() {
        Policy unemployedOnly = policy(1, "미취업 전용", "UNEMPLOYED");
        Policy highSchoolOnly = policy(2, "고교생 전용", null);
        Policy universityOnly = policy(3, "대학생 전용", null);
        when(regionMatchEvaluator.evaluate(any(), any())).thenReturn(region(RegionCompatibility.NATIONWIDE, true));

        PolicySearchPlan universityPlan = plan(condition(null, null, null, "EMPLOYED", false, true),
                Set.of(EducationStage.UNIVERSITY), true);
        PolicyEvaluationResult mismatch = evaluator.evaluate(context(universityPlan),
                collection(unemployedOnly, highSchoolOnly), userRegion,
                Map.of(2, audience(EducationStage.HIGH_SCHOOL, true)),
                Map.of(1, new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNEMPLOYED), true, 1.0, List.of("미취업자"))),
                new UserEmploymentStatusResult(UserEmploymentStatus.EMPLOYED, true, 1.0, List.of("직장")));

        assertThat(mismatch.passedCandidates()).isEmpty();
        assertThat(mismatch.metrics().employmentFiltered).isEqualTo(1);
        assertThat(mismatch.metrics().targetFiltered).isEqualTo(1);

        PolicyEvaluationResult unknownEducation = evaluator.evaluate(context(plan(condition(null, null, null, null, false, false),
                        Set.of(EducationStage.UNKNOWN), false)),
                collection(universityOnly), userRegion, Map.of(3, audience(EducationStage.UNIVERSITY, true)),
                Map.of(), UserEmploymentStatusResult.unknown());

        assertThat(unknownEducation.passedCandidates()).hasSize(1);
        assertThat(unknownEducation.passedCandidates().get(0).eligibility().preliminaryTier().name())
                .isEqualTo("NEEDS_CONFIRMATION");
        assertThat(unknownEducation.passedCandidates().get(0).eligibility().confirmationReasons())
                .anyMatch(reason -> reason.contains("대상 교육 단계 확인 필요"));
    }

    private PolicySearchExecutionContext context(PolicySearchPlan plan) {
        return new PolicySearchExecutionContext(new PolicySearchRequest("query", 10), plan, 1L);
    }

    private PolicyCandidateCollection collection(Policy... policies) {
        return new PolicyCandidateCollection(List.of(policies),
                java.util.Arrays.stream(policies).collect(java.util.stream.Collectors.toMap(Policy::getId,
                        policy -> new CandidateEvidence(policy.getId(), List.of(), 0.8, 0.5, 0.0))),
                Map.of(), Map.of(), Map.of(), Map.of(), new CandidateCollectionMetrics(0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, policies.length, 0, 0, 0, 0, 0, 0, 0), false, false, null);
    }

    private PolicySearchPlan plan(PolicySearchCondition condition, Set<EducationStage> stages, boolean educationExplicit) {
        return new PolicySearchPlan(SearchQueryType.ELIGIBILITY_SEARCH, "query", "query",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of(), Set.of(), condition, stages, educationExplicit, false, "TEST");
    }

    private PolicySearchCondition condition(String province, String city, Integer age, String employment,
                                            boolean regionExplicit, boolean employmentExplicit) {
        return new PolicySearchCondition(province, city, null, age, employment, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), city, null, null, Set.of(),
                regionExplicit, age != null, employmentExplicit, false, false, false, PolicySearchMode.HYBRID, 10);
    }

    private Policy policy(int id, String title, String employment) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.복지, title, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(18, 39, employment, null, null, title, false));
        return policy;
    }

    private RegionMatchResult region(RegionCompatibility compatibility, boolean eligible) {
        return new RegionMatchResult(compatibility, eligible, eligible ? 100 : 0, compatibility.name());
    }

    private PolicyTargetAudienceClassification audience(EducationStage stage, boolean exclusive) {
        return new PolicyTargetAudienceClassification(Set.of(stage), Set.of(), exclusive, 1.0, List.of(stage.name()));
    }
}
