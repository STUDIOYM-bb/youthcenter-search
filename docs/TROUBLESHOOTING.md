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

## 다른 지역 정책이 검색됨

관리자 화면에서 `전체 정책 지역 다시 계산`을 실행한다. 이후 변경 정책이 PENDING으로 등록되므로 `전체 PENDING 임베딩 처리`를 실행해 Qdrant Metadata를 갱신한다.

`UNKNOWN`은 기본 검색 결과에서 제외된다. 지역 미확인 정책까지 보고 싶으면 `RAG_INCLUDE_UNKNOWN_REGION=true`를 설정한다.

단, 지역을 입력하지 않은 키워드 검색에서는 지역 미확인 정책도 지역 필터 때문에 제거하지 않는다.

## 온통청년 API 오류

기본 URL은 `/go/ythip/getPlcy`이다. 구버전 `/opi/youthPlcyList.do`는 운영 수집에 사용하지 않는다. API 키는 서버 설정에만 둔다.

## 비밀 파일 ZIP 포함

`.gitignore`는 Git 추적만 막는다. ZIP으로 공유할 때는 `config/application-secret.yml`, `.env`, `build`, 로그, 원본 응답 파일을 직접 제외해야 한다.
