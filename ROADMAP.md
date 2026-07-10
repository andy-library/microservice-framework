# Roadmap

Author: Andy Yang

This roadmap describes the intended evolution of Microservice Framework. It is not a commitment to a fixed date; priorities may change based on production feedback and community contributions.

## Current Baseline

- Parent, dependency BOM, framework BOM, starter parent, and 19 starters.
- Demo application for starter capability verification.
- JMeter scripts and bilingual performance reports.
- PRD and design documentation for framework-level and starter-level capabilities.

## Near Term

- Improve quick-start documentation for first-time users.
- Keep CI verification stable across parent, starters, and demo.
- Expand demo APIs for all application-facing starter capabilities.
- Improve integration tests for middleware-backed starters.
- Add clearer configuration examples for Nacos, Apollo, Redis, Kafka, Elasticsearch, and database scenarios.

## Mid Term

- Publish formal release versioning rules.
- Add release notes and migration guides.
- Improve starter-level README coverage.
- Strengthen production diagnostics for high-concurrency scenarios.
- Add more acceptance tests around graceful shutdown, retries, timeouts, and failure isolation.

## Long Term

- Build a stronger community-driven starter ecosystem.
- Add reference architectures for C-end high-traffic systems and enterprise middle-platform systems.
- Improve performance baselines with repeatable JMeter scenarios.
- Establish compatibility policies for Spring Boot, Spring Cloud, Java, and key middleware versions.

## Contribution Focus

The most valuable contributions are production-proven improvements: safer defaults, clearer diagnostics, better tests, realistic demo scenarios, and documentation that helps application teams adopt the framework confidently.
