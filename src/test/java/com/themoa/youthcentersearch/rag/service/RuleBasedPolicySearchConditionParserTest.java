package com.themoa.youthcentersearch.rag.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedPolicySearchConditionParserTest {
    @Test
    void extractsRegionAgeEmploymentAndKeywords() {
        RuleBasedPolicySearchConditionParser parser = new RuleBasedPolicySearchConditionParser();

        var condition = parser.parseCondition("수원 사는 27살 무직 청년 지원금", 20);

        assertThat(condition.province()).isEqualTo("경기도");
        assertThat(condition.city()).isEqualTo("수원시");
        assertThat(condition.age()).isEqualTo(27);
        assertThat(condition.employmentStatus()).isEqualTo("UNEMPLOYED");
        assertThat(condition.keywords()).contains("청년", "지원금", "취업");
    }
}
