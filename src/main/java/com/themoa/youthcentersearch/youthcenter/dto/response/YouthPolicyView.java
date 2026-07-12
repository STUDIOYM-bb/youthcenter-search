package com.themoa.youthcentersearch.youthcenter.dto.response;

import java.util.List;
import java.util.Map;

public record YouthPolicyView(
        String policyNumber,
        String policyName,
        String policyDescription,
        List<String> keywordNames,
        String majorCategory,
        String middleCategory,
        String supportContent,
        String supervisingInstitution,
        String operatingInstitution,
        Integer minimumAge,
        Integer maximumAge,
        boolean ageLimited,
        String applicationPeriod,
        String applicationMethod,
        String applicationUrl,
        List<String> referenceUrls,
        String incomeCondition,
        String additionalQualification,
        String participantTarget,
        List<String> regionCodes,
        Map<String, Object> rawFields
) {
}
