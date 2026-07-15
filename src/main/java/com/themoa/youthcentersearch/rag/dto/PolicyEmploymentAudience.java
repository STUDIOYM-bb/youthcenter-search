package com.themoa.youthcentersearch.rag.dto;

import java.util.List;
import java.util.Set;

/**
 * 정책 신청 대상이 허용하는 취업 상태다.
 * exclusive=true일 때만 EMPLOYED/UNEMPLOYED 불일치를 hard filter로 적용한다.
 */
public record PolicyEmploymentAudience(
        Set<UserEmploymentStatus> allowedStatuses,
        boolean exclusive,
        double confidence,
        List<String> evidence
) {
    public PolicyEmploymentAudience {
        allowedStatuses = allowedStatuses == null || allowedStatuses.isEmpty()
                ? Set.of(UserEmploymentStatus.UNKNOWN)
                : Set.copyOf(allowedStatuses);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static PolicyEmploymentAudience unknown() {
        return new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNKNOWN), false, 0.0, List.of("취업 상태 대상 근거 없음"));
    }
}
