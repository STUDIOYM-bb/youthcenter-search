package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchIntentBuilderTest {
    private final PolicySearchIntentBuilder builder = new PolicySearchIntentBuilder();

    @Test
    void separatesConditionTermsFromPolicyIntentAndBuildsEmploymentQueries() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27,
                "UNEMPLOYED", null, null, "일자리", Set.of(), Set.of("청년"), Set.of("청년"),
                "수원", "EXACT", "SIGUNGU", Set.of(), true, true, true, false, true, false,
                PolicySearchMode.HYBRID, 20);

        var intent = builder.build("수원 사는 27살 취준생 정책", condition);

        assertThat(intent.conditionTerms()).contains("수원", "27살", "취준생");
        assertThat(intent.intentTerms()).contains("청년", "취업 지원", "구직 지원", "취업 준비");
        assertThat(intent.expandedIntentTerms()).contains("취업", "구직", "취업준비", "구직활동");
        assertThat(intent.semanticQuery()).contains("청년").contains("취업").doesNotContain("27살");
        assertThat(intent.lexicalQuery()).contains("구직").contains("면접");
    }

    @Test
    void expandsFinancialSupportIntentFromCategoryAndSupportTypes() {
        PolicySearchCondition condition = new PolicySearchCondition("경기도", "수원시", null, 27,
                null, null, "earlyCareer", "financialSupport", Set.of("CASH", "SUBSIDY"), Set.of("사회 초년생"),
                Set.of("사회 초년생"), "수원", "EXACT", "SIGUNGU", Set.of(), true, true, false, false,
                true, false, PolicySearchMode.HYBRID, 20);

        var intent = builder.build("수원 사는 27살 사회 초년생이 금융적으로 지원 받을 수 있는 정책", condition);

        assertThat(intent.conditionTerms()).contains("수원", "27살");
        assertThat(intent.intentTerms()).contains("청년", "금융 지원", "생활비 지원");
        assertThat(intent.expandedIntentTerms()).contains("금융", "생활비", "지원금", "사회초년생");
        assertThat(intent.semanticQuery()).contains("사회초년생").contains("금융").doesNotContain("27살");
    }
}
