# RAG Search Flow

사용자 검색은 저장된 정책 데이터만 사용한다.

1. 사용자가 자연어 질의 입력
2. OpenAI ChatModel로 조건 추출
3. OpenAI 실패 또는 키 없음이면 RuleBased fallback
4. Query Embedding 생성
5. Qdrant 후보 검색
6. MySQL에서 실제 정책 로드
7. Hard Filter 적용
8. 부족하면 retryTopK로 한 번 확장
9. 그래도 부족하면 MySQL fallback 검색
10. Hybrid Ranking
11. 검색된 정책만 근거로 답변 생성

## Hard Filter

- `active=false` 제외
- 명확한 지역 불일치 제외
- 명확한 나이 불일치 제외
- 명확한 취업/학생 상태 불일치 제외
- 신청 마감 정책 제외

지역 UNKNOWN은 기본 검색 결과에서 제외한다. 필요하면 별도 확인 필요 정책 영역으로 확장할 수 있다.

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
