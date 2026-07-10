# Release Guide

Author: Andy Yang

This guide describes how to publish a GitHub release for Microservice Framework.

## First Release

The first prepared tag is:

```text
v0.1.0-alpha
```

Use `RELEASE_NOTES_v0.1.0-alpha.md` as the release description.

## GitHub UI Steps

1. Open `https://github.com/andy-library/microservice-framework/releases/new`.
2. Select tag `v0.1.0-alpha`.
3. Set release title:

```text
v0.1.0-alpha - Initial Open Source Preview
```

4. Paste the content of `RELEASE_NOTES_v0.1.0-alpha.md`.
5. Mark it as a pre-release.
6. Publish the release.

## Release Rules

- Release only from reviewed commits on `main`.
- Use annotated tags.
- Review release notes before publishing.
- Do not attach raw logs, `.jtl` files, local configuration files, or credentials.
