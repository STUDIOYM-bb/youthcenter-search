#!/usr/bin/env bash
set -euo pipefail

archive="${1:-}"
failed=0

report() {
  printf '%s\n' "$1" >&2
  failed=1
}

tracked_secrets="$(git ls-files '.env' 'config/application-secret.yml' 'src/main/resources/application-secret.yml' '*application-secret.yml' 2>/dev/null || true)"
if [[ -n "$tracked_secrets" ]]; then
  report "Secret files are tracked by Git:"
  printf '%s\n' "$tracked_secrets" >&2
fi

tracked_candidates="$(git ls-files 'src/main/java' 'src/main/resources' 'src/test/java' 'scripts' 'docs' 'README.md' 'build.gradle' 'settings.gradle' '.gitignore' '.editorconfig' 2>/dev/null || true)"
if [[ -n "$tracked_candidates" ]]; then
  secret_hits="$(printf '%s\n' "$tracked_candidates" \
    | xargs grep -IlE 'sk-[A-Za-z0-9_-]{20,}|OPENAI_API_KEY[[:space:]]*=' 2>/dev/null || true)"
  if [[ -n "$secret_hits" ]]; then
    report "Potential secret patterns found in tracked files:"
    printf '%s\n' "$secret_hits" >&2
  fi
fi

if [[ -n "$archive" ]]; then
  if [[ ! -f "$archive" ]]; then
    report "Archive not found: $archive"
  else
    archive_hits="$(zipinfo -1 "$archive" 2>/dev/null \
      | grep -E '(^|/)(\.env|application-secret\.yml)$|(^|/)(build|\.gradle|\.git|\.idea|out|logs)/|\.jar$|\.zip$|__MACOSX|\.DS_Store' || true)"
    if [[ -n "$archive_hits" ]]; then
      report "Forbidden files found in archive:"
      printf '%s\n' "$archive_hits" >&2
    fi
  fi
fi

exit "$failed"
