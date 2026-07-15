package com.themoa.youthcentersearch.rag.dto;

import java.util.List;

/**
 * 원문에서 판정한 사용자 취업 상태와 근거다.
 */
public record UserEmploymentStatusResult(
        UserEmploymentStatus status,
        boolean explicit,
        double confidence,
        List<String> evidence
) {
    public UserEmploymentStatusResult {
        status = status == null ? UserEmploymentStatus.UNKNOWN : status;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static UserEmploymentStatusResult unknown() {
        return new UserEmploymentStatusResult(UserEmploymentStatus.UNKNOWN, false, 0.0, List.of());
    }
}
