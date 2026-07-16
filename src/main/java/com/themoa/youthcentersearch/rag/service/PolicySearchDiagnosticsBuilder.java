package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;

public final class PolicySearchDiagnosticsBuilder {
    private int vectorCandidateCount;
    private int originalVectorCandidateCount;
    private int intentVectorCandidateCount;
    private int expandedVectorCandidateCount;
    private int categoryVectorCandidateCount;
    private int lexicalCandidateCount;
    private int mysqlTitleCandidateCount;
    private int mysqlKeywordCandidateCount;
    private int mysqlSummaryCandidateCount;
    private int mysqlCategoryCandidateCount;
    private int mergedCandidateCount;
    private int duplicateCandidateCount;
    private int nationwideCandidateCount;
    private int provinceMatchedCount;
    private int cityMatchedCount;
    private int districtMatchedCount;
    private int exactSigunguMatchedCount;
    private int exactSidoMatchedCount;
    private int parentSidoMatchedCount;
    private int nationwideMatchedCount;
    private int multipleRegionMatchedCount;
    private int regionUnknownCount;
    private int regionNotMatchedCount;
    private int regionHardFilteredCount;
    private int similarityPassedCount;
    private int databaseLoadedCount;
    private int regionFilteredCount;
    private int topicThresholdPassedCount;
    private int topicThresholdFailedCount;
    private int topicFilteredCount;
    private int regionEligibleCount;
    private int regionIneligibleCount;
    private int ageMatchedCount;
    private int ageUnknownCount;
    private int ageMismatchedCount;
    private int employmentMatchedCount;
    private int employmentUnknownCount;
    private int employmentMismatchedCount;
    private int ageFilteredCount;
    private int employmentFilteredCount;
    private int studentFilteredCount;
    private int targetFilteredCount;
    private int applicationFilteredCount;
    private int mysqlFallbackCount;
    private int finalResultCount;
    private boolean retriedWithLargerTopK;
    private boolean mysqlFallbackUsed;
    private String searchMode;
    private boolean regionExplicit;
    private boolean ageExplicit;
    private boolean employmentExplicit;
    private boolean studentExplicit;
    private boolean regionFilterApplied;
    private boolean ageFilterApplied;
    private boolean employmentFilterApplied;
    private boolean studentFilterApplied;
    private String coreKeywords;
    private String expandedKeywords;
    private String fallbackReason;
    private long elapsedTimeMs;
    private int regionEligiblePoolCount;
    private int exactSigunguPoolCount;
    private int parentSidoPoolCount;
    private int nationwidePoolCount;
    private int multipleRegionPoolCount;
    private int unknownExcludedCount;
    private int wrongRegionExcludedCount;
    private int exactSigunguSelectedCount;
    private int parentSidoSelectedCount;
    private int nationwideSelectedCount;
    private int unknownReviewResultCount;
    private String normalizedGoal;
    private String desiredDomains;
    private String excludedDomains;
    private String positiveKeywords;
    private String excludedKeywords;
    private boolean explicitExclusion;
    private String semanticQuery;
    private boolean originalVectorUsed;
    private int normalizedVectorCandidateCount;
    private int excludedDomainFilteredCount;
    private String userEducationStages;
    private boolean educationStageExplicit;
    private int targetStageMismatchFilteredCount;
    private int targetStageUnknownCount;
    private String userEmploymentStatus;
    private boolean userEmploymentExplicit;
    private String userEmploymentEvidence;
    private String userEmploymentAnalysisSource;
    private int employedMismatchFilteredCount;
    private int unemployedMismatchFilteredCount;
    private int primaryCandidateCount;
    private int needsConfirmationCandidateCount;
    private boolean needsConfirmationUsed;
    private boolean semanticConflictDetected;
    private String semanticConflictReason;

    private PolicySearchDiagnosticsBuilder() {
    }

    public static PolicySearchDiagnosticsBuilder builder() {
        return new PolicySearchDiagnosticsBuilder();
    }

