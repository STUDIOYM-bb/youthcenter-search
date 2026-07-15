# CODE REVIEW GUIDE

## 1. 검색 요청 진입점

- 대표 클래스: `PolicySearchController`, `PolicyRagSearchService`
- 입력: `/api/policies/search`의 자연어 query, page, size
- 출력: 기존 `PolicySearchResponse` JSON 구조
- DB 사용: 정책 후보 로딩 시 `PolicyRepository`
- 외부 API 사용: 검색 시점에는 OpenAI 조건 분석과 Qdrant만 사용하며 온통청년 API는 호출하지 않는다.
- 주의사항: 컨트롤러나 Orchestrator에 문자열 contains 규칙을 추가하지 않는다. 검색 규칙은 Plan, Candidate, Evaluator, Ranking 전용 컴포넌트에서 수정한다.

## 2. SearchPlan 생성

- 대표 클래스: `PolicySearchPlanService`
- 입력: 사용자 원문 query, resultSize
- 출력: `PolicySearchPlan`, parser mode, fallback 정보
- DB 사용: 없음
- 외부 API 사용: `CompositePolicySearchConditionParser`를 통해 OpenAI를 최대 한 번 사용할 수 있다.
- 확인 값: `normalizedGoal`, `desiredDomains`, `excludedDomains`, `desiredSupportIntents`, `excludedSupportIntents`, `condition`
- 주의사항: 사용자 취업 상태와 취업 정책 선호를 합치지 않는다. 교육 단계도 Boolean `studentStatus`만으로 단정하지 않는다.

## 3. Candidate 수집

- 대표 클래스: `PolicySearchCandidateRetriever`
- 입력: `PolicySearchPlan`, `PolicySearchIntent`, 사용자 지역, page/size
- 출력: `PolicyCandidateCollection`, `CandidateEvidence`, `CandidateCollectionMetrics`
- DB 사용: `PolicyRepository`, `RegionEligiblePolicyCandidateService`
- 외부 API 사용: Qdrant `VectorStore`
- 실패 시 확인 값: vector 후보 수, lexical 후보 수, region pool 수, fallbackReason
- 주의사항: 명시적 제외가 있는 query는 원문 vector를 강하게 사용하지 않는다. 후보 source rank와 score는 최종 rank와 다르므로 `CandidateSourceEvidence`에 별도로 보존한다.

## 4. Eligibility 평가

- 대표 클래스: 현재 `PolicyRagSearchService` 내부 score/pass, `PolicyTargetEligibilityFilter`, `PolicyEmploymentAudienceClassifier`
- 입력: 정책, 검색 조건, 대상 분류 결과
- 출력: match/unknown/mismatch reason과 hard filter 여부
- DB 사용: 정책 relation 로딩 결과 사용
- 외부 API 사용: 없음
- 확인 값: regionCompatibility, ageMatchStatus, employmentMatchStatus, targetStageMatchStatus
- 주의사항: 지역, 나이, 명시 취업 상태, 교육 단계 불일치는 감점이 아니라 hard filter다. `UNKNOWN`은 데이터 부족이므로 명확한 불일치가 아니면 제거하지 않는다.

## 5. Domain/SupportIntent 필터

- 대표 클래스: `PolicyDomainClassifier`, `SearchDomainIntentPolicy`
- 입력: 정책 category/title/support/target/qualification/projection 텍스트, `PolicySearchPlan`
- 출력: primary domain, secondary domains, support intents, excludedDomainPassed
- DB 사용: projection 또는 로딩된 정책 텍스트
- 외부 API 사용: 없음
- 확인 값: `primaryDomain`, `secondaryDomains`, `supportIntents`, `excludedDomainFiltered`
- 주의사항: excluded domain은 primary만 보지 않는다. `EMPLOYMENT_SUPPORT`가 붙은 취업 목적 교육 정책은 primary가 EDUCATION이어도 취업 제외 검색에서 제거된다.

## 6. Ranking

- 대표 클래스: 현재 `PolicyRagSearchService` 내부 ranking 메서드
- 입력: semanticScore, lexicalScore, titleExactScore, topic relevance, recommendation tier
- 출력: finalScore와 정렬된 결과
- DB 사용: 없음
- 외부 API 사용: 없음
- 확인 값: title source, topicScore, finalScore, recommendationTier
- 주의사항: `POLICY_NAME` 검색은 제목 정확도를 먼저 본다. broad/eligibility 검색은 `PRIMARY`를 `NEEDS_CONFIRMATION`보다 앞에 둔다.

## 7. Result 조립

