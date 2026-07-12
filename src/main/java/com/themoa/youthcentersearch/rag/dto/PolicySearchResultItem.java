package com.themoa.youthcentersearch.rag.dto;

import java.time.LocalDate;
import java.util.List;

public record PolicySearchResultItem(
        Integer policyId,
        String sourcePolicyId,
        String title,
        String category,
        String region,
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
        List<String> needCheckReasons
) {
}
