# CODE REVIEW GUIDE

## 전체 요청 흐름

사용자 검색 요청은 `/api/policies/search`로 들어오며, 검색 조건 분석, 후보 조회, 자격 필터, 랭킹, 진단 응답 순서로 처리된다. OpenAI 조건 분석은 검색당 최대 한 번만 수행하고, 실패 시 Java rule 기반 분석으로 대체한다.

## 정책 수집 흐름

온통청년 API 원문은 `PolicyPersistenceService`에서 `Policy`와 `PolicyCondition`으로 저장된다. 저장 후 `PolicySourceSnapshotService`가 원문 Snapshot을 보존하고, `PolicyApplicabilityClassificationService`가 같은 원문 필드로 신청 가능 지역을 판정한다. 수집 과정에서 `policy_region`을 직접 병합하지 않는다.

## 지역 분류 흐름

`StrictPolicyRegionMentionExtractor`는 지역 표현을 찾을 때 역할을 함께 부여한다.

- `RESIDENCE_ELIGIBILITY`, `SERVICE_ELIGIBILITY`: `policy_region` 저장 가능
- `WORKPLACE_LOCATION`, `ACTIVITY_LOCATION`, `INSTITUTION_LOCATION`, `REFERENCE_ONLY`, `UNKNOWN`: Evidence에만 저장

`PolicyGeographyClassifier`는 Snapshot 원문에서 신청 자격 지역, 전국 표현, 기관 근거를 종합한다. `zipCd`는 아직 공식 검증된 신청 지역 매핑이 아니므로 Evidence에만 남기고 `policy_region` 생성 근거로 쓰지 않는다.

## Search Projection 흐름

`PolicySearchProjectionService`는 정책명, 키워드, 카테고리, 설명, 지원 내용, 대상, 자격, 기관 텍스트를 검색용 projection으로 만든다. BM25 lexical index는 이 projection을 메모리에 올려 필드별 가중치를 적용한다.

## Embedding 흐름

정책 원문이나 분류 결과가 바뀌어 검색 문서가 달라질 수 있는 경우에만 `PolicyEmbeddingSync`를 `PENDING`으로 전환한다. 전체 Qdrant collection 삭제나 무조건 전체 재임베딩은 금지한다.

## 자연어 검색 흐름

`PolicySearchPlanService`가 검색당 한 번만 `PolicySearchPlan`을 만든다. 이 계획에는 원문, 정규화된 긍정 목적, 원하는 domain/support intent, 제외 domain/support intent, 구조화 조건, 분석 모드가 들어 있다. 이후 Vector query, BM25 query, 자격 필터, 선호 필터, ranking, diagnostics는 원문을 다시 해석하지 않고 이 계획만 참조한다.

## 후보 수집

명시 지역이 있으면 `RegionEligiblePolicyCandidateService`가 정확 시군, 상위 시도, 전국, 허용 복수 지역 후보 pool을 만든다. BM25 후보는 `PolicyLexicalIndex`가 projection 기반 필드 가중치와 한국어 n-gram으로 반환한다. Qdrant 후보는 명시적 제외가 없는 경우 원문과 intent query를 사용하고, 제외가 있으면 원문 vector를 쓰지 않고 normalized goal과 긍정 intent query만 사용한다.

## Eligibility Filter

지역, 나이, 학생 여부, 명시 취업 상태, 신청 마감은 hard filter다. 무직 같은 사용자 상태는 취업 정책 선호가 아니므로 desired/excluded domain에 자동 반영하지 않는다.

## Preference Filter

`SearchDomainIntentPolicy`가 `PolicyDomainClassification`의 primary domain, secondary domain, support intents를 함께 검사한다. EMPLOYMENT 제외 검색에서는 primary가 EDUCATION이라도 `EMPLOYMENT_SUPPORT`가 붙은 취업 목적 교육 정책을 제거한다. 현금성 지원은 여러 분야에 걸치므로 FINANCE 제외만으로 자동 제거하지 않는다.

## Hybrid Ranking

제외와 자격 불일치는 감점하지 않고 filter에서 제거한다. 남은 후보만 제목 정확도, BM25 순위/점수, Vector 점수, 원하는 domain/support intent 일치, 지역 적합성으로 정렬한다. 첫 페이지 지역 보정이 적용되면 그 순서를 전체 결과 순서로 확정한 뒤 pagination해 같은 policyId가 다음 페이지에 중복되지 않게 한다.

