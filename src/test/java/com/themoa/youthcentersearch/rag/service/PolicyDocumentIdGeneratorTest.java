package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentIdGeneratorTest {
    @Test
    void generatesDeterministicIdFromSourcePolicyId() {
        PolicyDocumentIdGenerator generator = new PolicyDocumentIdGenerator();
        Policy policy = new Policy("P001");
        policy.updateBasic("청년 지원", "경기도", PolicyCategory.일자리, "지원", null, null, null, true, true, "OPEN");

        assertThat(generator.documentId(policy)).isEqualTo(generator.documentId(policy));
    }
}
