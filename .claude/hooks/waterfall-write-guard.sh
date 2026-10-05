#!/usr/bin/env bash
# Claude Code PreToolUse hook (Write, Edit), registered in the frontmatter of the
# waterfall-* agents rather than settings.json, so it only runs inside them.
# Keeps each agent to the files it owns: the phase documents named as arguments,
# under .claude/waterfall/<slug>/, and — with --source — project files outside
# .claude/. Exit code 2 stops the tool call and shows stderr to the agent.
#
# Usage: waterfall-write-guard.sh [--source] <document>...
#   e.g. waterfall-write-guard.sh --source 03-implementation.md

set -euo pipefail

allow_source=false
if [ "${1:-}" = "--source" ]; then
  allow_source=true
  shift
fi
owned="$*"
$allow_source && owned="$owned, and project source outside .claude/"

input="$(cat)"
path="$(printf '%s' "$input" | python3 -c 'import json,sys; t=json.load(sys.stdin).get("tool_input",{}); print(t.get("file_path") or t.get("notebook_path") or "")' 2>/dev/null || true)"

[ -z "$path" ] && exit 0

root="${CLAUDE_PROJECT_DIR:-$PWD}"
rel="$(python3 -c 'import os,sys; print(os.path.relpath(os.path.realpath(os.path.join(sys.argv[2], sys.argv[1])), os.path.realpath(sys.argv[2])))' "$path" "$root")"

deny() {
  echo "Blocked by waterfall-write-guard: $rel $1" >&2
  echo "This agent writes only: $owned" >&2
  echo "A defect in someone else's document goes in your own document and your gate — see waterfall-standards." >&2
  exit 2
}

case "$rel" in
  .claude/waterfall/*/*)
    doc="${rel#.claude/waterfall/*/}"
    case "$doc" in
      */*) deny "is not a phase document." ;;
    esac
    for name in "$@"; do
      [ "$doc" = "$name" ] && exit 0
    done
    deny "belongs to another phase."
    ;;
  ../*|/*)
    deny "is outside the project."
    ;;
  .claude/*)
    deny "is Claude Code configuration."
    ;;
  *)
    $allow_source && exit 0
    deny "is project source; this agent doesn't change code."
    ;;
esac
