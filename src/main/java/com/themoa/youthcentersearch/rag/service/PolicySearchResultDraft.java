package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;

import java.time.LocalDate;
import java.util.List;

record PolicySearchResultDraft(
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
        boolean excludedDomainPassed,
        List<String> targetEducationStages,
        List<String> targetEducationExcludedStages,
        List<String> targetEducationEvidence,
        String targetStageMatchStatus,
        String targetStageMatchReason,
        List<String> allowedEmploymentStatuses,
        boolean employmentAudienceExclusive,
        List<String> employmentAudienceEvidence,
        String recommendationTier,
        String recommendationTierReason
) {
    PolicySearchResultItem toResultItem() {
        return new PolicySearchResultItem(policyId, sourcePolicyId, title, category, region, regionMatchStatus,
                regionMatchDescription, regionEvidence, agencyName, summary, minAge, maxAge, employmentStatus,
                startDate, dueDate, applicationStatus, officialUrl, semanticScore, finalScore, matchedReasons,
                needCheckReasons, regionCompatibility, regionMatchLabel, regionMatchReason, regionScore, candidateSources,
                ageMatchStatus, ageMatchReason, employmentMatchStatus, employmentMatchReason, studentMatchStatus,
                studentMatchReason, topicScore, primaryDomain, secondaryDomains, supportIntents, domainEvidence,
                excludedDomainPassed, targetEducationStages, targetEducationExcludedStages, targetEducationEvidence,
                targetStageMatchStatus, targetStageMatchReason, allowedEmploymentStatuses, employmentAudienceExclusive,
                employmentAudienceEvidence, recommendationTier, recommendationTierReason);
    }
}
