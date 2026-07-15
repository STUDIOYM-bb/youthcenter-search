package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PolicyRagSearchServiceConfigurationTest {
    @Test
    void searchFailsClearlyWhenRagIsDisabled() {
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        SearchDomainIntentPolicy domainIntentPolicy = new SearchDomainIntentPolicy();
        UserEducationStageDetector educationStageDetector = new UserEducationStageDetector();
        PolicyRagSearchService service = new PolicyRagSearchService(
                mock(PolicyRepository.class),
                vectorStoreProvider,
                new RagProperties(),
                mock(RegionMatchEvaluator.class),
                mock(PolicyLexicalSearchService.class),
                mock(PolicySearchIntentBuilder.class),
                new PolicyDomainClassifier(),
                new PolicySearchPlanService(mock(CompositePolicySearchConditionParser.class),
                        new PolicyQueryClassifier(new PolicyKeywordNormalizer()), domainIntentPolicy, educationStageDetector),
                domainIntentPolicy,
                mock(PolicyTargetAudienceClassifier.class),
                new PolicyTargetEligibilityFilter(),
                mock(PolicyEmploymentAudienceClassifier.class),
                new UserEmploymentStatusDetector(),
                educationStageDetector,
                null,
                new RegionCoverageResultSelector());

        assertThatThrownBy(() -> service.search(new PolicySearchRequest("청년 지원금", null)))
                .isInstanceOf(YouthCenterApiException.class)
                .hasMessageContaining("RAG 기능이 비활성화되어 있습니다.")
                .hasMessageContaining("RAG_ENABLED=true 설정을 확인하세요.");
    }
}
