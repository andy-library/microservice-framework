# Contributing

Author: Andy Yang

Thank you for helping improve Microservice Framework. The project is designed as a long-term enterprise microservice foundation, so contributions should improve production readiness, maintainability, documentation clarity, or real-world verification.

## Ways To Contribute

- Report bugs, missing capabilities, unclear documentation, or production adoption issues.
- Improve starters, tests, demo APIs, JMeter scripts, documentation, or build governance.
- Add real-world acceptance scenarios for high concurrency, reliability, security, observability, and service governance.
- Share production feedback that can help the framework evolve without adding unnecessary complexity.

## Contribution Principles

1. Keep the framework generic and free from company-specific names, domains, credentials, and internal implementation details.
2. Keep starter boundaries explicit. Do not merge unrelated capabilities for convenience.
3. Preserve production defaults: bounded resources, explicit timeouts, safe failure behavior, observability, and clear diagnostics.
4. Add or update tests for behavior changes.
5. Update PRD or design documents when a change modifies public capabilities, configuration, architecture constraints, or acceptance criteria.

## Local Verification

Install the parent and BOM first:

```bash
cd Microservice/SourceCode/microservice-framework-parent
mvn clean install
```

Then verify the changed starter:

```bash
cd Microservice/SourceCode/<changed-module>
mvn clean verify
```

For demo-level verification:

```bash
cd "Microservice/Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

## Pull Request Checklist

- The change is scoped to one clear purpose.
- Maven verification passes for the changed module.
- Demo verification is updated when the public application-facing behavior changes.
- Documentation is updated when user-facing behavior changes.
- No secrets, local credentials, internal company names, private URLs, or generated build artifacts are included.
- Markdown documents include `Author: Andy Yang` or `作者：Andy Yang`.

## Commit Style

Use clear commit messages:

```text
feat(redis): add bounded cache refresh contract
fix(logging): protect masking rule initialization
docs(readme): clarify demo verification flow
test(kafka): cover manual commit strategy
```

## Community

Issues and pull requests are welcome. Technical discussion should focus on sustainable enterprise architecture, production reliability, and practical developer experience.
