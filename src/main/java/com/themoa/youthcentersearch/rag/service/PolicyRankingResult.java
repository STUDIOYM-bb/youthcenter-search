package com.themoa.youthcentersearch.rag.service;

import java.util.List;

/**
 * 랭킹 단계의 출력이다.
 *
 * <p>자격 평가 이후, 결과 DTO 조립 이전에 만들어진다. topic/domain/support intent 점수와
 * 최종 점수, 최종 순위를 포함한다. 페이지네이션 전에 전체 후보의 순위를 확정해야 Explain과
 * 목록 응답이 같은 finalRank를 공유할 수 있다.</p>
 *
 * <p>DB 또는 외부 시스템을 호출하지 않는 결과 객체다. 새 점수 요소를 추가할 때는
 * PolicyRankingService와 PolicySearchResultAssembler, PolicySearchExplainService를 함께 확인한다.</p>
 */
public record PolicyRankingResult(
        List<RankedPolicyCandidate> rankedCandidates,
        PolicySearchFilterMetrics metrics
) {
    public PolicyRankingResult {
        rankedCandidates = rankedCandidates == null ? List.of() : List.copyOf(rankedCandidates);
    }
}
