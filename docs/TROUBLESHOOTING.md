# Troubleshooting

## Docker Compose 포트 충돌

3306, 6333, 6334 포트를 이미 사용하는 컨테이너가 있으면 compose가 실패한다. 기존 컨테이너를 계속 사용할 수 있다면 `config/application-secret.yml`의 접속 정보만 맞춘다.

## Flyway checksum mismatch

이미 마이그레이션이 적용된 DB에서 SQL 파일이 바뀌면 checksum mismatch가 발생한다. 개발 중 새 프로젝트 DB라면 DB를 다시 만들 수 있지만, 운영 DB에서는 기존 migration을 수정하지 말고 새 V 파일을 추가해야 한다.

## OpenAI Bean이 생성되지 않음

`SPRING_AI_MODEL_CHAT=openai`, `SPRING_AI_MODEL_EMBEDDING=openai`, `OPENAI_API_KEY`를 확인한다. 키가 없으면 RuleBased parser로 fallback되지만 임베딩 처리는 사용할 수 없다.

## Qdrant 연결 실패

Qdrant gRPC 포트는 기본 6334다. `QDRANT_HOST`, `QDRANT_GRPC_PORT`, `QDRANT_COLLECTION_NAME`을 확인한다.

## 검색 결과가 없음

정책 수집과 임베딩이 먼저 완료되어야 한다. 관리자 화면에서 활성 정책 수, PENDING, SYNCED 상태를 확인한다.

지역을 입력하지 않은 `청년 면접 수당` 같은 검색은 `KEYWORD` 모드로 동작해야 한다. 검색 요약에서 `지역 필터: 미적용`, `나이 필터: 미적용`, `취업 상태 필터: 미적용`인지 확인한다.

OpenAI가 말하지 않은 조건을 `age=0`, `studentStatus=false`, 빈 문자열로 반환해도 서버에서 null로 정리한다. 그래도 결과가 없으면 검색 진단의 `fallbackReason`을 확인한다.

특정 정책이 왜 노출되지 않았는지 확인하려면 `/dev`의 `정책 미노출 원인 분석`을 사용한다. `NOT_RETRIEVED`, `REMOVED_BY_REGION`, `REMOVED_BY_AGE`, `REMOVED_BY_EMPLOYMENT`, `REMOVED_BY_TOPIC_THRESHOLD`, `BELOW_RESULT_LIMIT` 중 어느 단계에서 사라졌는지 확인할 수 있다.

지역은 맞는데 주제가 다른 정책만 남는다면 `Topic 통과`, `Topic 제외`, 후보 검색 경로를 확인한다. `RAG_MINIMUM_TOPIC_RELEVANCE`가 너무 높으면 관련 정책이 제외될 수 있고, 너무 낮으면 지역만 맞는 무관 정책이 남을 수 있다.

## 다른 지역 정책이 검색됨

관리자 화면에서 `전체 정책 지역 다시 계산`을 실행한다. 이후 변경 정책이 PENDING으로 등록되므로 `전체 PENDING 임베딩 처리`를 실행해 Qdrant Metadata를 갱신한다.

`UNKNOWN`은 기본 검색 결과에서 제외된다. 지역 미확인 정책까지 보고 싶으면 `RAG_INCLUDE_UNKNOWN_REGION=true`를 설정한다.

단, 지역을 입력하지 않은 키워드 검색에서는 지역 미확인 정책도 지역 필터 때문에 제거하지 않는다.

## 전국 또는 상위 시·도 정책이 보이지 않음

하위 시·군·자치구 검색에서는 정확한 하위 지역, 상위 시·도 전체, 전국 정책이 모두 지역 호환 후보여야 한다. 검색 진단에서 `상위 시·도 전체`, `전국 정책`, `다른 지역 후보` 수를 확인한다.

`전국 정책` 수가 계속 0이면 먼저 MySQL의 명시적 전국 정책 데이터가 있는지 확인한다. MySQL에는 전국 정책이 있는데 Qdrant Metadata만 누락된 경우에는 전체 재수집이 아니라 해당 전국 정책만 PENDING으로 등록해 부분 재임베딩한다. 지역 정보가 없다는 이유로 정책을 전국으로 바꾸면 안 된다.

## 온통청년 API 오류

기본 URL은 `/go/ythip/getPlcy`이다. 구버전 `/opi/youthPlcyList.do`는 운영 수집에 사용하지 않는다. API 키는 서버 설정에만 둔다.

## 비밀 파일 ZIP 포함

`.gitignore`는 Git 추적만 막는다. ZIP으로 공유할 때는 `config/application-secret.yml`, `.env`, `build`, 로그, 원본 응답 파일을 직접 제외해야 한다.
# 지역 검색에서 특정 시·군이 인식되지 않음

`칠곡`, `횡성`, `예산`, `해남`, `합천` 같은 지역이 `정보 없음`으로 표시되면 `region_code` 카탈로그가 최신이 아닐 수 있다.

확인 순서:

1. `config/application-secret.yml`에 `SGIS_CONSUMER_KEY`, `SGIS_CONSUMER_SECRET` 설정
2. `SGIS_REGION_SYNC_ENABLED=true`
3. `/dev` → `전국 행정지역 동기화`
4. `/dev` → 지역 검색 진단에서 입력값 확인
5. 기존 정책 반영이 필요하면 `전체 정책 지역 다시 계산`
6. Qdrant 반영이 필요하면 `전체 PENDING 임베딩 처리`

검색 요청 자체는 SGIS API를 호출하지 않는다. 동기화가 실패해도 기존 로컬 카탈로그로 검색은 계속 동작한다.

## 정책 지역 재계산에서 Snapshot 없음

`Snapshot 미보유 정책 수`가 0보다 크면 과거에 수집된 정책 중 정책별 원본 JSON이 없는 항목이 있다는 뜻이다. 이 경우 지역 재계산은 fallback 컬럼만 사용하므로 `zipCd`나 상세 기관 필드를 충분히 활용하지 못할 수 있다.

해결 순서:

1. 온통청년 전체 정책 수집
2. Snapshot 보유 정책 수 확인
3. 전체 정책 지역 다시 계산
4. 전체 PENDING 임베딩 처리

## 관리자 작업 진행률이 멈춘 것처럼 보임

작업은 `/api/admin/jobs/{jobId}` polling으로 갱신된다. 관리자 키를 저장하지 않았거나 브라우저 세션이 초기화되면 polling 요청이 거부될 수 있다.
