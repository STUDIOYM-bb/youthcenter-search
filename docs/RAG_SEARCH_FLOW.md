# RAG Search Flow

## Query semantics and exclusions

Natural language search now separates two concepts that must not be merged:

- user state, such as `EMPLOYED`, `UNEMPLOYED`, or unknown
- policy domains the user wants or explicitly excludes

`PolicyQuerySemantics` carries the positive-only goal, desired domains, excluded
domains, positive keywords, excluded keywords, and whether the query contains an
explicit exclusion. The OpenAI parser may return these fields, but Java rule
validation always re-checks explicit exclusions through
`PolicyIntentPolarityDetector`.

When an explicit exclusion is present, the search pipeline avoids using the raw
query as the strong vector query because negated words such as `취업 생각은 없어`
can still be close to employment policies in embedding space. The pipeline uses
`VECTOR_NORMALIZED_QUERY` from `normalizedGoal` and builds
`VECTOR_INTENT_QUERY` only from desired domains and positive keywords.

Lexical search receives polarity-filtered intent terms. Excluded keywords and
synonyms of excluded domains are not used for lexical candidate boosts.

After vector, lexical, and region-eligible candidates are merged and condition
filters are applied, `PolicyDomainClassifier` classifies each policy's primary
domain from the policy category plus policy title/support/summary purpose.
Policies whose primary domain is explicitly excluded are removed before final
ranking. Secondary mentions, such as employment wording only in eligibility
conditions, do not make a non-employment policy an employment-primary policy.

This change only modifies query analysis, ranking, and filtering. Existing
policy rows, search projections, embedding documents, Qdrant metadata, and
Qdrant collections do not need to be rebuilt.

사용자 검색은 저장된 정책 데이터만 사용한다.

1. 사용자가 자연어 질의 입력
2. OpenAI ChatModel로 조건 추출
3. Java 원문 검증으로 명시 조건만 유지
4. `KEYWORD`, `CONDITION`, `HYBRID` 검색 모드 판정
5. 정책 키워드와 동의어 추출
6. 조건어와 정책 의도를 분리한 `PolicySearchIntent` 생성
7. 원문, 정책 의도, 확장 의도, 카테고리 Query로 Qdrant 후보 검색
8. MySQL 제목/키워드/요약/분야 후보 검색
9. `policyId` 기준 후보 병합 및 중복 제거
10. 사용자가 명시한 조건만 Hard Filter 적용
11. Topic Relevance Threshold 적용
12. 검색 모드별 Dynamic Ranking
13. 검색된 정책만 근거로 답변 생성

## Query Rewrite

사용자 문장 안의 지역, 나이, 거주 표현은 구조화 조건으로 사용하고 semantic query의 중심에서 제외한다. 예를 들어 `수원 사는 27살 취준생 정책`은 다음처럼 나뉜다.

- 조건어: `수원`, `27살`, `취준생`
- 정책 의도: `청년`, `취업 지원`, `구직 지원`, `취업 준비`
- Semantic Query: `청년 취업 준비 및 구직 활동을 지원하는 정책`
- Lexical Query: `청년 취업 구직 면접 면접수당 구직활동 취업역량 ...`

원문 Query도 후보 검색 경로로 유지한다.

## Hard Filter

- `active=false` 제외
- 사용자가 지역을 명시한 경우에만 명확한 지역 불일치 제외
- 사용자가 나이를 명시한 경우에만 명확한 나이 불일치 제외
- 사용자가 취업/학생 상태를 명시한 경우에만 명확한 취업/학생 상태 불일치 제외
- 신청 마감 정책 제외

조건 판정은 `MATCH`, `UNKNOWN`, `MISMATCH` 세 단계다. 정책 데이터가 비어 있어 `UNKNOWN`인 경우에는 후보를 제거하지 않고 확인 필요로 표시한다. 명확하게 반대 조건인 `MISMATCH`만 제거한다.

지역은 점수만 낮추지 않고 Hard Filter로 제거한다. 하위 시·군·자치구를 입력한 사용자는 해당 시·군·자치구 정책, 상위 시·도 전체 정책, 전국 정책, 복수 지역 중 사용자 지역 또는 상위 시·도를 포함한 정책만 통과한다. 같은 시·도의 형제 시·군·자치구 전용 정책과 다른 시·도 정책은 제거된다.

시·도 전체를 입력한 경우에는 해당 시·도 전체 정책, 전국 정책, 복수 지역 중 해당 시·도 전체를 포함한 정책만 통과한다. 사용자가 하위 시·군·자치구를 말하지 않았으므로 해당 시·도 산하 특정 시·군·자치구 전용 정책은 자동 포함하지 않는다.

`전국`을 명시한 검색은 지역 미입력 검색과 다르다. 전국 명시 검색은 명시적 전국 정책만 통과하고, 지역 미입력 검색은 지역 Hard Filter를 적용하지 않는다.

지역을 입력하지 않은 키워드 검색에서는 지역 Hard Filter를 적용하지 않는다. `청년 면접 수당`은 서울, 경기, 부산, 서산 등 모든 지역 후보를 키워드와 의미 관련도로 비교한다.

## Hybrid Ranking

최종 점수는 의미 유사도만 사용하지 않는다.

- 의미 유사도
- 지역 적합도
- 나이 적합도
- 취업/학생 조건
- 지원 형태
- 제목/키워드 일치
- 신청 상태

점수는 검색 관련도이며 신청 가능성을 확정하지 않는다.

`KEYWORD` 모드는 lexical/title 점수를 높이고 지역 점수를 사용하지 않는다. `HYBRID` 모드는 semantic, lexical, title, region, 조건 점수를 함께 사용한다. 입력되지 않은 조건의 가중치는 제외하고 남은 가중치로 정규화한다.

지역 점수는 행정구역 거리 점수가 아니라 신청 지역 자격 충족 여부다. 정확한 시·군·자치구, 상위 시·도 전체, 전국, 복수 지역 일치 정책은 모두 지역 점수 100을 받는다. 호환되는 정책끼리의 최종 순서는 의미 유사도, 키워드, 제목 정확도, 나이·취업·학생·지원 형태, 신청 상태 점수로 결정한다.

## Topic Relevance

지역 자격을 만족해도 정책 주제가 검색 의도와 무관하면 최종 결과에서 제외할 수 있다. `RAG_MINIMUM_TOPIC_RELEVANCE` 기본값은 `0.25`이며, 제목 정확 일치, 강한 lexical 일치, 높은 semantic 일치, category 일치 중 하나가 충분하면 통과한다.

이 변경은 검색 Query와 Ranking 로직만 바꾸므로 Embedding Document와 Qdrant Point ID를 변경하지 않는다. 기존 임베딩을 전체 재생성할 필요는 없다.

## Explain API

관리자 API `POST /api/admin/search/explain`은 특정 정책이 검색 결과에 포함되지 않은 이유를 반환한다.

요청 예:

```json
{
  "query": "수원 사는 27살 취준생 정책",
  "policyId": 123
}
```

응답에는 후보 검색 경로, 지역/나이/취업/학생 판정, Topic 점수, 최종 포함 여부가 포함된다.
