package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.admin.dto.AdminJobStatus;
import com.themoa.youthcentersearch.policy.service.PolicyCollectionResult;
import com.themoa.youthcentersearch.policy.service.PolicyRegionRebuildResult;
import com.themoa.youthcentersearch.policy.service.PolicyRegionRebuildService;
import com.themoa.youthcentersearch.policy.service.YouthCenterPolicyCollectionService;
import com.themoa.youthcentersearch.rag.service.EmbeddingProcessResult;
import com.themoa.youthcentersearch.rag.service.EmbeddingQueueResult;
import com.themoa.youthcentersearch.rag.service.PolicyEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminJobService {
    private final YouthCenterPolicyCollectionService collectionService;
    private final PolicyEmbeddingService embeddingService;
    private final PolicyRegionRebuildService regionRebuildService;
    private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();

    public AdminJobService(YouthCenterPolicyCollectionService collectionService, PolicyEmbeddingService embeddingService,
                           PolicyRegionRebuildService regionRebuildService) {
        this.collectionService = collectionService;
        this.embeddingService = embeddingService;
        this.regionRebuildService = regionRebuildService;
    }

    public AdminJobStatus start(String type) {
        if (jobs.values().stream().anyMatch(job -> job.type.equals(type) && "RUNNING".equals(job.status))) {
            throw new IllegalStateException("JOB_ALREADY_RUNNING");
        }
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), type);
        jobs.put(job.id, job);
        CompletableFuture.runAsync(() -> run(job));
        return job.snapshot();
    }

    private void run(MutableJob job) {
        try {
            switch (job.type) {
                case "POLICY_COLLECTION" -> {
                    PolicyCollectionResult result = collectionService.collectAll();
                    job.processed = result.receivedCount();
                    job.success = result.insertedCount() + result.updatedCount();
                    job.failed = result.failedCount();
                    job.message = result.status();
                }
                case "EMBEDDING_QUEUE" -> {
                    EmbeddingQueueResult result = embeddingService.queueAll(false);
                    job.total = result.activePolicyCount();
                    job.success = result.newlyQueuedCount() + result.requeuedCount();
                    job.remaining = result.pendingCountAfter();
                    job.message = "QUEUE_COMPLETED";
                }
                case "EMBEDDING_PROCESS" -> {
                    EmbeddingProcessResult result = embeddingService.processPending(progress -> {
                        job.processed = progress.processedCount();
                        job.success = progress.successCount();
                        job.failed = progress.failedCount();
                        job.remaining = progress.pendingCountAfter();
                        job.message = "EMBEDDING_PROCESSING";
                    });
                    job.processed = result.processedCount();
                    job.success = result.successCount();
                    job.failed = result.failedCount();
                    job.remaining = result.pendingCountAfter();
                    job.message = result.message();
                }
                case "EMBEDDING_RETRY_FAILED" -> {
                    int count = embeddingService.retryFailed();
                    job.success = count;
                    job.message = "FAILED_REQUEUED";
                }
                case "POLICY_REGION_REBUILD" -> {
                    PolicyRegionRebuildResult result = regionRebuildService.rebuildAll();
                    job.total = result.totalCount();
                    job.processed = result.processedCount();
                    job.success = result.changedCount();
                    job.failed = result.failedCount();
                    job.remaining = Math.max(0, result.totalCount() - result.processedCount());
                    job.message = "REGION_REBUILD_COMPLETED changed=" + result.changedCount()
                            + ", nationwideToRegion=" + result.nationwideToRegionCount()
                            + ", nationwideToUnknown=" + result.nationwideToUnknownCount()
                            + ", unchanged=" + result.unchangedCount()
                            + ", pendingQueued=" + result.pendingQueuedCount();
                }
                case "FULL_REINDEX" -> {
                    PolicyCollectionResult collection = collectionService.collectAll();
                    EmbeddingQueueResult queue = embeddingService.queueAll(true);
                    EmbeddingProcessResult process = embeddingService.processPending();
                    job.total = queue.activePolicyCount();
                    job.processed = collection.receivedCount() + process.processedCount();
                    job.success = collection.insertedCount() + collection.updatedCount() + process.successCount();
                    job.failed = collection.failedCount() + process.failedCount();
                    job.remaining = process.pendingCountAfter();
                    job.message = "FULL_REINDEX_COMPLETED";
                }
                default -> throw new IllegalArgumentException("UNSUPPORTED_JOB_TYPE: " + job.type);
            }
            job.status = "COMPLETED";
        } catch (RuntimeException ex) {
            job.status = "FAILED";
            job.message = ex.getMessage();
        }
    }

    public Optional<AdminJobStatus> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(MutableJob::snapshot);
    }

    public Optional<AdminJobStatus> latest() {
        return jobs.values().stream()
                .max(Comparator.comparing(job -> job.id))
                .map(MutableJob::snapshot);
    }

    private static class MutableJob {
        private final String id;
        private final String type;
        private String status = "RUNNING";
        private long total;
        private long processed;
        private long success;
        private long failed;
        private long remaining;
        private String message = "";

        private MutableJob(String id, String type) {
            this.id = id;
            this.type = type;
        }

        private AdminJobStatus snapshot() {
            return new AdminJobStatus(id, type, status, total, processed, success, failed, remaining, 0, 0, message);
        }
    }
}
