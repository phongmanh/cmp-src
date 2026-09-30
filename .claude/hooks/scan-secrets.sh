#!/usr/bin/env bash
# Claude Code Stop hook.
# Looks at everything changed in the working tree and warns loudly if something
# that looks like a secret was written into the code.
#
# Install at: .claude/hooks/scan-secrets.sh   (chmod +x)

set -uo pipefail

# stop_hook_active is true when Claude is only still going because a Stop hook
# blocked the last stop. Claude has already seen the findings once, so let it
# stop: blocking again would loop forever on a match it rightly left alone.
input="$(cat)"
active="$(printf '%s' "$input" | python3 -c 'import json,sys; print(str(json.load(sys.stdin).get("stop_hook_active", False)).lower())' 2>/dev/null || true)"
[ "$active" = "true" ] && exit 0

files="$(git diff --name-only --diff-filter=ACM 2>/dev/null; git diff --cached --name-only --diff-filter=ACM 2>/dev/null)"
files="$(printf '%s\n' "$files" | sort -u | grep -E '\.(kt|kts|java|swift|plist|xml|yaml|yml|json|properties)$' || true)"

[ -z "$files" ] && exit 0

patterns=(
  'password[[:space:]]*=[[:space:]]*"[^"$]{4,}"'
  'secret[[:space:]]*=[[:space:]]*"[^"$]{4,}"'
  'apiKey[[:space:]]*=[[:space:]]*"[^"$]{8,}"'
  'token[[:space:]]*=[[:space:]]*"[^"$]{8,}"'
  'jdbc:postgresql://[^"]*:[^"@]*@'
  'AKIA[0-9A-Z]{16}'
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'
  'eyJhbGciOi[A-Za-z0-9._-]{20,}'
)

found=0
while IFS= read -r file; do
  [ -f "$file" ] || continue
  for p in "${patterns[@]}"; do
    if grep -nEI "$p" "$file" >/dev/null 2>&1; then
      if [ "$found" -eq 0 ]; then
        echo "Possible hardcoded secret in changed files:" >&2
        found=1
      fi
      grep -nEI "$p" "$file" | sed "s|^|  $file:|" >&2
    fi
  done
done <<< "$files"

if [ "$found" -eq 1 ]; then
  echo "Take these out of the source before committing: secrets never go in source or version control (root CLAUDE.md)." >&2
  echo "If a match is a test fixture or not a real secret, tell the user instead of changing it." >&2
  exit 2
fi

exit 0
