# Search V2 Acceptance

This project now builds policy search from the stored Youth Center snapshots, not from truncated policy columns only.

Operational flow:

- Rebuild policy region classifications with `POST /api/admin/jobs/policy-region-rebuild`.
- Rebuild search projections with `POST /api/admin/search-projection/rebuild`.
- Refresh the in-memory lexical index with `POST /api/admin/search-index/refresh`.
- Queue and process V2 embeddings with `POST /api/admin/jobs/embedding-queue` and `POST /api/admin/jobs/embedding-process`.
- Run the local data quality suite with `POST /api/admin/search/quality-suite`.
- Run the SIGUNGU region candidate suite with `POST /api/admin/search/region-quality-suite`.

Region-search V3 notes:

- Policy region classification metadata uses `policy-region-v3`.
- Region-explicit broad/eligibility search no longer takes the first N active policy IDs.
- Region-explicit search builds the candidate pool from `policy_region.region_id` using the user SIGUNGU, parent SIDO, and KR nationwide region IDs.
- Legacy and standard `region_code` rows that represent the same official province/city are treated as the same eligible administrative region.
- `UNKNOWN` policies are excluded from primary results when `app.rag.search.include-unknown-region=false`.
- `EXACT_SIGUNGU`, `PARENT_SIDO`, `EXACT_SIDO`, `NATIONWIDE`, and `MULTIPLE_REGION_MATCH` remain equally region-eligible with region score 100.
- The first page promotes one exact SIGUNGU policy and one parent SIDO policy when such candidates exist, without adding a large region score bonus.

Generated reports:

- `build/reports/search-baseline.json`
- `build/reports/search-quality-report.json`
- `build/reports/search-quality-report.md`
- `build/reports/region-search-quality.json`
- `build/reports/region-search-quality.md`
- `build/reports/tongyeong-search-quality.json`

The quality suite validates policy-name search, broad discovery, eligibility search, topic search, region compatibility, and strict region false-positive cases against the current local MySQL and Qdrant data.