    public PolicySearchDiagnosticsBuilder vectorCandidateCount(int value) { this.vectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder originalVectorCandidateCount(int value) { this.originalVectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder intentVectorCandidateCount(int value) { this.intentVectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder expandedVectorCandidateCount(int value) { this.expandedVectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder categoryVectorCandidateCount(int value) { this.categoryVectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder lexicalCandidateCount(int value) { this.lexicalCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlTitleCandidateCount(int value) { this.mysqlTitleCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlKeywordCandidateCount(int value) { this.mysqlKeywordCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlSummaryCandidateCount(int value) { this.mysqlSummaryCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlCategoryCandidateCount(int value) { this.mysqlCategoryCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mergedCandidateCount(int value) { this.mergedCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder duplicateCandidateCount(int value) { this.duplicateCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder nationwideCandidateCount(int value) { this.nationwideCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder provinceMatchedCount(int value) { this.provinceMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder cityMatchedCount(int value) { this.cityMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder districtMatchedCount(int value) { this.districtMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder exactSigunguMatchedCount(int value) { this.exactSigunguMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder exactSidoMatchedCount(int value) { this.exactSidoMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder parentSidoMatchedCount(int value) { this.parentSidoMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder nationwideMatchedCount(int value) { this.nationwideMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder multipleRegionMatchedCount(int value) { this.multipleRegionMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionUnknownCount(int value) { this.regionUnknownCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionNotMatchedCount(int value) { this.regionNotMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionHardFilteredCount(int value) { this.regionHardFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder similarityPassedCount(int value) { this.similarityPassedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder databaseLoadedCount(int value) { this.databaseLoadedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionFilteredCount(int value) { this.regionFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder topicThresholdPassedCount(int value) { this.topicThresholdPassedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder topicThresholdFailedCount(int value) { this.topicThresholdFailedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder topicFilteredCount(int value) { this.topicFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionEligibleCount(int value) { this.regionEligibleCount = value; return this; }
    public PolicySearchDiagnosticsBuilder regionIneligibleCount(int value) { this.regionIneligibleCount = value; return this; }
    public PolicySearchDiagnosticsBuilder ageMatchedCount(int value) { this.ageMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder ageUnknownCount(int value) { this.ageUnknownCount = value; return this; }
    public PolicySearchDiagnosticsBuilder ageMismatchedCount(int value) { this.ageMismatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentMatchedCount(int value) { this.employmentMatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentUnknownCount(int value) { this.employmentUnknownCount = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentMismatchedCount(int value) { this.employmentMismatchedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder ageFilteredCount(int value) { this.ageFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentFilteredCount(int value) { this.employmentFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder studentFilteredCount(int value) { this.studentFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder targetFilteredCount(int value) { this.targetFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder applicationFilteredCount(int value) { this.applicationFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlFallbackCount(int value) { this.mysqlFallbackCount = value; return this; }
    public PolicySearchDiagnosticsBuilder finalResultCount(int value) { this.finalResultCount = value; return this; }
    public PolicySearchDiagnosticsBuilder retriedWithLargerTopK(boolean value) { this.retriedWithLargerTopK = value; return this; }
    public PolicySearchDiagnosticsBuilder mysqlFallbackUsed(boolean value) { this.mysqlFallbackUsed = value; return this; }
    public PolicySearchDiagnosticsBuilder searchMode(String value) { this.searchMode = value; return this; }
    public PolicySearchDiagnosticsBuilder regionExplicit(boolean value) { this.regionExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder ageExplicit(boolean value) { this.ageExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentExplicit(boolean value) { this.employmentExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder studentExplicit(boolean value) { this.studentExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder regionFilterApplied(boolean value) { this.regionFilterApplied = value; return this; }
    public PolicySearchDiagnosticsBuilder ageFilterApplied(boolean value) { this.ageFilterApplied = value; return this; }
    public PolicySearchDiagnosticsBuilder employmentFilterApplied(boolean value) { this.employmentFilterApplied = value; return this; }
    public PolicySearchDiagnosticsBuilder studentFilterApplied(boolean value) { this.studentFilterApplied = value; return this; }
    public PolicySearchDiagnosticsBuilder coreKeywords(String value) { this.coreKeywords = value; return this; }
    public PolicySearchDiagnosticsBuilder expandedKeywords(String value) { this.expandedKeywords = value; return this; }
    public PolicySearchDiagnosticsBuilder fallbackReason(String value) { this.fallbackReason = value; return this; }
    public PolicySearchDiagnosticsBuilder elapsedTimeMs(long value) { this.elapsedTimeMs = value; return this; }
    public PolicySearchDiagnosticsBuilder regionEligiblePoolCount(int value) { this.regionEligiblePoolCount = value; return this; }
    public PolicySearchDiagnosticsBuilder exactSigunguPoolCount(int value) { this.exactSigunguPoolCount = value; return this; }
    public PolicySearchDiagnosticsBuilder parentSidoPoolCount(int value) { this.parentSidoPoolCount = value; return this; }
    public PolicySearchDiagnosticsBuilder nationwidePoolCount(int value) { this.nationwidePoolCount = value; return this; }
    public PolicySearchDiagnosticsBuilder multipleRegionPoolCount(int value) { this.multipleRegionPoolCount = value; return this; }
    public PolicySearchDiagnosticsBuilder unknownExcludedCount(int value) { this.unknownExcludedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder wrongRegionExcludedCount(int value) { this.wrongRegionExcludedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder exactSigunguSelectedCount(int value) { this.exactSigunguSelectedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder parentSidoSelectedCount(int value) { this.parentSidoSelectedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder nationwideSelectedCount(int value) { this.nationwideSelectedCount = value; return this; }
    public PolicySearchDiagnosticsBuilder unknownReviewResultCount(int value) { this.unknownReviewResultCount = value; return this; }
    public PolicySearchDiagnosticsBuilder normalizedGoal(String value) { this.normalizedGoal = value; return this; }
    public PolicySearchDiagnosticsBuilder desiredDomains(String value) { this.desiredDomains = value; return this; }
    public PolicySearchDiagnosticsBuilder excludedDomains(String value) { this.excludedDomains = value; return this; }
    public PolicySearchDiagnosticsBuilder positiveKeywords(String value) { this.positiveKeywords = value; return this; }
    public PolicySearchDiagnosticsBuilder excludedKeywords(String value) { this.excludedKeywords = value; return this; }
    public PolicySearchDiagnosticsBuilder explicitExclusion(boolean value) { this.explicitExclusion = value; return this; }
    public PolicySearchDiagnosticsBuilder semanticQuery(String value) { this.semanticQuery = value; return this; }
    public PolicySearchDiagnosticsBuilder originalVectorUsed(boolean value) { this.originalVectorUsed = value; return this; }
    public PolicySearchDiagnosticsBuilder normalizedVectorCandidateCount(int value) { this.normalizedVectorCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder excludedDomainFilteredCount(int value) { this.excludedDomainFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder userEducationStages(String value) { this.userEducationStages = value; return this; }
    public PolicySearchDiagnosticsBuilder educationStageExplicit(boolean value) { this.educationStageExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder targetStageMismatchFilteredCount(int value) { this.targetStageMismatchFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder targetStageUnknownCount(int value) { this.targetStageUnknownCount = value; return this; }
    public PolicySearchDiagnosticsBuilder userEmploymentStatus(String value) { this.userEmploymentStatus = value; return this; }
    public PolicySearchDiagnosticsBuilder userEmploymentExplicit(boolean value) { this.userEmploymentExplicit = value; return this; }
    public PolicySearchDiagnosticsBuilder userEmploymentEvidence(String value) { this.userEmploymentEvidence = value; return this; }
    public PolicySearchDiagnosticsBuilder userEmploymentAnalysisSource(String value) { this.userEmploymentAnalysisSource = value; return this; }
    public PolicySearchDiagnosticsBuilder employedMismatchFilteredCount(int value) { this.employedMismatchFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder unemployedMismatchFilteredCount(int value) { this.unemployedMismatchFilteredCount = value; return this; }
    public PolicySearchDiagnosticsBuilder primaryCandidateCount(int value) { this.primaryCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder needsConfirmationCandidateCount(int value) { this.needsConfirmationCandidateCount = value; return this; }
    public PolicySearchDiagnosticsBuilder needsConfirmationUsed(boolean value) { this.needsConfirmationUsed = value; return this; }
    public PolicySearchDiagnosticsBuilder semanticConflictDetected(boolean value) { this.semanticConflictDetected = value; return this; }
    public PolicySearchDiagnosticsBuilder semanticConflictReason(String value) { this.semanticConflictReason = value; return this; }

    public PolicySearchDiagnostics build() {
        return new PolicySearchDiagnostics(vectorCandidateCount, originalVectorCandidateCount, intentVectorCandidateCount,
                expandedVectorCandidateCount, categoryVectorCandidateCount, lexicalCandidateCount, mysqlTitleCandidateCount,
                mysqlKeywordCandidateCount, mysqlSummaryCandidateCount, mysqlCategoryCandidateCount, mergedCandidateCount,
                duplicateCandidateCount, nationwideCandidateCount, provinceMatchedCount, cityMatchedCount, districtMatchedCount,
                exactSigunguMatchedCount, exactSidoMatchedCount, parentSidoMatchedCount, nationwideMatchedCount,
                multipleRegionMatchedCount, regionUnknownCount, regionNotMatchedCount, regionHardFilteredCount,
                similarityPassedCount, databaseLoadedCount, regionFilteredCount, topicThresholdPassedCount,
                topicThresholdFailedCount, topicFilteredCount, regionEligibleCount, regionIneligibleCount,
                ageMatchedCount, ageUnknownCount, ageMismatchedCount, employmentMatchedCount, employmentUnknownCount,
                employmentMismatchedCount, ageFilteredCount, employmentFilteredCount, studentFilteredCount,
                targetFilteredCount, applicationFilteredCount, mysqlFallbackCount, finalResultCount,
                retriedWithLargerTopK, mysqlFallbackUsed, searchMode, regionExplicit, ageExplicit, employmentExplicit,
                studentExplicit, regionFilterApplied, ageFilterApplied, employmentFilterApplied, studentFilterApplied,
                coreKeywords, expandedKeywords, fallbackReason, elapsedTimeMs, regionEligiblePoolCount,
                exactSigunguPoolCount, parentSidoPoolCount, nationwidePoolCount, multipleRegionPoolCount,
                unknownExcludedCount, wrongRegionExcludedCount, exactSigunguSelectedCount, parentSidoSelectedCount,
                nationwideSelectedCount, unknownReviewResultCount, normalizedGoal, desiredDomains, excludedDomains,
                positiveKeywords, excludedKeywords, explicitExclusion, semanticQuery, originalVectorUsed,
                normalizedVectorCandidateCount, excludedDomainFilteredCount, userEducationStages, educationStageExplicit,
                targetStageMismatchFilteredCount, targetStageUnknownCount, userEmploymentStatus, userEmploymentExplicit,
                userEmploymentEvidence, userEmploymentAnalysisSource, employedMismatchFilteredCount,
                unemployedMismatchFilteredCount, primaryCandidateCount, needsConfirmationCandidateCount,
                needsConfirmationUsed, semanticConflictDetected, semanticConflictReason);
    }
}
