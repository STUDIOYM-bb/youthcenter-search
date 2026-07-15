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

class PolicyPreferenceFilterTest {
    @Test
    void excludedSupportIntentIsHardFilterEvenWhenPrimaryDomainIsDifferent() {
        SearchDomainIntentPolicy policy = new SearchDomainIntentPolicy();
        PolicySearchPlan plan = new PolicySearchPlan(
                SearchQueryType.ELIGIBILITY_SEARCH,
                "취업 관련 정책보다는 대학생 정책",
                "대학생이 신청 가능한 청년 지원 정책",
                Set.of(SearchDomain.EDUCATION),
                Set.of(SearchDomain.EMPLOYMENT),
                Set.of(SupportIntent.EDUCATION),
                Set.of(SupportIntent.EMPLOYMENT_SUPPORT),
                Set.of("대학생"),
                Set.of("취업"),
                new PolicySearchCondition("경기도", "수원시", null, 22, "UNEMPLOYED", true, null, null,
                        Set.of(), Set.of("대학생"), Set.of("대학생"),
                        "수원", "EXACT", "SIGUNGU", Set.of(),
                        true, true, true, true, false, false, PolicySearchMode.HYBRID, 20),
                Set.of(com.themoa.youthcentersearch.rag.dto.EducationStage.UNIVERSITY),
                true,
                true,
                "TEST"
        );
        PolicyDomainClassification educationJobTraining = new PolicyDomainClassification(
                SearchDomain.EDUCATION,
                Set.of(),
                Set.of(SupportIntent.EDUCATION, SupportIntent.EMPLOYMENT_SUPPORT),
                0.9,
                List.of("취업 목적 교육")
        );

        assertThat(policy.isExcluded(educationJobTraining, plan)).isTrue();
    }
}
