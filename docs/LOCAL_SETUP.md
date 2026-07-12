# Local Setup

## 1. Secret 파일

```powershell
Copy-Item config/application-secret.example.yml config/application-secret.yml
```

`config/application-secret.yml`에 실제 값을 입력한다.

- `YOUTH_CENTER_API_KEY`
- `OPENAI_API_KEY`
- MySQL 접속 정보
- Qdrant 접속 정보
- `ADMIN_API_KEY`

이 파일은 Git에 포함하지 않는다.

## 2. 인프라 실행

```powershell
docker compose up -d
```

기존 MySQL 또는 Qdrant가 3306, 6333, 6334 포트를 사용하면 compose 서비스가 뜨지 않는다. 기존 컨테이너를 사용할 경우 `config/application-secret.yml`의 접속 정보를 맞춘다.

## 3. 애플리케이션 실행

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootRun
```

접속:

- 사용자 화면: `http://localhost:8080`
- 관리자 화면: `http://localhost:8080/dev`

## 4. 운영 순서

1. 관리자 상태 확인
2. 온통청년 연결 진단
3. 전체 정책 수집
4. 전체 임베딩 대기열 등록
5. PENDING 임베딩 처리
6. 사용자 검색 실행
