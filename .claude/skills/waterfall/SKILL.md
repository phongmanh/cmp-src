---
name: waterfall
description: Delivers a feature through the waterfall-engineer agent — Requirements and Design first, a user sign-off, then Implementation, Verification and Handover.
disable-model-invocation: true
argument-hint: <task description>
---

# Waterfall delivery

Task: $ARGUMENTS

If the task is empty, ask the user what to build before doing anything else.

The `waterfall-engineer` agent does the work and keeps its phase documents in `.claude/waterfall/<slug>/`. Your job is to run the gates: the user signs off on the design before any code is written.

## 1. Requirements and design

Spawn `waterfall-engineer` with the task and `stop after design`.

When it returns:

- **Requirements gate failed** (open questions) — ask the user, using AskUserQuestion when the answers are choices. Spawn the agent again with the same task plus the answers. It resumes at the failed phase.
- **Design gate failed** (several defensible designs) — present the options and the agent's recommendation, and let the user pick. Re-spawn the same way.
- **Design passed** — go to step 2.

## 2. Design sign-off

Read `02-design.md` and give the user a short summary: module placement, the build steps, the test plan, and any risks. Ask them to approve it or request changes.

- **Changes requested** — replace the document's last line with `Gate: FAILED — revision requested: <their changes>`, then re-spawn the agent with `stop after design`. Repeat step 2.
- **Approved** — go to step 3.

## 3. Build, verify, hand over

Spawn `waterfall-engineer` with the task and no stop instruction. It resumes at Implementation.

If it stops early — something the design didn't decide, or a failure that needs a design change — show the user what it found, and reopen the design as in step 2.

## 4. Report

From `05-handover.md`, give the user: what changed, the verification results, what wasn't verified, follow-ups, and the suggested commit message. Don't commit — that's the user's call.
