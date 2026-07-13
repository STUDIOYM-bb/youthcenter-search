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
# 정책별 원본 Snapshot

페이지 원본은 `policy_raw_data`에 페이지당 한 번 저장한다. 정책 저장이 성공하면 각 정책의 원본 필드 전체를 `policy_source_snapshot`에 upsert한다.

이 snapshot은 `zipCd`, `plcySprtCn`, `ptcpPrpTrgtCn`, `addAplyQlfcCndCn`, 기관 필드 등 지역 재계산에 필요한 온통청년 원본 필드를 보존하기 위한 운영 테이블이다.

관리자 정책 수집 Job은 진행률 응답에 단계, 페이지, 처리 건수, 경과 시간, ETA를 포함한다.
