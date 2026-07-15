package com.themoa.youthcentersearch.rag.dto;

import java.util.List;
import java.util.Set;

/**
 * 사용자가 검색 문장에 명시한 교육 단계 조건이다.
 * 취준생, 청년, 나이만으로 학교 단계를 추론하지 않기 위해 evidence와 explicit 값을 함께 전달한다.
 */
public record UserEducationStageCondition(
        Set<EducationStage> stages,
        boolean explicit,
        List<String> evidence
) {
    public UserEducationStageCondition {
        stages = stages == null || stages.isEmpty() ? Set.of(EducationStage.UNKNOWN) : Set.copyOf(stages);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static UserEducationStageCondition unknown() {
        return new UserEducationStageCondition(Set.of(EducationStage.UNKNOWN), false, List.of());
    }
}
