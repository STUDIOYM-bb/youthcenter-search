package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 검색 실행 중 수집한 metrics를 기존 Diagnostics 응답 DTO로 조립한다.
 *
 * <p>API 호환성을 위해 {@link PolicySearchDiagnostics} record 구조는 유지한다.
 * 대신 검색 서비스 본문에서 위치 기반 생성자를 직접 호출하지 않게 하여 필드 순서 실수를 줄인다.</p>
 */
@Component
public class PolicySearchDiagnosticsFactory {

    /**
     * 후보 수집, hard filter, 지역 보장 선택 결과를 하나의 Diagnostics로 변환한다.
     *
     * <p>입력 객체들은 모두 검색 실행 중 실제로 사용된 값이다. Diagnostics는 화면 표시용이므로
     * 여기에서 검색 규칙을 다시 추론하거나 점수를 재계산하지 않는다.</p>
     */
    public PolicySearchDiagnostics create(PolicySearchPlan plan,
                                          PolicySearchIntent intent,
                                          PolicySearchConditionParser.ParsedPolicySearchCondition parsed,
                                          CandidateCollectionMetrics candidates,
                                          PolicySearchFilterMetrics filters,
                                          RegionCoverageResultSelector.Selection selection,
                                          List<PolicySearchResultItem> results,
                                          int semanticScoreCount,
                                          boolean retried,
                                          boolean mysqlFallbackUsed,
                                          String fallbackReason,
                                          long elapsedMillis,
                                          String userEmploymentStatus,
                                          boolean userEmploymentExplicit,
                                          String userEmploymentEvidence,
                                          String userEmploymentSource) {
        PolicySearchCondition condition = plan.condition();
        return new PolicySearchDiagnostics(
                candidates.vectorCandidateCount(),
                candidates.vectorOriginalCandidateCount(),
                candidates.vectorIntentCandidateCount(),
                candidates.vectorExpandedCandidateCount(),
                candidates.vectorCategoryCandidateCount(),
                candidates.lexicalCandidateCount(),
                candidates.mysqlTitleCandidateCount(),
                candidates.mysqlKeywordCandidateCount(),
                candidates.mysqlSummaryCandidateCount(),
                candidates.mysqlCategoryCandidateCount(),
                candidates.loadedPolicyCount(),
                candidates.duplicateCandidateCount(),
                filters.nationwideCandidateCount,
                filters.provinceMatchedCount,
                filters.cityMatchedCount,
                filters.districtMatchedCount,
                filters.exactSigunguMatchedCount,
                filters.exactSidoMatchedCount,
                filters.parentSidoMatchedCount,
                filters.nationwideMatchedCount,
                filters.multipleRegionMatchedCount,
                filters.regionUnknownCount,
                filters.regionNotMatchedCount,
                filters.regionHardFilteredCount,
                semanticScoreCount,
                candidates.loadedPolicyCount(),
                filters.regionFiltered,
                filters.topicThresholdPassedCount,
                filters.topicThresholdFailedCount,
                filters.topicFilteredCount,
                filters.regionEligibleCount,
                filters.regionIneligibleCount,
                filters.ageMatchedCount,
                filters.ageUnknownCount,
                filters.ageMismatchedCount,
                filters.employmentMatchedCount,
                filters.employmentUnknownCount,
                filters.employmentMismatchedCount,
                filters.ageFiltered,
                filters.employmentFiltered,
                filters.studentFiltered,
                filters.targetFiltered,
                filters.applicationFiltered,
                candidates.fallbackAddedCount(),
                results.size(),
                retried,
                mysqlFallbackUsed,
                condition.searchMode().name(),
                condition.regionExplicit(),
                condition.ageExplicit(),
                condition.employmentExplicit(),
                condition.studentExplicit(),
                condition.regionExplicit(),
                condition.ageExplicit(),
                condition.employmentExplicit(),
                condition.studentExplicit(),
                String.join(", ", condition.keywords()),
                String.join(", ", condition.expandedKeywords()),
                fallbackReason,
                elapsedMillis,
                candidates.regionPoolTotal(),
                candidates.regionPoolExactSigungu(),
                candidates.regionPoolParentSido(),
                candidates.regionPoolNationwide(),
                candidates.regionPoolMultiple(),
                filters.unknownExcludedCount,
                filters.wrongRegionExcludedCount,
                selection.exactSigunguSelectedCount(),
                selection.parentSidoSelectedCount(),
                selection.nationwideSelectedCount(),
                0,
                plan.normalizedGoal(),
                plan.desiredDomains().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")),
                plan.excludedDomains().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")),
                String.join(", ", plan.positiveTerms()),
                String.join(", ", plan.excludedTerms()),
                plan.explicitExclusion(),
                intent.semanticQuery(),
                candidates.vectorOriginalCandidateCount() > 0,
                candidates.vectorNormalizedCandidateCount(),
                filters.excludedDomainFiltered,
                plan.userEducationStages().stream().map(Enum::name).sorted().collect(java.util.stream.Collectors.joining(", ")),
                plan.educationStageExplicit(),
                filters.targetFiltered,
                filters.targetUnknownCount,
                userEmploymentStatus,
                userEmploymentExplicit,
                userEmploymentEvidence,
                userEmploymentSource,
                filters.employedMismatchFiltered,
                filters.unemployedMismatchFiltered,
                filters.primaryCandidateCount,
                filters.needsConfirmationCandidateCount,
                results.stream().anyMatch(item -> RecommendationTier.NEEDS_CONFIRMATION.name().equals(item.recommendationTier())),
                semanticConflictDetected(parsed),
                semanticConflictReason(parsed)
        );
    }

    private boolean semanticConflictDetected(PolicySearchConditionParser.ParsedPolicySearchCondition parsed) {
        PolicyQuerySemantics semantics = parsed.semantics();
        if (semantics == null) {
            return false;
        }
        return semantics.desiredDomains().stream().anyMatch(semantics.excludedDomains()::contains);
    }

    private String semanticConflictReason(PolicySearchConditionParser.ParsedPolicySearchCondition parsed) {
        if (!semanticConflictDetected(parsed)) {
            return null;
        }
        return "OpenAI/Rule 의미 분석에서 원하는 분야와 제외 분야가 충돌했습니다.";
    }
}
