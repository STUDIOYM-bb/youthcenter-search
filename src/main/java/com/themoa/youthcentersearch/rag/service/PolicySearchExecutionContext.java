package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;

/**
 * 검색 1회 실행의 최상위 문맥이다.
 *
 * <p>Plan 생성 직후 만들어지고 후보 수집, 자격 평가, 랭킹, 응답 조립 단계에 전달된다.
 * 요청 원문과 검색 계획, 시작 시각만 보관하며 후보나 평가 결과를 함께 넣지 않는다.
 * 단계별 데이터는 별도 타입으로 전달해야 검색 흐름의 책임 경계가 흐려지지 않는다.</p>
 *
 * <p>DB 또는 외부 시스템을 호출하지 않는 단순 값 객체다.</p>
 */
public record PolicySearchExecutionContext(
        PolicySearchRequest request,
        PolicySearchPlan plan,
        long startedAt
) {
}
