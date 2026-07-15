package com.themoa.youthcentersearch.rag.service;

/**
 * 검색 후보 평가와 hard filter 과정에서 발생한 카운터를 모은다.
 *
 * <p>지역/나이/취업/교육 단계처럼 명확한 자격 조건은 랭킹 점수가 아니라
 * 필터 단계에서 제거되므로, Diagnostics도 이 단계의 카운터를 기준으로 조립해야 한다.</p>
 */
public class PolicySearchFilterMetrics {
    int regionFiltered;
    int unknownExcludedCount;
    int wrongRegionExcludedCount;
    int nationwideCandidateCount;
    int provinceMatchedCount;
    int cityMatchedCount;
    int districtMatchedCount;
    int exactSigunguMatchedCount;
    int exactSidoMatchedCount;
    int parentSidoMatchedCount;
    int nationwideMatchedCount;
    int multipleRegionMatchedCount;
    int regionUnknownCount;
    int regionNotMatchedCount;
    int regionHardFilteredCount;
    int topicThresholdPassedCount;
    int topicThresholdFailedCount;
    int topicFilteredCount;
    int regionEligibleCount;
    int regionIneligibleCount;
    int ageMatchedCount;
    int ageUnknownCount;
    int ageMismatchedCount;
    int employmentMatchedCount;
    int employmentUnknownCount;
    int employmentMismatchedCount;
    int ageFiltered;
    int employmentFiltered;
    int studentFiltered;
    int targetFiltered;
    int targetUnknownCount;
    int employedMismatchFiltered;
    int unemployedMismatchFiltered;
    int primaryCandidateCount;
    int needsConfirmationCandidateCount;
    int applicationFiltered;
    int excludedDomainFiltered;

    boolean hasNoTopicRelevantCandidate() {
        return (topicFilteredCount > 0 || topicThresholdFailedCount > 0)
                && topicThresholdPassedCount == 0;
    }
}
