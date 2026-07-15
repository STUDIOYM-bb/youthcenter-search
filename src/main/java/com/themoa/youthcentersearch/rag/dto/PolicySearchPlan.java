package com.themoa.youthcentersearch.rag.dto;

import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 자연어 검색 한 번에 대해 확정된 실행 계획이다.
 * OpenAI/Rule 분석 결과, 구조화 조건, 긍정/부정 분야를 이 객체로 모아 이후 Vector, BM25, Filter, Ranking이
 * 원문을 각자 다시 해석하지 않도록 한다.
 */
public record PolicySearchPlan(
        SearchQueryType queryType,
        String originalQuery,
        String normalizedGoal,
        Set<SearchDomain> desiredDomains,
        Set<SearchDomain> excludedDomains,
        Set<SupportIntent> desiredSupportIntents,
        Set<SupportIntent> excludedSupportIntents,
        Set<String> positiveTerms,
        Set<String> excludedTerms,
        PolicySearchCondition condition,
        Set<EducationStage> userEducationStages,
        boolean educationStageExplicit,
        boolean explicitExclusion,
        String analysisMode
) {
    public PolicySearchPlan {
        queryType = queryType == null ? SearchQueryType.BROAD_DISCOVERY : queryType;
        originalQuery = originalQuery == null ? "" : originalQuery.trim();
        normalizedGoal = StringUtils.hasText(normalizedGoal) ? normalizedGoal.trim() : "청년 지원 정책";
        desiredDomains = desiredDomains == null ? Set.of() : Set.copyOf(desiredDomains);
        excludedDomains = excludedDomains == null ? Set.of() : Set.copyOf(excludedDomains);
        desiredSupportIntents = desiredSupportIntents == null ? Set.of() : Set.copyOf(desiredSupportIntents);
        excludedSupportIntents = excludedSupportIntents == null ? Set.of() : Set.copyOf(excludedSupportIntents);
        positiveTerms = positiveTerms == null ? Set.of() : Set.copyOf(positiveTerms);
        excludedTerms = excludedTerms == null ? Set.of() : Set.copyOf(excludedTerms);
        userEducationStages = userEducationStages == null || userEducationStages.isEmpty()
                ? Set.of(EducationStage.UNKNOWN)
                : Set.copyOf(userEducationStages);
        analysisMode = StringUtils.hasText(analysisMode) ? analysisMode.trim() : "UNKNOWN";
    }
}
