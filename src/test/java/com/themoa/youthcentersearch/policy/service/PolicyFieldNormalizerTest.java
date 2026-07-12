package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyFieldNormalizerTest {
    private final PolicyFieldNormalizer normalizer = new PolicyFieldNormalizer();

    @Test
    void convertsAgeStringAndCategory() {
        Map<String, Object> fields = Map.of(
                "sprtTrgtMinAge", "19",
                "sprtTrgtMaxAge", "34",
                "plcyNm", "경기도 청년 면접수당"
        );

        assertThat(normalizer.integer(fields, "sprtTrgtMinAge")).isEqualTo(19);
        assertThat(normalizer.integer(fields, "sprtTrgtMaxAge")).isEqualTo(34);
        assertThat(normalizer.category(fields)).isEqualTo(PolicyCategory.일자리);
    }

    @Test
    void truncatesLongTextSafely() {
        String value = "첫 문장입니다. 두 번째 문장이 매우 깁니다.";

        assertThat(normalizer.truncate(value, 8)).hasSizeLessThanOrEqualTo(8);
    }
}
