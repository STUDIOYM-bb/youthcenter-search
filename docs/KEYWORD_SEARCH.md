# 키워드 검색

## 검색 모드

- `KEYWORD`: 지역, 나이, 취업 상태 같은 명시 조건 없이 정책명이나 주제를 검색한다.
- `CONDITION`: 지역, 나이, 취업 상태 등 자격 조건 중심으로 검색한다.
- `HYBRID`: 정책 키워드와 자격 조건을 함께 사용한다.

예:

- `청년 면접 수당` -> `KEYWORD`
- `경기도 청년 면접 수당` -> `HYBRID`
- `수원 사는 27살 무직 청년 지원금` -> `HYBRID`

## 명시 조건 판별

값이 추론됐다는 이유만으로 필터에 사용하지 않는다.

- 지역: 원문에 행정지역명이나 약칭이 있을 때만 적용
- 나이: `27살`, `만 27세`처럼 숫자와 나이 표현이 있을 때만 적용
- 취업 상태: `무직`, `미취업`, `취준생`, `취업 준비`, `구직자`, `직장인` 등이 있을 때만 적용
- 학생 상태: 사용자가 학생임을 직접 표현할 때만 적용

`청년 면접 수당`은 나이와 취업 상태를 명시한 문장이 아니므로 나이/취업 필터를 적용하지 않는다.

## 키워드 동의어

동의어는 `src/main/resources/policy-keyword-synonyms.yml`에 둔다.

- `면접수당`: 면접 수당, 면접수당, 면접비, 면접 비용, 면접 지원금, 구직 면접비
- `월세`: 월세, 임차료, 주거비, 월 임대료
- `지원금`: 지원금, 수당, 보조금, 장려금
- `자산형성`: 자산형성, 목돈, 저축 지원, 매칭 저축

## 후보 병합

검색 후보는 두 경로에서 수집한다.

1. Qdrant semantic search
2. MySQL lexical search

두 후보를 `policyId` 기준으로 병합한 뒤 중복을 제거한다. MySQL lexical search는 fallback이 아니라 항상 실행되는 hybrid 검색 구성 요소다.

## 필터 원칙

지역, 나이, 취업 상태는 사용자가 명시했을 때만 hard filter로 적용한다.

- 지역 미입력: 모든 지역, 전국, 지역 미확인 정책이 후보로 남을 수 있다.
- 지역 입력: 기존 지역 hard filter를 적용한다.
- 나이 미입력: 나이 조건으로 제거하지 않는다.
- 취업 상태 미입력: 취업 상태로 제거하지 않는다.

## 가중치

`KEYWORD`:

- lexical 45
- semantic 35
- title exact 15
- application 5

`CONDITION`:

- semantic 30
- region 25
- age 15
- employment 10
- student 5
- support type 10
- application 5

`HYBRID`:

- semantic 25
- lexical 20
- title exact 15
- region 20
- age 5
- employment 5
- support type 5
- application 5

입력되지 않은 조건의 가중치는 제외하고 남은 가중치로 정규화한다.