- 대표 클래스: 현재 `PolicyRagSearchService` 내부 result item 조립
- 입력: Policy, domain classification, condition match, recommendation
- 출력: 기존 `PolicySearchResultItem` JSON 필드
- DB 사용: 없음
- 외부 API 사용: 없음
- 주의사항: API 필드명은 프론트와 연결되어 있으므로 변경하지 않는다. 긴 record 생성자는 단계적으로 assembler로 분리해야 한다.

## 8. Diagnostics

- 대표 클래스: `PolicySearchDiagnosticsFactory`
- 입력: plan, intent, candidate metrics, filter metrics, selection metrics, elapsed time
- 출력: 기존 `PolicySearchDiagnostics`
- DB 사용: 없음
- 외부 API 사용: 없음
- 확인 값: vector/mysql 후보 수, hard filter count, excludedDomainFiltered, userEmploymentStatus, educationStage
- 주의사항: Diagnostics는 검색 후 재추론하지 않고 실행 중 실제로 쓴 metrics만 조립한다.

## 9. Explain

- 대표 클래스: `PolicyRagSearchService.explain`
- 입력: query, policyId 또는 sourcePolicyId
- 출력: 후보 source, 조건 판정, topic/final score, disposition
- DB 사용: `PolicyRepository`
- 외부 API 사용: 내부적으로 동일 검색을 수행하므로 검색과 같은 외부 의존성을 가진다.
- 주의사항: 최종 rank를 source rank처럼 표시하면 안 된다. 후보 수집 단계의 `CandidateSourceEvidence`를 Explain에 계속 연결해야 한다.

## 10. 관리자 Job

- 대표 클래스: `AdminController`, `AdminJobService`
- 입력: `/api/admin/jobs/**`
- 출력: jobId, 진행률, 최근 작업 상태
- DB 사용: 작업 종류에 따라 정책, embedding, region 테이블 사용
- 외부 API 사용: 수집 job은 온통청년 API, embedding job은 Qdrant/OpenAI embedding 사용 가능
- 주의사항: URL은 유지해야 한다. 컨트롤러는 아직 분리 대상이며 비즈니스 로직은 service로 이동해야 한다.

## 11. 정책 수집

- 대표 클래스: `YouthCenterPolicyCollectionService`, `YouthCenterApiClient`, `YouthCenterResponseParser`
- 입력: 온통청년 목록/상세 API 응답
- 출력: `Policy`, `PolicyCondition`, `PolicySourceSnapshot`
- DB 사용: 정책 원본과 snapshot 저장
- 외부 API 사용: 온통청년 API
- 주의사항: API URL, 파라미터, 페이지네이션 규칙을 검색 리팩터링 중 변경하지 않는다.

## 12. Projection

- 대표 클래스: `PolicySearchProjectionService`
- 입력: 정책 및 snapshot 원문 필드
- 출력: `policy_search_projection`
- DB 사용: projection upsert
- 외부 API 사용: 없음
- 주의사항: projection format 변경은 BM25와 embedding 입력에 영향을 줄 수 있으므로 별도 마이그레이션/재색인 계획 없이 바꾸지 않는다.

## 13. Embedding

- 대표 클래스: `PolicyEmbeddingService`
- 입력: `PENDING` 상태 embedding sync rows
- 출력: Qdrant point와 `policy_embedding_sync` 상태
- DB 사용: embedding sync 상태 전환
- 외부 API 사용: embedding model, Qdrant
- 주의사항: 검색 코드 검증을 이유로 전체 재임베딩이나 Qdrant collection 삭제를 하지 않는다.

## 14. 지역 동기화

- 대표 클래스: `RegionCatalog`, `RegionSynchronizationService`, `PolicyApplicabilityClassificationService`
- 입력: SGIS 행정구역, snapshot 원문
- 출력: `region_code`, `policy_region`, `policy_region_classification`
- DB 사용: 지역 카탈로그와 정책 지역 저장
- 외부 API 사용: SGIS 동기화 실행 시에만 사용
- 주의사항: 기업 소재지, 면접 장소, 기관 소재지를 신청자 거주 지역으로 저장하지 않는다. `zipCd`는 검증 전까지 신청 지역 근거로 쓰지 않는다.

## 수정하면 안 되는 핵심 불변 조건

- 실제 정책명 예외를 production 검색/지역 판정에 넣지 않는다.
- 취업 상태와 취업 정책 선호를 같은 값으로 합치지 않는다.
- excluded domain은 primary, secondary, support intent를 함께 검사한다.
- 지역/나이 같은 명확한 자격 불일치는 감점하지 않고 제거한다.
- 기존 REST API 경로와 JSON 필드명은 유지한다.
- 전체 정책 재수집, projection 전체 재생성, Qdrant 전체 재임베딩, Docker volume 삭제는 리팩터링 검증 수단이 아니다.
