package com.themoa.youthcentersearch.policy.service;

public record PolicyRegionRebuildResult(
        long totalCount,
        long processedCount,
        long changedCount,
        long nationwideToRegionCount,
        long nationwideToUnknownCount,
        long unchangedCount,
        long failedCount,
        long pendingQueuedCount
) {
}
