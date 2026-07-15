# RAG Search Flow

## 실행 순서

1. `/api/policies/search` 요청이 `PolicyRagSearchService`로 들어온다.
2. `PolicySearchPlanService`가 OpenAI/Rule 결과를 합쳐 `PolicySearchPlan`을 만든다.
3. `PolicySearchIntentBuilder`가 vector/lexical 질의에 사용할 긍정 의도 문장을 만든다.
4. `PolicySearchCandidateRetriever`가 Qdrant, BM25 lexical, 지역 적격 pool 후보를 수집하고 policyId 기준으로 병합한다.
5. 정책 relation을 로딩한 뒤 지역, 나이, 취업 상태, 학생 Boolean, 교육 단계, 신청 상태를 hard filter로 평가한다.
6. `PolicyDomainClassifier`와 `SearchDomainIntentPolicy`가 제외 domain/support intent를 적용한다.
7. 제목 정확도, semantic score, lexical score, topic relevance, recommendation tier로 정렬한다.
8. `RegionCoverageResultSelector`가 첫 페이지 지역 보장을 적용하고, 그 순서를 전체 결과 순서로 확정한 뒤 pagination한다.
9. `PolicySearchDiagnosticsFactory`가 실행 중 수집된 metrics를 기존 Diagnostics JSON으로 조립한다.

## SearchPlan

`PolicySearchPlan`은 검색 요청당 한 번만 생성된다.

- `originalQuery`: 사용자 원문
- `normalizedGoal`: 부정 표현을 제거한 긍정 목적
- `desiredDomains`, `excludedDomains`
- `desiredSupportIntents`, `excludedSupportIntents`
- `positiveTerms`, `excludedTerms`
- `condition`: 지역, 나이, 취업 상태, 학생 여부 등 구조화 조건
- `userEducationStages`, `educationStageExplicit`

취업 상태(`EMPLOYED`, `UNEMPLOYED`)와 취업 정책 선호(`EMPLOYMENT_SUPPORT` 원함/제외)는 서로 다른 값이다.

## Candidate 수집

후보 수집은 `PolicySearchCandidateRetriever`가 담당한다.

- Qdrant 원문/정규화/의도 query 후보
- `PolicyLexicalIndex` 기반 BM25 후보
- 지역이 명시된 경우 정확 시군, 상위 시도, 전국, 허용 복수 지역 후보 pool
- Broad/Eligibility 검색에서 지역 pool 또는 active policy fallback 후보

명시적 제외가 있으면 원문 vector query를 강하게 쓰지 않는다. 예를 들어 `취업 생각은 없어`의 원문 embedding은 취업 정책과 가까울 수 있으므로 `normalizedGoal`과 긍정 intent query를 사용한다.

각 후보는 `CandidateEvidence`와 `CandidateSourceEvidence`를 가진다. 최종 순위와 source별 rank는 다르므로 별도로 보존한다.

## Hard Filter

명확한 자격 불일치는 감점하지 않고 제거한다.

- 지역 불일치
- 나이 불일치
- 명시 취업 상태 불일치
- 정책 취업 대상 불일치
- 학생 Boolean 불일치
- 교육 단계 전용 정책 불일치
- 신청 마감

정책 정보가 부족한 `UNKNOWN`은 기본적으로 제거하지 않는다. 데이터 부족 정책을 모두 제거하면 실제 신청 가능한 정책까지 누락될 수 있기 때문이다.

## Preference Filter

사용자가 제외한 domain/support intent는 `SearchDomainIntentPolicy`가 검사한다.

EMPLOYMENT 제외 검색에서는 다음 정책을 제거한다.

- primaryDomain = EMPLOYMENT
- secondaryDomains contains EMPLOYMENT
- supportIntents contains EMPLOYMENT_SUPPORT

단순 자격 문구에 “취업 시 지원 종료”가 있다는 이유만으로 취업 정책으로 분류하지 않는다.

## Ranking

정렬은 기존 점수 공식을 유지한다.

- `POLICY_NAME`: Exact title 우선
- `BROAD_DISCOVERY`, `ELIGIBILITY_SEARCH`: `PRIMARY` tier 우선, 이후 기존 finalScore
- 같은 tier 내부에서는 finalScore, 지역 동률 보정, policyId 순서를 사용한다.

지원 형태 가중치는 실제 support 점수 없이 분모에만 들어가면 점수를 낮출 수 있으므로 현재 공식에서는 사용하지 않는다.

## Diagnostics

`PolicySearchDiagnosticsFactory`는 다음 값을 재계산하지 않고 실행 중 수집된 metrics에서 조립한다.

- vector/mysql 후보 수
- 중복 후보 수
- 지역 pool 수
- hard filter count
- excluded domain count
- 사용자 취업 상태와 evidence
- 사용자 교육 단계
- recommendation tier count
- semantic conflict 여부

기존 JSON 필드명은 프론트와 호환을 위해 유지한다.

## Explain

Explain은 현재 검색을 동일하게 실행한 뒤 특정 정책이 결과에 포함됐는지 확인한다.

- 포함된 경우 결과 item의 실제 score와 condition match를 반환한다.
- 제외된 경우 지역/나이/취업/학생/교육 단계 기준으로 disposition을 계산한다.
- 후보 source별 rank/score는 `CandidateSourceEvidence`를 계속 연결해야 하며, 최종 rank를 vector rank처럼 표시하면 안 된다.

## 재색인·재임베딩

이 흐름은 query 분석, 후보 수집, hard filter, ranking만 다룬다. 정책 원문, projection format, embedding document, Qdrant point id를 바꾸지 않는 한 전체 재수집이나 전체 재임베딩은 필요하지 않다.
