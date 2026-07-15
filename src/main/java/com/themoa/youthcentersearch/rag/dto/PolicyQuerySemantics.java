package com.themoa.youthcentersearch.rag.dto;

import org.springframework.util.StringUtils;

import java.util.Set;

public record PolicyQuerySemantics(
        String normalizedGoal,
        Set<SearchDomain> desiredDomains,
        Set<SearchDomain> excludedDomains,
        Set<String> positiveKeywords,
        Set<String> excludedKeywords,
        boolean explicitExclusion
) {
    public PolicyQuerySemantics {
        normalizedGoal = StringUtils.hasText(normalizedGoal) ? normalizedGoal.trim() : "청년 지원 정책";
        desiredDomains = desiredDomains == null ? Set.of() : Set.copyOf(desiredDomains);
        excludedDomains = excludedDomains == null ? Set.of() : Set.copyOf(excludedDomains);
        positiveKeywords = positiveKeywords == null ? Set.of() : Set.copyOf(positiveKeywords);
        excludedKeywords = excludedKeywords == null ? Set.of() : Set.copyOf(excludedKeywords);
    }

    public static PolicyQuerySemantics empty() {
        return new PolicyQuerySemantics("청년 지원 정책", Set.of(), Set.of(), Set.of("청년"), Set.of(), false);
    }
}
