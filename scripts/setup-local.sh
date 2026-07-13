#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

mkdir -p config

created=()
kept=()

if [ ! -f config/application-secret.yml ]; then
  cp config/application-secret.example.yml config/application-secret.yml
  created+=("config/application-secret.yml")
else
  kept+=("config/application-secret.yml")
fi

if [ ! -f .env ]; then
  cp .env.example .env
  created+=(".env")
else
  kept+=(".env")
fi

if [ ${#created[@]} -gt 0 ]; then
  for file in "${created[@]}"; do
    printf '[CREATED] %s\n' "$file"
  done
fi

if [ ${#kept[@]} -gt 0 ]; then
  for file in "${kept[@]}"; do
    printf '[EXISTS] %s\n' "$file"
  done
fi

cat <<'EOF'

다음 파일에 실제 로컬 값을 입력하세요.
- config/application-secret.yml

필수 항목:
- YOUTH_CENTER_API_KEY
- OPENAI_API_KEY
- ADMIN_API_KEY

RAG 사용 시 활성화:
- SPRING_AI_MODEL_CHAT=openai
- SPRING_AI_MODEL_EMBEDDING=openai
- RAG_ENABLED=true
EOF
