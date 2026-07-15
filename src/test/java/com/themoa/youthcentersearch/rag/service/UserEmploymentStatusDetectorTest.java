package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEmploymentStatusDetectorTest {
    private final UserEmploymentStatusDetector detector = new UserEmploymentStatusDetector();

    @Test
    void detectsEmployedNarrativeExpressions() {
        assertThat(detector.detect("직장에 다니고 있다").status()).isEqualTo(UserEmploymentStatus.EMPLOYED);
        assertThat(detector.detect("서울 직장에 다니고 있다").explicit()).isTrue();
        assertThat(detector.detect("회사에서 일하고 있다").status()).isEqualTo(UserEmploymentStatus.EMPLOYED);
        assertThat(detector.detect("현재 근무 중이다").status()).isEqualTo(UserEmploymentStatus.EMPLOYED);
        assertThat(detector.detect("재직 중이고 이직 준비를 한다").status()).isEqualTo(UserEmploymentStatus.EMPLOYED);
    }

    @Test
    void detectsUnemployedSearchOrPreparationExpressions() {
        assertThat(detector.detect("직장을 구하고 있다").status()).isEqualTo(UserEmploymentStatus.UNEMPLOYED);
        assertThat(detector.detect("취업 준비 중이다").status()).isEqualTo(UserEmploymentStatus.UNEMPLOYED);
    }

    @Test
    void doesNotTreatWishAsCurrentEmployment() {
        assertThat(detector.detect("직장에 다니고 싶다").status()).isEqualTo(UserEmploymentStatus.UNKNOWN);
        assertThat(detector.detect("청년 정책을 찾고 있다").explicit()).isFalse();
    }
}
