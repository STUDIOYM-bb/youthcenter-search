package com.themoa.youthcentersearch.youthcenter.search;

public record YouthCenterSearchPlan(
        String filterType,
        String filterValue,
        String policyKeywordName,
        String policyDescription
) {
}
