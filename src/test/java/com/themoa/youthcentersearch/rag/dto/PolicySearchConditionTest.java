package com.themoa.youthcentersearch.rag.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchConditionTest {
    @Test
    void normalizesOpenAiEmptyDefaults() {
        var condition = new PolicySearchCondition(" 제주도 ", "", " ", 0,
                "", false, "", "청년", Set.of(), Set.of("월세"), 10);

        assertThat(condition.province()).isEqualTo("제주도");
        assertThat(condition.city()).isNull();
        assertThat(condition.district()).isNull();
        assertThat(condition.age()).isNull();
        assertThat(condition.employmentStatus()).isNull();
        assertThat(condition.studentStatus()).isNull();
    }
}
