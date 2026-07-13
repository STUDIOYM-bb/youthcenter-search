package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.admin.dto.AdminJobStatus;
import com.themoa.youthcentersearch.policy.service.PolicyCollectionResult;
import com.themoa.youthcentersearch.policy.service.PolicyRegionRebuildResult;
import com.themoa.youthcentersearch.policy.service.PolicyRegionRebuildService;
import com.themoa.youthcentersearch.policy.service.YouthCenterPolicyCollectionService;
import com.themoa.youthcentersearch.rag.service.EmbeddingProcessResult;
import com.themoa.youthcentersearch.rag.service.EmbeddingQueueResult;
import com.themoa.youthcentersearch.rag.service.PolicyEmbeddingService;
import com.themoa.youthcentersearch.region.service.RegionSynchronizationResult;
import com.themoa.youthcentersearch.region.service.RegionSynchronizationService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.time.Instant;
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
    private final RegionSynchronizationService regionSynchronizationService;
    private final TaskExecutor adminJobExecutor;
    private final Map<String, MutableJob> jobs = new ConcurrentHashMap<>();

    public AdminJobService(YouthCenterPolicyCollectionService collectionService, PolicyEmbeddingService embeddingService,
                           PolicyRegionRebuildService regionRebuildService,
                           RegionSynchronizationService regionSynchronizationService,
                           @Qualifier("adminJobExecutor") TaskExecutor adminJobExecutor) {
        this.collectionService = collectionService;
        this.embeddingService = embeddingService;
        this.regionRebuildService = regionRebuildService;
        this.regionSynchronizationService = regionSynchronizationService;
        this.adminJobExecutor = adminJobExecutor;
    }

    public AdminJobStatus start(String type) {
        if (jobs.values().stream().anyMatch(job -> job.type.equals(type) && "RUNNING".equals(job.status))) {
            throw new IllegalStateException("JOB_ALREADY_RUNNING");
        }
        MutableJob job = new MutableJob(UUID.randomUUID().toString(), type);
        jobs.put(job.id, job);
        CompletableFuture.runAsync(() -> run(job), runnable -> adminJobExecutor.execute(runnable));
        return job.snapshot();
    }

    private void run(MutableJob job) {
        try {
            switch (job.type) {
                case "POLICY_COLLECTION" -> {
                    job.update(new JobProgressUpdate("CONNECTING", "온통청년 연결 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "온통청년 API 연결 중"));
                    PolicyCollectionResult result = collectionService.collectAll(job::update);
                    job.processed = result.receivedCount();
                    job.success = result.insertedCount() + result.updatedCount();
                    job.failed = result.failedCount();
                    job.message = result.status();
                    if ("FAILED".equals(result.status())) {
                        job.status = "FAILED";
                    }
                }
                case "EMBEDDING_QUEUE" -> {
                    job.update(new JobProgressUpdate("QUEUEING", "임베딩 대기열 등록 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "활성 정책을 조회하고 있습니다."));
                    EmbeddingQueueResult result = embeddingService.queueAll(false, job::update);
                    job.total = result.activePolicyCount();
                    job.success = result.newlyQueuedCount() + result.requeuedCount();
                    job.remaining = result.pendingCountAfter();
                    job.message = "QUEUE_COMPLETED";
                }
                case "EMBEDDING_PROCESS" -> {
                    long initialPending = embeddingService.pendingCount();
                    int batchSize = Math.max(1, embeddingService.batchSize());
                    int totalBatches = (int) Math.ceil((double) initialPending / batchSize);
                    job.total = initialPending;
                    job.remaining = initialPending;
                    job.totalBatches = totalBatches;
                    job.update(new JobProgressUpdate("PREPARING", "임베딩 준비 중", initialPending, 0, 0, 0, 0, 0, 0, totalBatches, null, 0, 0, "PENDING 임베딩을 준비하고 있습니다."));
                    EmbeddingProcessResult result = embeddingService.processPending(progress -> {
                        job.update(new JobProgressUpdate("PROCESSING", "OpenAI 임베딩 생성 중", initialPending,
                                progress.processedCount(), progress.successCount(), progress.failedCount(), 0, 0,
                                Math.min(totalBatches, (int) Math.ceil((double) progress.processedCount() / batchSize)),
                                totalBatches, null, 0, 0, "임베딩을 처리하고 있습니다."));
                        job.remaining = progress.pendingCountAfter();
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
                    job.update(new JobProgressUpdate("REBUILDING", "지역 재계산 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "정책 지역을 다시 계산합니다."));
                    PolicyRegionRebuildResult result = regionRebuildService.rebuildAll(job::update);
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
                case "REGION_CATALOG_SYNC" -> {
                    job.update(new JobProgressUpdate("AUTHENTICATING", "SGIS 인증 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "SGIS 인증 중입니다."));
                    RegionSynchronizationResult result = regionSynchronizationService.synchronize(job::update);
                    job.total = result.provinceReceivedCount() + result.childReceivedCount();
                    job.processed = job.total;
                    job.success = result.insertedCount() + result.updatedCount() + result.unchangedCount();
                    job.failed = result.failedCount();
                    job.remaining = 0;
                    job.message = "REGION_CATALOG_SYNC_COMPLETED provinces=" + result.provinceReceivedCount()
                            + ", children=" + result.childReceivedCount()
                            + ", inserted=" + result.insertedCount()
                            + ", updated=" + result.updatedCount()
                            + ", unchanged=" + result.unchangedCount()
                            + ", failedProvinceCodes=" + result.failedProvinceCodes();
                }
                case "FULL_REINDEX" -> {
                    PolicyCollectionResult collection = collectionService.collectAll(job::update);
                    EmbeddingQueueResult queue = embeddingService.queueAll(true, job::update);
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
            if ("RUNNING".equals(job.status)) {
                job.status = job.failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED";
            }
            job.completedAt = Instant.now();
        } catch (RuntimeException ex) {
            job.status = "FAILED";
            job.completedAt = Instant.now();
            job.message = ex.getMessage();
        }
    }

    public Optional<AdminJobStatus> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(MutableJob::snapshot);
    }

    public Optional<AdminJobStatus> latest() {
        return jobs.values().stream()
                .max(Comparator.comparing(job -> job.startedAt))
                .map(MutableJob::snapshot);
    }

    private static class MutableJob {
        private final String id;
        private final String type;
        private final Instant startedAt = Instant.now();
        private volatile Instant updatedAt = startedAt;
        private volatile Instant completedAt;
        private String status = "RUNNING";
        private String stage;
        private String stageLabel;
        private long total;
        private long processed;
        private long success;
        private long failed;
        private long remaining;
        private int currentPage;
        private int totalPages;
        private int currentBatch;
        private int totalBatches;
        private String currentItem;
        private long apiRequestCount;
        private long retryCount;
        private String message = "";

        private MutableJob(String id, String type) {
            this.id = id;
            this.type = type;
        }

        private synchronized void update(JobProgressUpdate update) {
            this.stage = update.stage();
            this.stageLabel = update.stageLabel();
            this.total = update.total() > 0 ? update.total() : this.total;
            this.processed = update.processed();
            this.success = update.success();
            this.failed = update.failed();
            this.remaining = Math.max(0, this.total - this.processed);
            this.currentPage = update.currentPage();
            this.totalPages = update.totalPages();
            this.currentBatch = update.currentBatch();
            this.totalBatches = update.totalBatches();
            this.currentItem = update.currentItem();
            this.apiRequestCount = update.apiRequestCount();
            this.retryCount = update.retryCount();
            this.message = update.message();
            this.updatedAt = Instant.now();
        }

        private AdminJobStatus snapshot() {
            long elapsed = Duration.between(startedAt, completedAt == null ? Instant.now() : completedAt).toMillis();
            Double throughput = elapsed > 0 && processed > 0 ? processed / (elapsed / 1000.0) : null;
            Long eta = throughput != null && throughput > 0 && remaining > 0 ? Math.round(remaining / throughput) : null;
            Integer percent = percent();
            return new AdminJobStatus(id, type, status, stage, stageLabel, percent, percent, total <= 0,
                    total, processed, success, failed, remaining, currentPage, totalPages, currentBatch, totalBatches,
                    currentItem, apiRequestCount, retryCount, startedAt, updatedAt, completedAt, elapsed, eta, throughput, message);
        }

        private Integer percent() {
            if (total <= 0) return null;
            if ("COMPLETED".equals(status) || "COMPLETED_WITH_ERRORS".equals(status)) return 100;
            int value = (int) Math.floor((double) Math.min(processed, total) * 100 / total);
            return "RUNNING".equals(status) ? Math.min(value, 99) : Math.max(0, Math.min(value, 100));
        }
    }
}
