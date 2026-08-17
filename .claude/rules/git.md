# Git & PR Process

- Branch naming: `feature/<ticket-id>-short-desc`, `fix/<ticket-id>-short-desc`
- Commit messages: imperative mood, reference ticket ID (`JIRA-123: Add offline cache to UserRepository`)
- No direct commits to `main`/`develop` — PR required
- PRs must be small and focused on one change; split unrelated changes into separate PRs
- Before opening a PR: run `./gradlew ktlintCheck detekt test` locally and ensure all pass
- PR description must state what changed and why, not just what
