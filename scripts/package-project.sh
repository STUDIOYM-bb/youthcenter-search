#!/usr/bin/env bash
set -euo pipefail

output="${1:-build/youth-center-api-lab-share.zip}"
mkdir -p "$(dirname "$output")"
rm -f "$output"

zip -r "$output" . \
  -x ".git/*" \
  -x ".idea/*" \
  -x ".gradle/*" \
  -x "build/*" \
  -x "out/*" \
  -x ".env" \
  -x ".env.*" \
  -x "src/main/resources/application-secret.yml" \
  -x "*.log"

echo "Created $output"
