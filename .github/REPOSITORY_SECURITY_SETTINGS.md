# Repository Security Settings

Author: Andy Yang

This document lists the recommended GitHub repository settings for `andy-library/microservice-framework`.

Some settings must be enabled in the GitHub web UI because they cannot be enforced by repository files alone.

## Repository Description

```text
Enterprise-grade Java/Spring Boot microservice framework with parent POMs, BOM governance, production-ready starters, demo verification, and JMeter performance reports.
```

## Branch Protection For `main`

Enable branch protection for `main`:

- Require a pull request before merging.
- Require approvals: `1` or more.
- Require review from Code Owners.
- Dismiss stale pull request approvals when new commits are pushed.
- Require status checks to pass before merging.
- Required status check:
  - `Verify Parent, Starters, And Demo`
- Require branches to be up to date before merging.
- Require conversation resolution before merging.
- Block force pushes.
- Block deletions.
- Do not allow bypassing the above settings unless you intentionally define a trusted maintainer policy.

## Actions Security

Recommended GitHub Actions settings:

- Actions permissions: allow selected actions and reusable workflows, or allow GitHub-created actions and verified creators.
- Workflow permissions: read repository contents only.
- Disable "Allow GitHub Actions to create and approve pull requests" unless specifically needed.
- Require approval for first-time contributors.
- Do not expose repository secrets to pull requests from forks.

## Pull Request Safety

- Treat all external pull requests as untrusted until reviewed.
- Review `.github/workflows/**`, Maven plugin changes, parent POM changes, dependency version changes, and shell scripts carefully.
- Avoid `pull_request_target` for workflows that check out and execute contributor code.
- Prefer small, focused pull requests.
- Reject unexplained binary files, encoded payloads, generated artifacts, and unexpected executable files.

## Release Safety

- Releases should be created only from reviewed commits on `main`.
- Release tags should be annotated.
- Release notes should be reviewed before publishing.
- Do not publish release artifacts that include credentials, local test outputs, raw JMeter `.jtl` files, logs, or local IDE files.

## Topics

Use the topic list in `GITHUB_TOPICS.md`.
