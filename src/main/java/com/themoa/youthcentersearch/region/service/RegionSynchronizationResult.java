package com.themoa.youthcentersearch.region.service;

import java.util.List;

public record RegionSynchronizationResult(
        int provinceReceivedCount,
        int childReceivedCount,
        int insertedCount,
        int updatedCount,
        int unchangedCount,
        int failedCount,
        List<String> failedProvinceCodes,
        long elapsedTimeMs
) {
}
