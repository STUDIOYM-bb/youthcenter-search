package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PolicyEmploymentAudienceClassifierTest {
    private final PolicyEmploymentAudienceClassifier classifier =
            new PolicyEmploymentAudienceClassifier(mock(PolicySearchProjectionRepository.class));

    @Test
    void classifiesUnemployedOnlyPolicy() {
        var result = classifier.classify(projection("지원 대상: 미취업 청년", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void classifiesEmployedOnlyPolicy() {
        var result = classifier.classify(projection("지원 대상: 중소기업 재직 청년", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.EMPLOYED);
        assertThat(result.exclusive()).isTrue();
    }

    @Test
    void generalYouthIsNotExclusiveEmploymentAudience() {
        var result = classifier.classify(projection("지원 대상: 19세~39세 청년 누구나", ""));

        assertThat(result.allowedStatuses()).contains(UserEmploymentStatus.EMPLOYED, UserEmploymentStatus.UNEMPLOYED);
        assertThat(result.exclusive()).isFalse();
    }

    @Test
    void missingTargetIsUnknown() {
        var result = classifier.classify(projection("", ""));

        assertThat(result.allowedStatuses()).containsExactly(UserEmploymentStatus.UNKNOWN);
        assertThat(result.exclusive()).isFalse();
    }

    private PolicySearchProjection projection(String target, String qualification) {
        Policy policy = new Policy("P-EMP-AUD");
        ReflectionTestUtils.setField(policy, "id", 1);
        policy.updateBasic("테스트 정책", "기관", PolicyCategory.일자리, "", null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update("테스트정책", "테스트 정책", "", "일자리", "", "", target, qualification,
                "", "기관", target + " " + qualification, "test", false);
        return projection;
    }
}
