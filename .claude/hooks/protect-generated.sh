#!/usr/bin/env bash
# Claude Code PreToolUse hook (Edit, MultiEdit, Write, NotebookEdit).
# Blocks hand edits to files a tool generates: Room's exported schemas and
# anything under a build or Gradle/Kotlin cache directory. Exit code 2 stops the
# tool call and shows stderr to Claude.
#
# Install at: .claude/hooks/protect-generated.sh   (chmod +x)

set -euo pipefail

input="$(cat)"
path="$(printf '%s' "$input" | python3 -c 'import json,sys; t=json.load(sys.stdin).get("tool_input",{}); print(t.get("file_path") or t.get("notebook_path") or "")' 2>/dev/null || true)"

[ -z "$path" ] && exit 0

deny() {
  echo "Blocked by protect-generated hook: $path $1" >&2
  echo "Don't write it another way (sed, a heredoc): change the source it is generated from." >&2
  exit 2
}

case "$path" in
  */schemas/*/[0-9]*.json)
    deny "is a schema Room exports. Change the entity, bump DATABASE_VERSION and compile core:database — that regenerates it."
    ;;
esac

# Build output: a `build`, `.gradle` or `.kotlin` directory that isn't inside a source tree
# (.gitignore keeps src/**/build/, so a source package may be called `build`).
for dir in build .gradle .kotlin; do
  case "/$path" in
    */"$dir"/*)
      outer="${path%%/$dir/*}"
      case "$outer/" in
        */src/*) ;;
        *) deny "is build output, regenerated on the next build." ;;
      esac
      ;;
  esac
done

exit 0
