package com.themoa.youthcentersearch.region.service;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RegionSynchronizationState {
    private volatile Instant lastSyncTime;
    private volatile RegionSynchronizationResult lastResult;
    private volatile String lastStatus = "NOT_RUN";

    public Instant lastSyncTime() {
        return lastSyncTime;
    }

    public RegionSynchronizationResult lastResult() {
        return lastResult;
    }

    public String lastStatus() {
        return lastStatus;
    }

    public void completed(RegionSynchronizationResult result) {
        completed(result, "COMPLETED");
    }

    public void completed(RegionSynchronizationResult result, String status) {
        this.lastSyncTime = Instant.now();
        this.lastResult = result;
        this.lastStatus = status == null || status.isBlank() ? "COMPLETED" : status;
    }

    public void failed(String message) {
        this.lastSyncTime = Instant.now();
        this.lastStatus = message == null || message.isBlank() ? "FAILED" : "FAILED: " + message;
    }
}
