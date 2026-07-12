# ERD Implementation Checklist

첨부 `final_ERD.png`의 테이블을 Flyway V1에 포함했다. 현재 서비스가 직접 사용하는 정책 관련 테이블만 Entity/Repository를 구현했고, 금융/소비 테이블은 통합 ERD 호환을 위해 스키마만 생성한다.

| ERD 테이블 | Flyway 생성 | Entity 구현 | PK | FK | UNIQUE | 비고 |
|---|---:|---:|---|---|---|---|
| member | 예 | 예 | id | 없음 | email | 정책 사용자 기준 |
| goal | 예 | 아니오 | id | member | 없음 | ERD 호환 |
| region_code | 예 | 예 | id | parent_id | region_code | 정책 지역 정규화 |
| policy | 예 | 예 | id | 없음 | source_policy_id | 온통청년 정책 |
| policy_condition | 예 | 예 | id | policy_id | policy_id | 조건 1:1 |
| policy_region | 예 | 예 | id | policy_id, region_id | policy_id, region_id | 지역 N:M |
| user_policy_profile | 예 | 아니오 | id | member, region_code | member_id | ERD 호환 |
| policy_bookmark | 예 | 아니오 | id | member, policy | member_id, policy_id | ERD 호환 |
| policy_calendar_event | 예 | 아니오 | id | member, policy | 없음 | ERD 호환 |
| policy_notification | 예 | 아니오 | id | member, bookmark, policy | 없음 | ERD 호환 |
| savings_product | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| savings_product_option | 예 | 아니오 | id | savings_product | 없음 | ERD 호환 |
| pension_product | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| pension_product_option | 예 | 아니오 | id | pension_product | 없음 | ERD 호환 |
| loan_product | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| loan_product_option | 예 | 아니오 | id | loan_product | 없음 | ERD 호환 |
| product_bookmark | 예 | 아니오 | id | member | 없음 | ERD 호환 |
| product_recommendation | 예 | 아니오 | id | member | 없음 | ERD 호환 |
| financial_profile | 예 | 아니오 | id | member | member_id | ERD 호환 |
| surplus_fund | 예 | 아니오 | id | member, goal | 없음 | ERD 호환 |
| goal_history | 예 | 아니오 | id | member, goal | 없음 | ERD 호환 |
| budget | 예 | 아니오 | id | 없음 | 없음 | `budget`, `year_month`는 MySQL 예약어 충돌 방지를 위해 백틱 사용 |
| category | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| cards | 예 | 아니오 | id | member | 없음 | ERD 호환 |
| merchant_alias | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| merchant | 예 | 아니오 | id | merchant_alias | 없음 | ERD 호환 |
| card_transaction | 예 | 아니오 | id | member, cards, merchant, goal, category | 없음 | ERD 호환 |
| merchant_alias_terms | 예 | 아니오 | id | merchant_alias, member | 없음 | ERD 호환 |
| recurring_payment_group | 예 | 아니오 | id | 없음 | 없음 | ERD 호환 |
| fixed_expense_candidate | 예 | 아니오 | id | recurring_payment_group, category | 없음 | ERD 호환 |
| fixed_expense | 예 | 아니오 | id | fixed_expense_candidate, category, cards | 없음 | ERD 호환 |
| recurring_payment_group_transaction | 예 | 아니오 | recurring_group_id, transaction_id | recurring_payment_group, card_transaction | PK | ERD 호환 |
| habit_expense | 예 | 아니오 | id | fixed_expense_candidate, merchant, category | 없음 | ERD 호환 |
| notification | 예 | 아니오 | id | fixed_expense | 없음 | ERD 호환 |
| user_merchant_preferences | 예 | 아니오 | id | merchant, category | 없음 | ERD 호환 |

## 운영 테이블

| 테이블 | Flyway | Entity | 비고 |
|---|---:|---:|---|
| policy_raw_data | V2 | 예 | 페이지별 원본 응답 저장 |
| policy_collection_run | V2 | 예 | 수집 실행 이력 |
| policy_collection_error | V2 | 예 | 수집 오류 이력 |
| policy_embedding_sync | V2 | 예 | 임베딩 대기열과 동기화 상태 |
