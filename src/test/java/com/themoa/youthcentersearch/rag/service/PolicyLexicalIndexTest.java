package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyLexicalIndexTest {
    @Test
    void assetBuildingConceptTermsFindSavingsAccountProjectionWithoutPolicyNameSynonym() {
        PolicySearchProjection savings = projection(1, "청년 미래 적립 통장",
                "자산형성 목돈 저축 적립 매칭 지원");
        PolicySearchProjection interview = projection(2, "청년 면접 교육",
                "취업 역량 면접 교육 지원");
        PolicyLexicalIndex index = new PolicyLexicalIndex(List.of(savings, interview), new PolicyKeywordNormalizer());
        PolicySearchCondition condition = new PolicySearchCondition(null, null, null, null, null, null, null, null,
                Set.of("자산형성"), Set.of("자산형성"), Set.of("자산형성", "계좌", "통장", "저축"),
                null, null, null, Set.of(), false, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
        PolicySearchIntent intent = new PolicySearchIntent("사회초년생 자산형성 정책", Set.of(), Set.of("자산형성"),
                Set.of("자산형성", "계좌", "통장", "저축"), "청년 자산형성 저축 계좌 통장 금융 지원 정책", "자산형성 계좌 통장 저축");

        var result = index.search(condition, intent, 10);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).policyId()).isEqualTo(1);
    }

    private PolicySearchProjection projection(int id, String title, String supportText) {
        Policy policy = new Policy("P-LEX-" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", PolicyCategory.금융, supportText, null, null, null, true, true, "OPEN");
        PolicySearchProjection projection = new PolicySearchProjection(policy);
        projection.update(
                title.replaceAll("\\s+", ""),
                title,
                "",
                "금융",
                supportText,
                supportText,
                "청년",
                "",
                "",
                "기관",
                title + " " + supportText,
                "test",
                false
        );
        return projection;
    }
}
