# Release Guide

Author: Andy Yang

This guide describes how to publish a GitHub release for Microservice Framework.

## Current Preview Release

The current prepared tag is:

```text
v1.0.0-alpha.1
```

Use `RELEASE_NOTES_v1.0.0-alpha.1.md` as the release description.

## GitHub UI Steps

1. Open `https://github.com/andy-library/microservice-framework/releases/new`.
2. Create tag `v1.0.0-alpha.1` from the reviewed `main` commit.
3. Set release title:

```text
v1.0.0-alpha.1 - AI-Driven Community Preview
```

4. Paste the content of `RELEASE_NOTES_v1.0.0-alpha.1.md`.
5. Mark it as a pre-release.
6. Publish the release.

## Release Rules

- Release only from reviewed commits on `main`.
- Use annotated tags.
- Review release notes before publishing.
- Do not attach raw logs, `.jtl` files, local configuration files, or credentials.
- Confirm Parent, BOM, all Starters, consumer contracts, Demo, Dependency Review, and CodeQL are green before tagging.
- Keep the Git tag, Maven `revision`, Demo parent version, changelog, and release title aligned.
- A GitHub source release does not imply Maven Central availability; state publication channels explicitly.
