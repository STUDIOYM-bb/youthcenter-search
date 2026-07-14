package com.themoa.youthcentersearch.rag.dto;

public record ConditionMatchResult(
        ConditionMatchStatus status,
        String reason,
        double score
) {
    public static ConditionMatchResult match(String reason) {
        return new ConditionMatchResult(ConditionMatchStatus.MATCH, reason, 1.0);
    }

    public static ConditionMatchResult unknown(String reason) {
        return new ConditionMatchResult(ConditionMatchStatus.UNKNOWN, reason, 0.65);
    }

    public static ConditionMatchResult mismatch(String reason) {
        return new ConditionMatchResult(ConditionMatchStatus.MISMATCH, reason, 0.0);
    }
}
