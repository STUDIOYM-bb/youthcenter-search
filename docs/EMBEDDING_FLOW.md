# Embedding Flow

임베딩은 사전 처리 작업이다. 사용자 검색 중 자동으로 전체 임베딩을 실행하지 않는다.

## 대기열 등록

`POST /api/admin/jobs/embedding-queue`

- 활성 정책 전체 조회
- 정책 문서 생성
- SHA-256 `contentHash` 계산
- 신규 정책은 `PENDING`
- 내용이 바뀐 정책은 `PENDING`
- 동일 hash로 이미 `SYNCED`이면 유지

## 처리

`POST /api/admin/jobs/embedding-process`

1. `PENDING` ID를 batch-size만큼 조회한다.
2. 해당 정책을 다시 로드한다.
3. `PolicyDocumentBuilder`로 문서 본문을 만든다.
4. OpenAI Embedding과 Qdrant VectorStore에 저장한다.
5. 성공하면 `SYNCED`, 실패하면 `FAILED`로 표시한다.
6. `PENDING`이 0이 될 때까지 반복한다.

컬렉션 fetch join과 Pageable을 같이 쓰지 않고, ID 조회 후 상세 로드를 사용한다.

문서 ID는 정책 ID 기반 결정적 UUID이므로 재임베딩 시 Qdrant point가 중복 생성되지 않는다.
# 임베딩 진행률과 배치 처리

PENDING 임베딩 처리는 시작 시 initial pending 수를 `totalCount`로 고정한다. 진행률은 처리 건수 / initial pending 수로 계산하며 완료 전에는 100%를 표시하지 않는다.

기본 처리는 배치 단위로 `vectorStore.add(documents)`를 호출한다. 배치 실패 시 해당 배치를 개별 문서 처리로 전환하고 실패 정책만 `FAILED`로 표시한다. Qdrant Point ID는 기존 deterministic ID를 유지한다.
