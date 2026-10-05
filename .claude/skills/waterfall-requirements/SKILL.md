---
name: waterfall-requirements
description: How to write and gate 01-requirements.md in a waterfall delivery — the template, what makes a requirement testable, and the gate. Preloaded by waterfall-analyst; not for direct use.
user-invocable: false
---

# Phase 1 — Requirements

Pin down *what*, never *how*. Read enough of the code to know what already exists — so you don't require something that's there, or miss a constraint it imposes — but name no class, module or library.

## Template — `01-requirements.md`

```markdown
# Requirements: <task>

## Goal
<The problem and the user outcome, one paragraph.>

## Current behaviour
<What the app does today in this area, with file:line where it helps. "Nothing — new screen" is fine.>

## Functional requirements
- **R1** — <one observable behaviour>
- **R2** — ...

## Non-functional requirements
- **N1** — <Android and iOS parity, offline, performance, accessibility, security, localisation — only those that apply, each checkable>

## Acceptance criteria
- **R1** — Given <state>, when <action>, then <observable result>.
- **R2** — ...

## Out of scope
- <What a reasonable engineer might drift into, and won't.>

## Open questions
- **Q1** — <question> — Options: <a> / <b> — Recommended: <a>, because <reason>.

Gate: <PASSED | FAILED — questions: Q1, Q2>
```

## What makes a requirement testable

- It names an observable result — something on screen, in storage, or sent over the wire — not an intention ("works well", "handles errors").
- Every failure the user can hit is its own requirement: offline, timeout, server error, empty result, invalid input, 401.
- Each acceptance criterion could be turned into a ViewModel, repository or use-case test without asking anyone what it means.

## Answers

When you are re-spawned with answers, fold each one into the requirements it settles, and delete the question. Don't leave an "Answered" trail — the document states what is true now.

## Gate

PASSED when every `R` is testable, every `N` is checkable, every `R` has acceptance criteria, and **Open questions** is empty. Otherwise `FAILED — questions`. A gap that changes what gets built or tested — scope, behaviour, failure handling — is a question, not a guess. A gap the codebase or CLAUDE.md already settles is not: cite the convention under **Current behaviour** and move on. Give each question options and a recommendation so the user can answer with one click.
