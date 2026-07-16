package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;

/**
 * 후보 평가 단계에서만 사용하는 작은 실행 문맥이다.
 *
 * <p>후보 수집 이후, 자격 평가 내부에서 SearchPlan과 후보 묶음을 함께 참조할 때 사용한다.
 * 랭킹 점수나 응답 DTO 상태를 담지 않으며 DB 또는 외부 시스템을 호출하지 않는다.</p>
 */
public record PolicyEvaluationContext(
        PolicySearchPlan plan,
        PolicyCandidateCollection candidates
) {
}
