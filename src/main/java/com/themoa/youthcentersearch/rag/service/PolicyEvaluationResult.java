package com.themoa.youthcentersearch.rag.service;

import java.util.List;

/**
 * 자격 평가 단계의 출력이다.
 *
 * <p>후보 수집 이후, 랭킹 이전에 만들어진다. 명확한 지역·나이·취업·교육 단계 불일치와
 * 신청 마감 정책은 passedCandidates에서 제외되고, 제외/확인 필요 카운터는 metrics에 보존된다.</p>
 *
 * <p>DB 또는 외부 시스템을 호출하지 않는 결과 객체다. 새 hard filter를 추가할 때는
 * PolicyEligibilityEvaluator에서 판정 근거와 metrics를 함께 갱신해야 한다.</p>
 */
public record PolicyEvaluationResult(
        List<EvaluatedPolicyCandidate> passedCandidates,
        PolicySearchFilterMetrics metrics
) {
    public PolicyEvaluationResult {
        passedCandidates = passedCandidates == null ? List.of() : List.copyOf(passedCandidates);
    }
}