## 실제 Trace

검색 응답과 Explain은 후보 출처, Vector/BM25 점수, domain/support intent, hard filter 통과 여부, 최종 순위를 확인할 수 있어야 한다. 검색 종료 뒤 별도 contains 점수를 재추정하지 않는다.

## 주요 클래스별 리뷰 포인트

- `PolicyApplicabilityClassificationService`: 지역 분류 단일 진입점이다. 수집과 재분류가 이 서비스를 우회하면 안 된다.
- `PolicyGeographyClassifier`: 신청 지역과 단순 위치 언급을 분리한다. 실제 정책명 예외를 넣지 않는다.
- `StrictPolicyRegionMentionExtractor`: 지역명 주변 문맥으로 role을 판정한다. 실제 지역명 하드코딩 대신 행정구역 catalog를 사용한다.
- `PolicySearchPlanService`: 검색 계획 단일 진입점이다. parser, rule, classifier 결과를 중복 저장하지 않는다.
- `SearchDomainIntentPolicy`: 제외 분야와 support intent hard filter의 기준이다. primaryDomain만 확인하는 코드를 다시 만들지 않는다.
- `PolicyLexicalIndex`: contains 합산이 아니라 projection 기반 BM25를 사용한다. 일반어는 DF가 커져 IDF가 낮아지고, 한국어 복합어는 2~4글자 n-gram과 개념어로 보완한다.
- `PolicyDomainClassifier`: domain과 support intent를 함께 판정한다. 지원금/수당 단어만으로 금융으로 단정하지 않고, 취업 목적 교육은 `EMPLOYMENT_SUPPORT`를 부여한다.
- `PolicyRagSearchService`: SearchPlan 생성, 후보 병합, filter, ranking, diagnostics를 조율한다. 새 기능 추가 시 원문 contains 조건을 늘리지 말고 계획/분류/필터 모듈을 수정한다.

## DB 테이블 관계

- `policy`: 정책 기본 정보
- `policy_condition`: 나이, 취업 상태, 학생 상태 등 구조화 조건
- `policy_source_snapshot`: 온통청년 원문 보존
- `policy_region`: 신청 가능 지역만 저장
- `policy_region_classification`: 지역 판정 scope, confidence, evidence JSON, classifier version
- `policy_search_projection`: BM25와 검색 텍스트 projection
- `policy_embedding_sync`: Qdrant embedding 동기화 상태

## 디버깅 시 확인 순서

1. `policy_source_snapshot`에 원문 필드가 있는지 확인한다.
2. `policy_region_classification.evidence_json`에서 지역 role과 confidence를 확인한다.
3. `policy_region`이 신청 가능 지역만 갖는지 확인한다.
4. `/api/admin/search-index/status`로 BM25 index 문서 수를 확인한다.
5. `/api/policies/search` diagnostics에서 normalized goal, desired/excluded domain, support intent, vector source, excludedDomainFilteredCount를 확인한다.
6. 결과 카드의 primary domain, secondary domain, support intents, domain evidence를 확인한다.
7. `/api/admin/search/explain`으로 특정 정책의 후보 포함/제외 단계를 확인한다.

## 수정하면 안 되는 핵심 불변 조건

- Snapshot 원문 없이 기존 `policy_region`만 보고 재분류하지 않는다.
- `zipCd`를 SGIS 코드와 숫자가 같다는 이유로 신청 지역에 연결하지 않는다.
- 기업 소재지, 면접 장소, 교육 장소, 기관 소재지를 신청자 거주 지역으로 저장하지 않는다.
- 실제 정책명 예외를 production ranking이나 지역 판정에 넣지 않는다.
- 취업 상태와 취업 정책 선호를 같은 값으로 합치지 않는다.
- excluded domain은 primary domain만이 아니라 secondary domain과 support intent까지 검사한다.
- 구형 title/summary contains 점수와 BM25 점수를 혼용하지 않는다.
- 전체 정책 재수집, Qdrant collection 삭제, Docker volume 삭제를 검색 코드 검증 수단으로 사용하지 않는다.
