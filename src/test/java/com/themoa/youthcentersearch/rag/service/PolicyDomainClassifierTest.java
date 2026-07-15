package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDomainClassifierTest {
    private final PolicyDomainClassifier classifier = new PolicyDomainClassifier();

    @Test
    void educationCategoryWithEmploymentPurposeGetsEmploymentSupportIntent() {
        Policy policy = policy("취업 역량 강화 교육", "채용 연계 직업훈련과 면접 교육",
                PolicyCategory.교육, "신청 자격 제한 없음");

        var result = classifier.classify(policy);

        assertThat(result.primaryDomain()).isEqualTo(SearchDomain.EDUCATION);
        assertThat(result.secondaryDomains()).contains(SearchDomain.EMPLOYMENT);
        assertThat(result.supportIntents()).contains(SupportIntent.EMPLOYMENT_SUPPORT);
    }

    @Test
    void qualificationOnlyEmploymentWordDoesNotCreateEmploymentSupportIntent() {
        Policy policy = policy("청년 생활비 지원", "생활 안정 지원금",
                PolicyCategory.복지, "취업 시 지원 종료");

        var result = classifier.classify(policy);

        assertThat(result.primaryDomain()).isEqualTo(SearchDomain.WELFARE);
        assertThat(result.supportIntents()).doesNotContain(SupportIntent.EMPLOYMENT_SUPPORT);
    }

    private Policy policy(String title, String summary, PolicyCategory category, String conditionSummary) {
        Policy policy = new Policy("P-DOMAIN");
        ReflectionTestUtils.setField(policy, "id", 100);
        policy.updateBasic(title, "기관", category, summary, null, null, null, true, true, "OPEN");
        policy.updateCondition(new PolicyCondition(null, null, null, null, null, conditionSummary, false));
        return policy;
    }
}
