# Collection Flow

온통청년 전체 수집은 관리자 Job으로 실행한다.

1. `POST /api/admin/jobs/policy-collection`
2. 1페이지를 호출한다.
3. `result.pagging.totCount`와 설정 `page-size`로 마지막 페이지를 계산한다.
4. 페이지 원본 응답을 `policy_raw_data`에 한 번 저장한다.
5. `result.youthPolicyList`를 파싱한다.
6. 정책별로 `source_type=YOUTH_CENTER`, `source_policy_id=plcyNo` 기준 Upsert한다.
7. `policy_condition`은 기존 행을 갱신한다.
8. `policy_region`은 기존 관계와 신규 관계를 비교해 차이만 반영한다.
9. 반복 페이지 또는 동일 첫 정책 번호 반복이 감지되면 중단한다.

상세 조회는 `DetailFetchMode`로 제어한다.

- `NEVER`: 상세 API를 호출하지 않음
- `MISSING_ONLY`: 핵심 필드가 부족할 때만 호출
- `ALWAYS`: 매 정책 상세 API 호출

기본값은 `MISSING_ONLY`이다.
