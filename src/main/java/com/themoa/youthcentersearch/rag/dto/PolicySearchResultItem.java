package com.themoa.youthcentersearch.rag.dto;

import java.time.LocalDate;
import java.util.List;

public record PolicySearchResultItem(
        Integer policyId,
        String sourcePolicyId,
        String title,
        String category,
        String region,
        String regionMatchStatus,
        String regionMatchDescription,
        List<String> regionEvidence,
        String agencyName,
        String summary,
        Integer minAge,
        Integer maxAge,
        String employmentStatus,
        LocalDate startDate,
        LocalDate dueDate,
        String applicationStatus,
        String officialUrl,
        double semanticScore,
        double finalScore,
        List<String> matchedReasons,
        List<String> needCheckReasons,
        String regionCompatibility,
        String regionMatchLabel,
        String regionMatchReason,
        int regionScore,
        List<String> candidateSources,
        String ageMatchStatus,
        String ageMatchReason,
        String employmentMatchStatus,
        String employmentMatchReason,
        String studentMatchStatus,
        String studentMatchReason,
        double topicScore,
        String primaryDomain,
        List<String> secondaryDomains,
        List<String> supportIntents,
        List<String> domainEvidence,
        boolean excludedDomainPassed
) {
}
