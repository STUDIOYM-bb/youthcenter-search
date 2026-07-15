package com.themoa.youthcentersearch.rag.dto;

/**
 * 사용자 교육 단계와 정책 대상 단계의 hard filter 결과다.
 */
public record TargetStageMatchResult(
        ConditionMatchStatus status,
        String reason
) {
    public static TargetStageMatchResult match(String reason) {
        return new TargetStageMatchResult(ConditionMatchStatus.MATCH, reason);
    }

    public static TargetStageMatchResult mismatch(String reason) {
        return new TargetStageMatchResult(ConditionMatchStatus.MISMATCH, reason);
    }

    public static TargetStageMatchResult unknown(String reason) {
        return new TargetStageMatchResult(ConditionMatchStatus.UNKNOWN, reason);
    }
}
