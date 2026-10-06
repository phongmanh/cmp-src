---
name: waterfall-verification
description: How to verify a waterfall delivery and hand it over — 04-verification.md (traceability, checks, checklist review, findings, gate) and 05-handover.md. Preloaded by waterfall-verifier; not for direct use.
user-invocable: false
---

# Phase 4 — Verification, and Phase 5 — Handover

Prove the requirements, not just the build. **The diff is the truth; `03-implementation.md` is a claim about the diff.** Check the claim — run everything yourself.

## Verification

1. `git diff` and `git status --short` — what actually changed, new files included.
2. Run the **Checks** in `waterfall-standards` for those paths.
3. **Traceability** — for every `R`, find the test that proves it in the diff and confirm it ran and passed; for every `N`, the test or check the design named, and its result. A requirement with no test or check, or one that doesn't verify what its criterion says, is a finding.
4. **Design conformance** — every build step landed; nothing in the diff that no step asked for. The `docs/design/` files the design named match the code: every class, signature and call they draw exists, nothing in the diff they should show is missing, and no highlight is left in them.
5. **Mobile checklist** — review the diff against it (`waterfall-standards`).
6. In round N > 1, check every finding of round N-1 against the code: resolved, unresolved, or disputed — and whether the dispute holds.

You never modify source. If a test fails, report it with the first real error; the implementer fixes it.

## Severity

- **CRITICAL** — wrong behaviour, crash, data loss, race, security.
- **MAJOR** — a requirement not met or not tested, a design step not followed, a missing failure path, a failing check.
- **MINOR** — naming, structure, clarity. Doesn't block; list it and pass.

## Template — `04-verification.md`

```markdown
# Verification: <task>

## Round 1

### Checks
| Check | Command | Result |
|---|---|---|

### Traceability
| Requirement | Test | Result |
|---|---|---|
| R1 | `XViewModelTest` → `emits Error when offline` | ✅ |

### Prior findings (round > 1 only)
- Round <N-1> #1: RESOLVED | UNRESOLVED | DISPUTED — <evidence>

### Findings
#### 1. [CRITICAL|MAJOR|MINOR] <one-line title>
- **Where:** `path/File.kt:42`
- **Problem:** <concrete>
- **Fix:** <specific enough to act on without a conversation>
- **Upstream:** <only if the fix needs a design or requirements change — which, and why>

### Verified good
- <What you checked that holds up, so the next round doesn't re-examine it.>

### Not verified
- <Checks you couldn't run — on-device UI, a real iOS launch, Keychain tests needing an entitlement — and why.>

Gate: <PASSED | FAILED — implementation: <n> CRITICAL, <n> MAJOR | FAILED — design: <defect> | FAILED — requirements: <defect>>
```

PASSED means every check green, every `R` traced to a passing test, every `N` to a passing test or check — or, for a check only a person can run, listed under **Not verified** — and zero CRITICAL or MAJOR findings. If any finding is marked **Upstream**, the gate names that phase instead of `implementation`.

Don't invent findings to look thorough — a clean diff gets a short round and `PASSED`. Every finding needs `file:line`.

## Handover — `05-handover.md`

Write it only when `04` has passed, in the same run.

```markdown
# Handover: <task>

## Summary
<What changed and why, ready to paste into a PR description.>

## Files changed
- `path/File.kt` — <one line>

## System design
<The **Change summary** from `02`, as built, and the `docs/design/` files updated.>

## How to try it
- **Android** — <steps>
- **iOS** — <steps>

## Not verified
- <Carried from 04, with the reason.>

## Follow-ups
- <Known limitations, MINOR findings left open, adjacent problems noticed and left alone.>

## Commit message
<Following .claude/rules/git.md: imperative mood, ticket ID if the task gave one.>

Gate: PASSED
```

Handover is where the delivery stops. Never commit, push or open a PR.
