# 정책별 원본 Snapshot

`policy_raw_data`는 온통청년 API 페이지 원본을 페이지당 한 번 저장한다. 페이지 응답을 정책마다 중복 저장하지 않는다.

정책별 지역 판별과 디버깅을 위해 `policy_source_snapshot` 테이블을 추가했다.

저장 내용:

- `policy_id`
- `raw_data_id`
- `source`
- `source_policy_id`
- `raw_policy_json`
- `raw_content_hash`
- `collected_at`
- `updated_at`

정책 수집 시 `PolicyPersistenceService.upsert(item, rawData)`가 정책 저장 후 snapshot을 upsert한다. 기존 정책을 다시 수집하면 snapshot은 최신 원본 JSON으로 갱신된다.

지역 재계산은 snapshot이 있으면 `raw_policy_json` 전체를 사용한다. snapshot이 없으면 기존 policy 컬럼 기반 fallback을 사용하고 `snapshotMissingCount`, `fallbackUsedCount`, `reviewRequiredCount`를 증가시킨다.

기존 정책에 snapshot이 없으면 온통청년 전체 정책 수집을 다시 실행해야 완전한 원본 기반 지역 재계산이 가능하다.
