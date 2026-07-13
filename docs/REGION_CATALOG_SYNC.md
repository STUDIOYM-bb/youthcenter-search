# SGIS 행정지역 카탈로그 동기화

지역 카탈로그는 로컬 `region_code` 테이블을 기준으로 동작한다. 사용자 검색 중에는 SGIS API를 호출하지 않는다.

`region_code.region_code`는 내부 표준 지역 키다. SGIS `cd`나 온통청년 `zipCd` 같은 외부 코드는 `region_external_code`에 별도로 저장한다.

## 설정

`config/application-secret.yml`에 SGIS 키를 입력한다.

```yaml
SGIS_CONSUMER_KEY: ""
SGIS_CONSUMER_SECRET: ""
SGIS_REGION_SYNC_ENABLED: true
SGIS_REGION_SYNC_ON_STARTUP: false
```

실제 키는 example, README, Java, JavaScript, Git 추적 파일에 넣지 않는다.

## 동기화 범위

동기화 범위는 시·도와 시·군·자치구·군까지만이다. 읍·면·동 API는 호출하지 않고, 읍·면·동은 검색 단위로 저장하지 않는다.

저장 매핑:

- 전국: `region_code=KR`, `province=전국`, `region_level=NATIONWIDE`
- 시·도: `region_code=P:{시도명}`, `province=addr_name`, `city=null`, `region_level=PROVINCE`
- 시·군·자치구: `region_code=M:{시도명}:{시군자치구명}`, `province=부모 시도명`, `city=시군자치구명`, `region_level=CITY`
- SGIS 코드: `region_external_code(code_system=SGIS, external_code=SGIS cd)`
- 온통청년 zipCd: 매핑 근거가 있을 때만 `region_external_code(code_system=YOUTH_CENTER_ZIP, external_code=zipCd)`
- 도 단위 하위 시·군: `region_level=CITY`
- 특별시·광역시 하위 자치구·군: `region_level=CITY`
- 일반시 하위 일반구: 상위 시로 통합

SGIS 코드와 온통청년 zipCd는 같은 숫자여도 같은 코드 체계로 간주하지 않는다.

SGIS에서 사라진 기존 지역은 자동 삭제하지 않는다. 정책 데이터와 `policy_region` 관계도 동기화만으로 삭제하지 않는다.

## 관리자 실행 순서

1. `/dev`에서 관리자 키 입력
2. `전국 행정지역 동기화` 실행
3. 지역 전체 수, 시·도 수, 시·군·자치구 수, 외부 코드 수 확인
4. `지역 검색 진단`에서 `칠곡`, `경북 칠곡`, `경기도 광주`, `광주` 확인
5. 기존 정책에 반영하려면 `전체 정책 지역 다시 계산` 실행
6. 변경 정책이 PENDING으로 등록되면 `전체 PENDING 임베딩 처리` 실행

## 짧은 지역명과 모호성

DB의 정식 지역명에서 짧은 별칭을 생성한다.

- 칠곡군 → 칠곡
- 횡성군 → 횡성
- 수원시 → 수원
- 남동구 → 남동

짧은 이름이 둘 이상 지역에 매칭되면 자동 확정하지 않는다.

- `광주` → `AMBIGUOUS`
- `광주광역시` → 광주광역시
- `경기도 광주` → 경기도 광주시

## 장애 처리

SGIS 인증 실패, HTTP 오류, SGIS `errCd` 오류, 빈 응답, JSON 오류, timeout은 동기화 Job 실패 또는 부분 실패로 기록된다. 특정 시·도 조회 실패는 다른 시·도 동기화를 막지 않는다.

검색은 로컬 DB 카탈로그만 사용하므로 SGIS 장애가 사용자 정책 검색 장애로 이어지지 않는다.

## 운영 확인 API

- `GET /api/admin/regions/resolve?q=해남`
- `GET /api/admin/regions/search?name=해남`
- `GET /api/admin/regions/coverage`
- `GET /api/admin/regions/sync-runs/latest`
- `POST /api/admin/regions/cache/refresh`

일부 시·도 실패가 있으면 동기화 이력은 `COMPLETED_WITH_ERRORS`로 저장된다. 모든 시·도 처리가 실패하면 `FAILED`로 저장된다.
