# Youth Center Field Mapping

| 온통청년 필드 | 저장 위치 | 설명 |
|---|---|---|
| `plcyNo` | `policy.source_policy_id` | YOUTH_CENTER 내 정책 식별자 |
| `plcyNm` | `policy.title` | 정책명 |
| `sprvsnInstCdNm`, `operInstCdNm`, `rgtrInstCdNm` | `policy.agency_name` | 주관/운영/등록 기관 후보 |
| `lclsfNm`, `mclsfNm`, `plcyMajorCd`, `plcyKywdNm` | `policy.category` | 공통 카테고리 추론 |
| `plcyExplnCn`, `plcySprtCn` | `policy.summary` | 요약/지원 내용 |
| `aplyUrlAddr`, `refUrlAddr1`, `refUrlAddr2` | `policy.official_url` | 공식 링크 후보 |
| `bizPrdBgngYmd` | `policy.start_date` | 사업 시작일 |
| `bizPrdEndYmd` | `policy.due_date` | 사업 종료일 |
| `sprtTrgtMinAge` | `policy_condition.min_age` | 최소 나이 |
| `sprtTrgtMaxAge` | `policy_condition.max_age` | 최대 나이 |
| `jobCd`, `ptcpPrpTrgtCn`, `addAplyQlfcCndCn` | `policy_condition.employment_status`, `condition_summary` | 취업 조건과 기타 자격 |
| `schoolCd`, `ptcpPrpTrgtCn` | `policy_condition.student_status` | 학생 조건 추론 |
| `earnCndSeCd`, `earnMinAmt`, `earnMaxAmt`, `earnEtcCn` | `policy_condition.income_condition` | 소득 조건 |
| `zipCd` | `policy_region` | 지역 코드 매핑 |

원본 응답 전체는 `policy_raw_data.response_body`에 페이지 단위로 저장한다. 공통 컬럼 길이를 초과하는 문자열은 의미 단위로 축약하고, API 키가 포함된 URL은 저장하지 않는다.
