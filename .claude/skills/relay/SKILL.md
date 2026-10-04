---
name: relay
description: Runs the model relay on a task — optional relay-advisor, relay-planner, a plan sign-off, relay-implementer, then relay-reviewer with a bounded fix loop.
disable-model-invocation: true
argument-hint: <task description>
---

# Model relay

Task: $ARGUMENTS

If the task is empty, ask the user what to build before doing anything else.

Derive a short kebab-case slug. The relay directory is `.claude/relay/<slug>/`. The agents pass their work to each other through files there; you run the stages and the gates.

## 1. Advice — only when it earns it

Spawn `relay-advisor` only if the task has more than one defensible approach, crosses module boundaries, involves concurrency or state correctness, or is expensive to reverse. Otherwise skip it, and say so in one line.

## 2. Plan, and sign-off

Spawn `relay-planner` with the task and the relay directory.

- **The planner says the task needs the advisor** — run step 1, then plan again.
- **`01-plan.md` has open questions** — put them to the user, then re-spawn the planner with the answers.

Then summarise the plan for the user (steps, files, out of scope) and wait for their approval before any code is written. If they ask for changes, re-spawn the planner with those changes.

## 3. Implement and review — at most 3 review rounds

1. Spawn `relay-implementer` with the relay directory.
2. Spawn `relay-reviewer` with the relay directory and round N (starting at 1).
3. If the verdict is `CHANGES_REQUESTED`, spawn `relay-implementer` with the relay directory and `03-review-N.md`, then review again as round N+1.

Stop and hand the decision to the user when:

- the verdict is `APPROVED`;
- the implementer reports a step **Blocked** — the plan needs a decision, so ask the user whether to re-plan;
- round 3 still ends in `CHANGES_REQUESTED` — show the unresolved findings and don't start round 4.

## 4. Report

Give the user the verdict, the files changed, the build and test results from the last review, and any disputed or MINOR findings left open. Don't commit — that's the user's call.
