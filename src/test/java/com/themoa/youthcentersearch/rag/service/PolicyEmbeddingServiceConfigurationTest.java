package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.common.config.LocalSecretConfigurationStatus;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyEmbeddingServiceConfigurationTest {
    @Test
    void processPendingReportsEmbeddingDisabled() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        PolicyDocumentBuilder documentBuilder = mock(PolicyDocumentBuilder.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(syncRepository.countBySyncStatus("PENDING")).thenReturn(3L);

        PolicyEmbeddingService service = new PolicyEmbeddingService(policyRepository, syncRepository, documentBuilder,
                vectorStoreProvider, new RagProperties(),
                new LocalSecretConfigurationStatus("", "", "none", "none", false),
                mock(TransactionTemplate.class));

        EmbeddingProcessResult result = service.processPending();

        assertThat(result.pendingCountAfter()).isEqualTo(3L);
        assertThat(result.message()).contains("OpenAI Embedding Model이 비활성화되어 있습니다.");
    }

    @Test
    void processPendingReportsRagDisabledWhenEmbeddingIsAvailable() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        PolicyDocumentBuilder documentBuilder = mock(PolicyDocumentBuilder.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(syncRepository.countBySyncStatus("PENDING")).thenReturn(2L);

        PolicyEmbeddingService service = new PolicyEmbeddingService(policyRepository, syncRepository, documentBuilder,
                vectorStoreProvider, new RagProperties(),
                new LocalSecretConfigurationStatus("", "openai-test-key", "openai", "openai", false),
                mock(TransactionTemplate.class));

        EmbeddingProcessResult result = service.processPending();

        assertThat(result.pendingCountAfter()).isEqualTo(2L);
        assertThat(result.message()).contains("RAG 기능이 비활성화되어 있습니다.");
    }
}
