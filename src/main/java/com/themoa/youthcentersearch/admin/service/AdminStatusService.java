package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.admin.dto.AdminStatusResponse;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminStatusService {
    private final PolicyRepository policyRepository;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final String openAiKey;

    public AdminStatusService(PolicyRepository policyRepository,
                              PolicyEmbeddingSyncRepository syncRepository,
                              ObjectProvider<VectorStore> vectorStoreProvider,
                              RagProperties ragProperties,
                              @Value("${spring.ai.openai.api-key:}") String openAiKey) {
        this.policyRepository = policyRepository;
        this.syncRepository = syncRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.ragProperties = ragProperties;
        this.openAiKey = openAiKey;
    }

    public AdminStatusResponse status() {
        return new AdminStatusResponse(
                "UP",
                true,
                vectorStoreProvider.getIfAvailable() != null,
                StringUtils.hasText(openAiKey),
                StringUtils.hasText(openAiKey),
                ragProperties.isEnabled(),
                ragProperties.getCollectionName(),
                policyRepository.count(),
                policyRepository.countByActiveTrue(),
                syncRepository.countBySyncStatus("PENDING"),
                syncRepository.countBySyncStatus("PROCESSING"),
                syncRepository.countBySyncStatus("SYNCED"),
                syncRepository.countBySyncStatus("FAILED"),
                null
        );
    }
}
