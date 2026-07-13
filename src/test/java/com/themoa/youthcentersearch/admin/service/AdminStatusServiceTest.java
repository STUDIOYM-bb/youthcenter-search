package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.common.config.LocalSecretConfigurationStatus;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStatusServiceTest {
    @Test
    void statusDoesNotExposeSecretValues() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        RagProperties ragProperties = new RagProperties();
        ragProperties.setEnabled(true);
        LocalSecretConfigurationStatus configurationStatus = new LocalSecretConfigurationStatus(
                "real-youth-key", "real-openai-key", "openai", "openai", true);
        when(policyRepository.count()).thenReturn(1L);
        when(policyRepository.countByActiveTrue()).thenReturn(1L);
        when(syncRepository.countBySyncStatus("PENDING")).thenReturn(0L);
        when(syncRepository.countBySyncStatus("PROCESSING")).thenReturn(0L);
        when(syncRepository.countBySyncStatus("SYNCED")).thenReturn(1L);
        when(syncRepository.countBySyncStatus("FAILED")).thenReturn(0L);

        var response = new AdminStatusService(policyRepository, syncRepository, vectorStoreProvider,
                ragProperties, configurationStatus).status();

        assertThat(response.youthCenterApiKeyConfigured()).isTrue();
        assertThat(response.openAiApiKeyConfigured()).isTrue();
        assertThat(response.chatModelAvailable()).isTrue();
        assertThat(response.embeddingModelAvailable()).isTrue();
        assertThat(response.toString()).doesNotContain("real-youth-key", "real-openai-key");
    }

    @Test
    void statusRespondsWhenDatabaseIsUnavailable() {
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        PolicyEmbeddingSyncRepository syncRepository = mock(PolicyEmbeddingSyncRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        RagProperties ragProperties = new RagProperties();
        LocalSecretConfigurationStatus configurationStatus = new LocalSecretConfigurationStatus("", "", "none", "none", false);
        when(policyRepository.count()).thenThrow(new IllegalStateException("db down"));

        var response = new AdminStatusService(policyRepository, syncRepository, vectorStoreProvider,
                ragProperties, configurationStatus).status();

        assertThat(response.mysqlAvailable()).isFalse();
        assertThat(response.youthCenterApiKeyConfigured()).isFalse();
        assertThat(response.chatModelAvailable()).isFalse();
        assertThat(response.embeddingModelAvailable()).isFalse();
        assertThat(response.ragEnabled()).isFalse();
    }
}
