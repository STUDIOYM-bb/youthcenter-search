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
  -x "logs/*" \
  -x "__MACOSX/*" \
  -x ".DS_Store" \
  -x "*/.DS_Store" \
  -x "*.jar" \
  -x "*.zip" \
  -x ".env" \
  -x ".env.*" \
  -x "application-secret.yml" \
  -x "*/application-secret.yml" \
  -x "config/application-secret.yml" \
  -x "src/main/resources/application-secret.yml" \
  -x "*.log"

echo "Created $output"
