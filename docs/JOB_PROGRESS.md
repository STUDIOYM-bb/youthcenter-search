# 관리자 작업 진행률

관리자 작업은 메모리 기반 Job 상태로 진행률을 갱신한다. 진행률 표시를 위해 정책마다 DB에 기록하지 않는다.

공통 응답 필드:

- `overallProgressPercent`: 전체 진행률, 완료 전 최대 99
- `stageProgressPercent`: 현재 단계 진행률
- `indeterminate`: 전체 수를 아직 모를 때 true
- `currentPage`, `totalPages`: 페이지 기반 작업 진행
- `currentBatch`, `totalBatches`: 배치 기반 작업 진행
- `startedAt`, `updatedAt`, `completedAt`
- `elapsedTimeMs`, `throughputPerSecond`, `estimatedRemainingSeconds`

ETA는 현재 처리 속도 기반 참고값이다. 초기 처리량이 적거나 남은 건수 계산이 불가능하면 null로 반환한다.

작업별 단계:

- 정책 수집: 온통청년 연결 중 → 페이지 요청 중 → 정책 저장 중 → 정책 수집 완료
- 지역 동기화: SGIS 인증 중 → 시·도 목록 조회 중 → 시·군 동기화 중 → 지역 캐시 갱신 중
- 지역 재계산: 정책 원본 조회 중 → 지역 재계산 중 → 지역 관계 저장 중 → 재임베딩 등록 중
- 임베딩 대기열: 활성 정책 조회 중 → 임베딩 대기열 등록 중
- 임베딩 처리: 임베딩 준비 중 → OpenAI 임베딩 생성 중 → Qdrant 저장 중

`/dev` 화면은 작업 시작 후 `GET /api/admin/jobs/{jobId}`를 주기적으로 호출한다. Polling 요청에는 `X-Admin-Key`가 포함된다.
