package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyQueryClassifierTest {
    private final PolicyQueryClassifier classifier = new PolicyQueryClassifier(new PolicyKeywordNormalizer());

    @Test
    void detectsPolicyNameVariantsWithoutHardCodingSpecificPolicy() {
        PolicySearchCondition condition = condition(Set.of("K패스"), Set.of("K패스"), false);

        assertThat(classifier.classify("K-패스", condition)).isEqualTo(SearchQueryType.POLICY_NAME);
        assertThat(classifier.classify("k 패스", condition)).isEqualTo(SearchQueryType.POLICY_NAME);
    }

    @Test
    void broadDiscoveryDoesNotBecomeTopicSearchOnlyBecauseYouthPolicyWordsExist() {
        PolicySearchCondition condition = condition(Set.of("청년", "정책"), Set.of("청년", "정책"), true);

        assertThat(classifier.classify("수원에 사는 청년 정책", condition)).isEqualTo(SearchQueryType.BROAD_DISCOVERY);
    }

    private PolicySearchCondition condition(Set<String> keywords, Set<String> expanded, boolean regionExplicit) {
        return new PolicySearchCondition(regionExplicit ? "경기도" : null, regionExplicit ? "수원시" : null, null,
                null, null, null, null, null, Set.of(), keywords, expanded,
                regionExplicit ? "수원" : null, regionExplicit ? "EXACT" : null, regionExplicit ? "SIGUNGU" : null,
                Set.of(), regionExplicit, false, false, false, false, false, PolicySearchMode.KEYWORD, 20);
    }
}
