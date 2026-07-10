# Security Policy

Author: Andy Yang

Security is a core goal of Microservice Framework. Please report vulnerabilities responsibly so they can be reviewed and fixed before public disclosure.

## Supported Versions

| Version | Supported |
| --- | --- |
| `main` | Yes |
| Released versions | Best effort until a formal release policy is published |

## Reporting A Vulnerability

Please do not disclose security vulnerabilities through public issues.

Use GitHub private vulnerability reporting if it is enabled for the repository. If it is not available yet, create a minimal public issue stating that you need a private security contact, without including exploit details.

When reporting, include:

- Affected module or starter.
- Impact and attack scenario.
- Minimal reproduction steps.
- Relevant configuration.
- Suggested fix, if available.

## Security Expectations

- Do not commit credentials, tokens, private keys, or local environment secrets.
- Do not include company-specific internal domains, IPs, or private infrastructure details.
- Security-sensitive failures should fail closed unless the relevant PRD explicitly defines another behavior.
- Logs, traces, metrics, and error responses must not expose sensitive data.

## Disclosure

The project aims to acknowledge valid reports promptly, investigate impact, prepare a fix, and coordinate disclosure after a patched release or documented mitigation is available.
