package com.themoa.youthcentersearch.rag.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {
    private boolean enabled;
    private String collectionName = "youthcenter_policies";
    private String chatModel = "gpt-4.1-mini";
    private String embeddingModel = "text-embedding-3-small";
    private Embedding embedding = new Embedding();
    private Search search = new Search();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
    public Search getSearch() { return search; }
    public void setSearch(Search search) { this.search = search; }

    public static class Embedding {
        @Min(1)
        @Max(500)
        private int batchSize = 100;
        @Min(1)
        private int maxBatchesPerRun = 1000;
        private Duration requestDelay = Duration.ofMillis(50);
        @Min(1)
        private int maxConsecutiveFailures = 20;

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaxBatchesPerRun() { return maxBatchesPerRun; }
        public void setMaxBatchesPerRun(int maxBatchesPerRun) { this.maxBatchesPerRun = maxBatchesPerRun; }
        public Duration getRequestDelay() { return requestDelay; }
        public void setRequestDelay(Duration requestDelay) { this.requestDelay = requestDelay; }
        public int getMaxConsecutiveFailures() { return maxConsecutiveFailures; }
        public void setMaxConsecutiveFailures(int maxConsecutiveFailures) { this.maxConsecutiveFailures = maxConsecutiveFailures; }
    }

    public static class Search {
        @Min(1)
        private int topK = 50;
        @Min(1)
        private int retryTopK = 150;
        @Min(1)
        private int resultSize = 20;
        private double minimumSimilarity = 0.55;
        private double minimumTopicRelevance = 0.25;
        private double regionSpecificityTieWindow = 0.05;
        private boolean includeUnknownRegion = false;
        private boolean mysqlFallbackEnabled = true;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public int getRetryTopK() { return retryTopK; }
        public void setRetryTopK(int retryTopK) { this.retryTopK = retryTopK; }
        public int getResultSize() { return resultSize; }
        public void setResultSize(int resultSize) { this.resultSize = resultSize; }
        public double getMinimumSimilarity() { return minimumSimilarity; }
        public void setMinimumSimilarity(double minimumSimilarity) { this.minimumSimilarity = minimumSimilarity; }
        public double getMinimumTopicRelevance() { return minimumTopicRelevance; }
        public void setMinimumTopicRelevance(double minimumTopicRelevance) { this.minimumTopicRelevance = minimumTopicRelevance; }
        public double getRegionSpecificityTieWindow() { return regionSpecificityTieWindow; }
        public void setRegionSpecificityTieWindow(double regionSpecificityTieWindow) { this.regionSpecificityTieWindow = regionSpecificityTieWindow; }
        public boolean isIncludeUnknownRegion() { return includeUnknownRegion; }
        public void setIncludeUnknownRegion(boolean includeUnknownRegion) { this.includeUnknownRegion = includeUnknownRegion; }
        public boolean isMysqlFallbackEnabled() { return mysqlFallbackEnabled; }
        public void setMysqlFallbackEnabled(boolean mysqlFallbackEnabled) { this.mysqlFallbackEnabled = mysqlFallbackEnabled; }
    }
}
