package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

@Service
public class PolicyEmbeddingService {
    private final PolicyRepository policyRepository;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final PolicyDocumentBuilder documentBuilder;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;

    public PolicyEmbeddingService(PolicyRepository policyRepository,
                                  PolicyEmbeddingSyncRepository syncRepository,
                                  PolicyDocumentBuilder documentBuilder,
                                  ObjectProvider<VectorStore> vectorStoreProvider,
                                  RagProperties properties) {
        this.policyRepository = policyRepository;
        this.syncRepository = syncRepository;
        this.documentBuilder = documentBuilder;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
    }

    @Transactional
    public EmbeddingQueueResult queueAll(boolean force) {
        long activeCount = policyRepository.countByActiveTrue();
        int newlyQueued = 0;
        int requeued = 0;
        int unchanged = 0;
        int page = 0;
        while (true) {
            List<Integer> ids = policyRepository.findActivePolicyIds(PageRequest.of(page++, 500));
            if (ids.isEmpty()) {
                break;
            }
            List<Policy> policies = policyRepository.findWithRelationsByIdIn(ids);
            for (Policy policy : policies) {
                PolicyDocumentBuilder.BuiltPolicyDocument built = documentBuilder.build(policy);
                PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policy.getId()).orElse(null);
                if (sync == null) {
                    syncRepository.save(new PolicyEmbeddingSync(policy, built.contentHash()));
                    newlyQueued++;
                } else if (force || !built.contentHash().equals(sync.getContentHash()) || "FAILED".equals(sync.getSyncStatus())) {
                    sync.queue(built.contentHash());
                    requeued++;
                } else {
                    unchanged++;
                }
            }
        }
        return new EmbeddingQueueResult(activeCount, newlyQueued, requeued, unchanged, syncRepository.countBySyncStatus("PENDING"));
    }

    @Transactional
    public EmbeddingProcessResult processPending() {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null || !properties.isEnabled()) {
            return new EmbeddingProcessResult(0, 0, 0, syncRepository.countBySyncStatus("PENDING"),
                    "RAG 또는 Qdrant VectorStore가 비활성화되어 있습니다.");
        }
        int processed = 0;
        int success = 0;
        int failed = 0;
        int batches = 0;
        while (batches++ < properties.getEmbedding().getMaxBatchesPerRun()) {
            List<Integer> ids = syncRepository.findPolicyIdsByStatus("PENDING", PageRequest.of(0, properties.getEmbedding().getBatchSize()));
            if (ids.isEmpty()) {
                break;
            }
            List<Policy> policies = policyRepository.findWithRelationsByIdIn(ids);
            for (Policy policy : policies) {
                PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policy.getId()).orElseThrow();
                processed++;
                try {
                    sync.processing();
                    PolicyDocumentBuilder.BuiltPolicyDocument built = documentBuilder.build(policy);
                    vectorStore.add(List.of(built.document()));
                    sync.synced();
                    success++;
                } catch (RuntimeException ex) {
                    sync.failed(ex.getMessage());
                    failed++;
                }
            }
            sleepQuietly(properties.getEmbedding().getRequestDelay().toMillis());
        }
        return new EmbeddingProcessResult(processed, success, failed, syncRepository.countBySyncStatus("PENDING"), "COMPLETED");
    }

    @Transactional
    public int retryFailed() {
        List<Integer> ids = syncRepository.findPolicyIdsByStatus("FAILED", PageRequest.of(0, properties.getEmbedding().getBatchSize()));
        int count = 0;
        for (Integer id : ids) {
            Policy policy = policyRepository.findWithRelationsByIdIn(List.of(id)).stream().findFirst().orElse(null);
            if (policy == null) {
                continue;
            }
            PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policy.getId()).orElseThrow();
            sync.queue(documentBuilder.build(policy).contentHash());
            count++;
        }
        return count;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
