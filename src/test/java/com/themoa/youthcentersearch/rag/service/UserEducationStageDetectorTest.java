package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.EducationStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEducationStageDetectorTest {
    private final UserEducationStageDetector detector = new UserEducationStageDetector();

    @Test
    void detectsUniversityWhenUserSaysCollegeStudentJobSeeker() {
        var result = detector.detect("대학생 취준생이 받을 수 있는 혜택");

        assertThat(result.explicit()).isTrue();
        assertThat(result.stages()).contains(EducationStage.UNIVERSITY);
    }

    @Test
    void doesNotInferEducationStageFromAgeAndJobSeeker() {
        var result = detector.detect("22살 취준생");

        assertThat(result.explicit()).isFalse();
        assertThat(result.stages()).containsExactly(EducationStage.UNKNOWN);
    }

    @Test
    void detectsHighSchoolStudent() {
        var result = detector.detect("고등학생 취업 지원");

        assertThat(result.explicit()).isTrue();
        assertThat(result.stages()).contains(EducationStage.HIGH_SCHOOL);
    }

    @Test
    void universityGraduationIsNotCurrentUniversityStudent() {
        var result = detector.detect("대학교 졸업 후 취준 중");

        assertThat(result.explicit()).isFalse();
        assertThat(result.stages()).containsExactly(EducationStage.UNKNOWN);
    }
}
