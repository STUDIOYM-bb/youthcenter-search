package com.themoa.youthcentersearch.rag.dto;

import java.util.List;
import java.util.Set;

/**
 * Search Projection의 대상/자격 텍스트에서 판정한 정책 신청 대상 학교 단계다.
 * stageExclusive=true이면 포함 단계만 신청 가능하다는 강한 근거로 Hard Filter에 사용할 수 있다.
 */
public record PolicyTargetAudienceClassification(
        Set<EducationStage> includedStages,
        Set<EducationStage> excludedStages,
        boolean stageExclusive,
        double confidence,
        List<String> evidence
) {
    public PolicyTargetAudienceClassification {
        includedStages = includedStages == null || includedStages.isEmpty() ? Set.of(EducationStage.UNKNOWN) : Set.copyOf(includedStages);
        excludedStages = excludedStages == null ? Set.of() : Set.copyOf(excludedStages);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static PolicyTargetAudienceClassification unknown() {
        return new PolicyTargetAudienceClassification(Set.of(EducationStage.UNKNOWN), Set.of(), false, 0.0, List.of("대상 단계 판정 근거 없음"));
    }
}
