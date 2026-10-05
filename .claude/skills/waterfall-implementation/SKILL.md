---
name: waterfall-implementation
description: How to land 02-design.md and keep 03-implementation.md in a waterfall delivery — build rounds, fix rounds against verification findings, deviations versus design defects, and the gate. Preloaded by waterfall-implementer; not for direct use.
user-invocable: false
---

# Phase 3 — Implementation

Build exactly the design, step by step, running each step's **Verify** before starting the next. The design is the spec; the surrounding code is the style guide.

## Deviation or design defect

- **Mechanical** — a renamed symbol, a missing import, a path typo, a test name that clashes. Fix it and log it under **Deviations**.
- **A decision** — the step is impossible, contradicts the code, or needs a choice the design didn't make. Stop. Log it under **Blocked**, leave the remaining steps undone, and gate `FAILED — design`. A halted phase costs one round trip; an improvised fix costs the verifier's trust in the whole diff.

## Fix round

You're in a fix round when `04-verification.md` ends in `FAILED — implementation`. Read its latest round and address **every** finding, in order. If you believe a finding is wrong, leave the code it points at unchanged and say why — never skip one silently. Append a new round; don't rewrite earlier ones — the verifier reads the history.

## Template — `03-implementation.md`

```markdown
# Implementation: <task>

## Round 1 — build

### Completed
- Step 1: <what landed> — `path/File.kt`
- Step 2: ...

### Deviations
- <Anything not exactly as designed, and why. "None" is a fine answer.>

### Verification
- Step 1: `<command>` → <result>
- Touched paths (**Checks**): `<command>` → <result>

### Blocked
- <Steps not done, and the decision the design needs. Omit if none.>

## Round 2 — fixes for 04 round 1

### Findings
- #1: FIXED — <what changed> — `path/File.kt:42`
- #2: DISPUTED — <why the code is right as it is>

### Verification
- `<command>` → <result>

Gate: <PASSED | FAILED — design: <decision needed>>
```

## Gate

PASSED when every step (or every finding) is landed or disputed with a reason, and the **Checks** in `waterfall-standards` for the touched paths pass. A failure you can't fix within the design is `FAILED — design`; one you simply haven't fixed yet is not a gate result — keep working.

## Standards

- Match the surrounding code over your own defaults.
- Run the build. An unbuilt change is a guess.
- The requirements' **Out of scope** is binding. Adjacent problems you spot go in this document as notes, not in the diff.
- Don't add comments narrating the design. The code is the artifact; this document is the narration.
