# Local Setup

처음 clone한 개발자는 예제 파일을 복사해 로컬 전용 설정 파일을 만든 뒤 실제 Key를 직접 입력한다. 실제 Key와 비밀번호는 Git 추적 파일에 넣지 않는다.

## Windows

```powershell
git clone https://github.com/STUDIOYM-bb/youthcenter-search.git
cd youthcenter-search

.\scripts\setup-local.ps1

# config/application-secret.yml에 실제 Key 입력

docker compose up -d
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

## macOS/Linux

```bash
git clone https://github.com/STUDIOYM-bb/youthcenter-search.git
cd youthcenter-search

chmod +x scripts/setup-local.sh
./scripts/setup-local.sh

# config/application-secret.yml에 실제 Key 입력

docker compose up -d
./gradlew clean test
./gradlew bootRun
```

## SGIS 지역 동기화 설정

전국 시·군·구 지역 검색을 사용하려면 SGIS 키를 발급받아 `config/application-secret.yml`에 입력한다.

```yaml
SGIS_CONSUMER_KEY: ""
SGIS_CONSUMER_SECRET: ""
SGIS_REGION_SYNC_ENABLED: true
SGIS_REGION_SYNC_ON_STARTUP: false
```

서버 시작 자동 동기화는 기본적으로 끈다. `/dev` 관리자 화면에서 `전국 행정지역 동기화`를 수동 실행한 뒤, 기존 정책에 적용하려면 `전체 정책 지역 다시 계산`과 `전체 PENDING 임베딩 처리`를 순서대로 실행한다.

## 생성되는 로컬 파일

Windows PowerShell:

```powershell
Copy-Item config/application-secret.example.yml config/application-secret.yml
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cp config/application-secret.example.yml config/application-secret.yml
cp .env.example .env
```

`config/application-secret.yml`은 IntelliJ 또는 `bootRun`으로 실행하는 Spring Boot가 읽는다.

`.env`는 Docker Compose가 읽는다. Spring Boot는 별도 라이브러리 없이는 `.env`를 자동으로 읽지 않는다.

## 필수 입력

`config/application-secret.yml`에 최소한 다음 값을 입력한다.

```yaml
YOUTH_CENTER_API_KEY: "실제 온통청년 API Key"
OPENAI_API_KEY: "실제 OpenAI API Key"
ADMIN_API_KEY: "로컬 관리자 Key"
```

RAG를 사용할 때는 다음 값도 활성화한다.

```yaml
SPRING_AI_MODEL_CHAT: openai
SPRING_AI_MODEL_EMBEDDING: openai
RAG_ENABLED: true
```

`SPRING_AI_MODEL_CHAT=openai`는 OpenAI 자연어 조건 분석을 활성화한다.

`SPRING_AI_MODEL_EMBEDDING=openai`는 OpenAI 임베딩 모델을 활성화한다.

`RAG_ENABLED=true`는 Qdrant VectorStore와 RAG 기능을 활성화한다.

API Key만 입력하고 위 값을 `none`, `false`로 두면 OpenAI와 RAG는 동작하지 않는다.

## IntelliJ Working Directory

`application.yml`은 외부 비밀 설정 파일을 다음 상대경로에서 읽는다.

```text
./config/application-secret.yml
```

IntelliJ 설정 절차:

1. Run
2. Edit Configurations
3. Spring Boot 실행 설정 선택
4. Working directory
5. 현재 `youthcenter-search` 프로젝트 루트 선택

Working directory가 다른 경로라면 `config/application-secret.yml`을 찾지 못할 수 있다. 파일이 없어도 서버 기동은 실패하지 않는다.

시작 로그 예:

```text
External secret configuration: FOUND
External secret configuration path: ./config/application-secret.yml
```

또는:

```text
External secret configuration: NOT FOUND
Run scripts/setup-local.ps1 or copy the example file.
```

## 실행 후 확인 순서

1. MySQL UP
2. Qdrant UP
3. 온통청년 API Key 설정됨
4. OpenAI API Key 설정됨
5. ChatModel 사용 가능
6. EmbeddingModel 사용 가능
7. RAG 활성
8. 온통청년 연결 진단
9. 전체 정책 수집
10. 전체 임베딩 처리

관리자 개발 화면:

```text
http://localhost:8080/dev
```

## 설정 오류 메시지

온통청년 API Key가 없을 때:

```text
온통청년 API Key가 설정되지 않았습니다.
config/application-secret.yml의 YOUTH_CENTER_API_KEY를 입력하세요.
```

OpenAI Chat이 비활성화됐을 때:

```text
OpenAI Chat Model이 비활성화되어 있습니다.
OPENAI_API_KEY와 SPRING_AI_MODEL_CHAT=openai 설정을 확인하세요.
```

OpenAI Embedding이 비활성화됐을 때:

```text
OpenAI Embedding Model이 비활성화되어 있습니다.
OPENAI_API_KEY와 SPRING_AI_MODEL_EMBEDDING=openai 설정을 확인하세요.
```

RAG가 비활성화됐을 때:

```text
RAG 기능이 비활성화되어 있습니다.
RAG_ENABLED=true 설정을 확인하세요.
```
