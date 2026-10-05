---
paths:
  - ".claude/waterfall/**"
---

# Waterfall phase documents

Files under `.claude/waterfall/<slug>/` are the gated phase documents of a waterfall delivery (the `/waterfall` skill). Each one is owned by one agent — `01` analyst, `02` architect, `03` implementer, `04`/`05` verifier — and ends in a gate line: `Gate: PASSED` or `Gate: FAILED — <category>: <reason>`.

- A document ending in `Gate: PASSED` is signed off. Don't edit its content, and don't build on a document that hasn't passed.
- Outside its owning agent, the only edit to a document is to its gate line, to reopen a phase: `Gate: FAILED — revision requested: <changes>` on the reopened document, and `Gate: FAILED — superseded: <phase> reopened` on every later one that exists. Then re-spawn the owner — it revises from there.
- The full protocol (categories, resuming, rounds) is the `waterfall-standards` skill.
