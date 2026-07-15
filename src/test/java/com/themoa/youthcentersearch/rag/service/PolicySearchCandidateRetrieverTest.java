package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicySearchCandidateRetrieverTest {
    @Test
    void broadDiscoveryUsesLexicalAndBroadPoolWithoutVectorStore() {
        PolicyRepository repository = mock(PolicyRepository.class);
        Policy policy = new Policy("P1");
        when(repository.findActivePolicyIds(any())).thenReturn(List.of(1));
        when(repository.findWithRelationsByIdIn(any())).thenReturn(List.of(policy));

        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        PolicyLexicalSearchService lexicalSearchService = mock(PolicyLexicalSearchService.class);
        when(lexicalSearchService.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                List.of(1),
                Map.of(1, 0.8),
                Map.of(1, 1.0),
                Map.of(1, Set.of(CandidateSource.MYSQL_TITLE)),
                Map.of(CandidateSource.MYSQL_TITLE, 1)));

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicySearchCandidateRetriever retriever = new PolicySearchCandidateRetriever(
                repository, vectorStoreProvider, properties, lexicalSearchService, null);

        PolicyCandidateCollection collection = retriever.retrieve(plan(), intent(), new ResolvedUserRegion(null, null, null), 0, 10, 10);

        assertThat(collection.policies()).hasSize(1);
        assertThat(collection.metrics().lexicalCandidateCount()).isEqualTo(1);
        assertThat(collection.candidateSources().get(1)).contains(CandidateSource.LEXICAL_INDEX, CandidateSource.EXACT_TITLE);
        assertThat(collection.evidenceByPolicyId().get(1).sourceEvidence())
                .extracting(CandidateSourceEvidence::source)
                .contains(CandidateSource.MYSQL_TITLE, CandidateSource.LEXICAL_INDEX, CandidateSource.EXACT_TITLE);
        assertThat(collection.fallbackReason()).isEqualTo("VECTOR_SEARCH_DISABLED");
    }

    private PolicySearchPlan plan() {
        PolicySearchCondition condition = new PolicySearchCondition(null, null, null, null, null, null, null, "general",
                Set.of(), Set.of("청년"), Set.of("청년"), null, null, null, Set.of(),
                false, false, false, false, false, false, PolicySearchMode.HYBRID, 10);
        return new PolicySearchPlan(SearchQueryType.BROAD_DISCOVERY, "청년 정책", "청년 정책",
                Set.of(SearchDomain.GENERAL), Set.of(), Set.of(SupportIntent.GENERAL), Set.of(),
                Set.of("청년"), Set.of(), condition, Set.of(EducationStage.UNKNOWN), false, false, "TEST");
    }

    private PolicySearchIntent intent() {
        return new PolicySearchIntent("청년 정책", Set.of(), Set.of("청년"), Set.of("청년"),
                "청년 정책", "청년");
    }
}
