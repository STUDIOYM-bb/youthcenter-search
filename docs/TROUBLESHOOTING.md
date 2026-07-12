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

## 온통청년 API 오류

기본 URL은 `/go/ythip/getPlcy`이다. 구버전 `/opi/youthPlcyList.do`는 운영 수집에 사용하지 않는다. API 키는 서버 설정에만 둔다.

## 비밀 파일 ZIP 포함

`.gitignore`는 Git 추적만 막는다. ZIP으로 공유할 때는 `config/application-secret.yml`, `.env`, `build`, 로그, 원본 응답 파일을 직접 제외해야 한다.
