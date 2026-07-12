package com.themoa.youthcentersearch.rag.dto;

public record PolicySearchDiagnostics(
        int vectorCandidateCount,
        int similarityPassedCount,
        int databaseLoadedCount,
        int regionFilteredCount,
        int ageFilteredCount,
        int employmentFilteredCount,
        int studentFilteredCount,
        int targetFilteredCount,
        int applicationFilteredCount,
        int mysqlFallbackCount,
        int finalResultCount,
        boolean retriedWithLargerTopK,
        boolean mysqlFallbackUsed,
        String fallbackReason,
        long elapsedTimeMs
) {
}
