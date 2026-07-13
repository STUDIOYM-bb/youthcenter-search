package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.common.config.LocalSecretConfigurationStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.function.Consumer;

@Service
public class PolicyEmbeddingService {
    private final PolicyRepository policyRepository;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final PolicyDocumentBuilder documentBuilder;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;
    private final LocalSecretConfigurationStatus configurationStatus;
    private final TransactionTemplate transactionTemplate;

    public PolicyEmbeddingService(PolicyRepository policyRepository,
                                  PolicyEmbeddingSyncRepository syncRepository,
                                  PolicyDocumentBuilder documentBuilder,
                                  ObjectProvider<VectorStore> vectorStoreProvider,
                                  RagProperties properties,
                                  LocalSecretConfigurationStatus configurationStatus,
                                  TransactionTemplate transactionTemplate) {
        this.policyRepository = policyRepository;
        this.syncRepository = syncRepository;
        this.documentBuilder = documentBuilder;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
        this.configurationStatus = configurationStatus;
        this.transactionTemplate = transactionTemplate;
    }

    public EmbeddingQueueResult queueAll(boolean force) {
        return transactionTemplate.execute(status -> {
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
        });
    }

    public EmbeddingProcessResult processPending() {
        return processPending(null);
    }

    public EmbeddingProcessResult processPending(Consumer<EmbeddingProgress> progressConsumer) {
        if (!configurationStatus.embeddingModelAvailable()) {
            return new EmbeddingProcessResult(0, 0, 0, syncRepository.countBySyncStatus("PENDING"),
                    """
                            OpenAI Embedding Model이 비활성화되어 있습니다.
                            OPENAI_API_KEY와 SPRING_AI_MODEL_EMBEDDING=openai 설정을 확인하세요.""");
        }
        if (!properties.isEnabled()) {
            return new EmbeddingProcessResult(0, 0, 0, syncRepository.countBySyncStatus("PENDING"),
                    """
                            RAG 기능이 비활성화되어 있습니다.
                            RAG_ENABLED=true 설정을 확인하세요.""");
        }
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
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
            for (Integer policyId : ids) {
                processed++;
                PolicyDocumentBuilder.BuiltPolicyDocument built = null;
                try {
                    built = markProcessingAndBuild(policyId);
                    vectorStore.add(List.of(built.document()));
                    markSynced(policyId);
                    success++;
                } catch (RuntimeException ex) {
                    markFailed(policyId, ex);
                    failed++;
                }
                notifyProgress(progressConsumer, processed, success, failed);
            }
            sleepQuietly(properties.getEmbedding().getRequestDelay().toMillis());
        }
        return new EmbeddingProcessResult(processed, success, failed, syncRepository.countBySyncStatus("PENDING"), "COMPLETED");
    }

    public int retryFailed() {
        return transactionTemplate.execute(status -> {
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
        });
    }

    private PolicyDocumentBuilder.BuiltPolicyDocument markProcessingAndBuild(Integer policyId) {
        return transactionTemplate.execute(status -> {
            Policy policy = policyRepository.findWithRelationsByIdIn(List.of(policyId)).stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다: " + policyId));
            PolicyEmbeddingSync sync = syncRepository.findByPolicyId(policy.getId()).orElseThrow();
            sync.processing();
            return documentBuilder.build(policy);
        });
    }

    private void markSynced(Integer policyId) {
        transactionTemplate.executeWithoutResult(status -> syncRepository.findByPolicyId(policyId).orElseThrow().synced());
    }

    private void markFailed(Integer policyId, RuntimeException ex) {
        transactionTemplate.executeWithoutResult(status -> syncRepository.findByPolicyId(policyId).orElseThrow().failed(ex.getMessage()));
    }

    private void notifyProgress(Consumer<EmbeddingProgress> progressConsumer, int processed, int success, int failed) {
        if (progressConsumer != null) {
            progressConsumer.accept(new EmbeddingProgress(processed, success, failed, syncRepository.countBySyncStatus("PENDING")));
        }
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

    public record EmbeddingProgress(int processedCount, int successCount, int failedCount, long pendingCountAfter) {
    }
}
