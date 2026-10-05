---
name: waterfall
description: Delivers a feature in gated Waterfall phases through the waterfall-* agents — analyst (requirements), architect (design), a user sign-off, implementer, then verifier with a bounded fix loop and a handover.
disable-model-invocation: true
argument-hint: <task description> [stop after <phase>]
---

# Waterfall delivery

Task: $ARGUMENTS

If the task is empty, ask the user what to build before doing anything else. If it ends in `stop after <requirements|design|implementation|verification>`, stop once that phase passes.

Derive a short kebab-case slug. The workspace is `.claude/waterfall/<slug>/`. Each agent owns its documents and gates them; you run the gates, talk to the user, and pass every agent the slug. The protocol — gate categories, ownership, resuming — is the `waterfall-standards` skill; read it if you haven't.

If documents already exist there, resume at the first one that doesn't end in `Gate: PASSED`.

Each step below lists the results its agent can return. Any other result — no gate line, a run cut short by its turn limit, a category the step doesn't list — show the user what the agent's document says and stop.

## 1. Requirements — `waterfall-analyst`

Spawn it with the task and the slug.

- **`FAILED — questions`** — put the questions to the user with AskUserQuestion (the analyst gives options and a recommendation for each). Re-spawn it with the answers. Repeat until it passes.

## 2. Design — `waterfall-architect`

Spawn it with the slug.

- **`FAILED — options`** — present the alternatives and the recommendation; the user picks. Re-spawn it with the choice.
- **`FAILED — requirements`** — show the user the defect and reopen requirements (see **Reopening**).

## 3. Design sign-off

Read `02-design.md` and give the user a short summary: module placement, the build steps, the test plan and the riskiest part. Ask them to approve it or request changes. Nothing is built until they approve.

- **Changes requested** — reopen design with their changes, and repeat from step 2.

## 4. Implementation — `waterfall-implementer`

Spawn it with the slug.

- **`FAILED — design`** — the design left a decision open. Show the user the finding, get their call, and reopen design with it.

## 5. Verification — `waterfall-verifier`, at most 3 rounds

Spawn it with the slug and round N, starting at 1.

- **`PASSED`** — it has written `05-handover.md`; go to step 6.
- **`FAILED — implementation`** — spawn `waterfall-implementer` with the slug and "fix round for 04 round N", then the verifier again as round N+1.
- **`FAILED — design` or `requirements`** — show the user the upstream finding and reopen that phase.
- **Round 3 still fails** — show the user the unresolved findings and stop. Don't start round 4.

## 6. Report

From `05-handover.md`, give the user what changed, the verification results, what wasn't verified, the follow-ups and the suggested commit message. Don't commit — that's the user's call.

## Reopening a phase

Replace the reopened document's last line with `Gate: FAILED — revision requested: <the changes or the user's call>`, and the last line of every later document that exists with `Gate: FAILED — superseded: <phase> reopened`. Then continue from that phase's step. Code already written stays in the working tree; the implementer reconciles it with the revised design.
