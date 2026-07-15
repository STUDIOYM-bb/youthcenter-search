package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SearchDomainIntentPolicyTest {
    private final SearchDomainIntentPolicy policy = new SearchDomainIntentPolicy();

    @Test
    void employmentExclusionRemovesEducationPolicyWithEmploymentSupportIntent() {
        PolicySearchPlan plan = plan(Set.of(SearchDomain.EMPLOYMENT), Set.of(SupportIntent.EMPLOYMENT_SUPPORT));
        PolicyDomainClassification classification = new PolicyDomainClassification(
                SearchDomain.EDUCATION,
                Set.of(),
                Set.of(SupportIntent.EDUCATION, SupportIntent.EMPLOYMENT_SUPPORT),
                0.9,
                List.of("취업 목적 교육")
        );

        assertThat(policy.isExcluded(classification, plan)).isTrue();
    }

    @Test
    void financeExclusionDoesNotRemoveCashAssistanceByItself() {
        PolicySearchPlan plan = plan(Set.of(SearchDomain.FINANCE), Set.of());
        PolicyDomainClassification classification = new PolicyDomainClassification(
                SearchDomain.WELFARE,
                Set.of(),
                Set.of(SupportIntent.CASH_ASSISTANCE),
                0.8,
                List.of("생활 지원금")
        );

        assertThat(policy.isExcluded(classification, plan)).isFalse();
    }

    private PolicySearchPlan plan(Set<SearchDomain> excludedDomains, Set<SupportIntent> excludedSupportIntents) {
        return new PolicySearchPlan(
                SearchQueryType.ELIGIBILITY_SEARCH,
                "취업 말고 대학생 정책",
                "대학생이 신청 가능한 청년 지원 정책",
                Set.of(),
                excludedDomains,
                Set.of(),
                excludedSupportIntents,
                Set.of("대학생"),
                Set.of("취업"),
                new PolicySearchCondition(null, null, null, null, null, null, null, null,
                        Set.of(), Set.of(), Set.of(), null, null, null, Set.of(),
                        false, false, false, false, false, false, PolicySearchMode.HYBRID, 10),
                Set.of(com.themoa.youthcentersearch.rag.dto.EducationStage.UNKNOWN),
                false,
                true,
                "TEST"
        );
    }
}
