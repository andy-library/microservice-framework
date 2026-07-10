# Microservice Framework

Author: Andy Yang

Enterprise-grade Java/Spring Boot microservice foundation for production-ready distributed systems.

[中文文档](./README-zh.md) | [Contributing](./CONTRIBUTING.md) | [Security](./SECURITY.md) | [Roadmap](./ROADMAP.md)

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.6-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)
![Build](https://github.com/andy-library/microservice-framework/actions/workflows/maven-verify.yml/badge.svg)

Microservice Framework is an enterprise-grade Java/Spring Boot microservice foundation. It provides managed parent POMs, BOM-based dependency governance, reusable starters, and a verification model so application teams can focus on business development.

## Why This Project

- Unified engineering governance with parent POMs, dependency BOMs, framework BOM, and starter parent.
- 19 production-oriented Spring Boot starters.
- Enterprise capabilities across web, security, logging, observability, Redis, Kafka, database, Elasticsearch, Feign, XXL-JOB, Drools, and more.
- Standalone demo application with Controller APIs for starter capability verification.
- JMeter scripts and bilingual performance reports for reproducible community testing.
- Designed for enterprise middle-platform systems, C-end high-traffic applications, and Spring Boot microservice teams that need unified engineering governance.

## Quick Start

```bash
git clone https://github.com/andy-library/microservice-framework.git
cd microservice-framework

cd SourceCode/microservice-framework-parent
mvn clean install

cd ../microservice-framework-common-starter
mvn clean verify
```

Demo verification:

```bash
cd "Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

## Project Structure

```text
Microservice/
├── README-zh.md
├── README-en.md
├── LICENSE
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── SECURITY.md
├── CHANGELOG.md
├── ROADMAP.md
├── PRD/
│   ├── Requirements/        # Product requirements and starter PRDs
│   └── Design/              # Technical design documents and stack matrix
├── SourceCode/
│   ├── microservice-framework-parent/
│   ├── microservice-framework-common-starter/
│   ├── microservice-framework-json-starter/
│   ├── microservice-framework-web-starter/
│   ├── microservice-framework-logging-starter/
│   ├── microservice-framework-observability-starter/
│   ├── microservice-framework-async-starter/
│   ├── microservice-framework-security-starter/
│   ├── microservice-framework-redis-starter/
│   ├── microservice-framework-database-starter/
│   ├── microservice-framework-kafka-starter/
│   ├── microservice-framework-elasticsearch-starter/
│   ├── microservice-framework-feign-starter/
│   ├── microservice-framework-xxl-job-starter/
│   ├── microservice-framework-audit-starter/
│   ├── microservice-framework-field-encryption-starter/
│   ├── microservice-framework-object-storage-starter/
│   ├── microservice-framework-drools-starter/
│   ├── microservice-framework-nacos-starter/
│   └── microservice-framework-apollo-starter/
└── Microservice Demo/
    ├── SourceCode/          # Standalone demo source code
    └── Reports/             # JMeter scripts and latest performance reports
```

The demo project has been moved to `Microservice Demo`, at the same level as `PRD` and `SourceCode`.

## Technology Stack

| Category | Technology |
| --- | --- |
| Language | Java 21 |
| Build | Maven 3.9+ |
| Application Framework | Spring Boot 3.3.13 |
| Microservice Stack | Spring Cloud 2023.0.6, Spring Cloud OpenFeign |
| Configuration | Nacos, Apollo, Kubernetes ConfigMap |
| Web | Spring MVC, Jakarta Validation, Springdoc OpenAPI |
| Security | Spring Security, OAuth2 Resource Server, JWT, Keycloak integration model |
| Logging | SLF4J, Logback, Logstash Logback Encoder |
| Observability | Spring Boot Actuator, Micrometer, Micrometer Tracing, OpenTelemetry, Prometheus, OTLP |
| Database | Spring JDBC/Transaction, HikariCP, ShardingSphere-JDBC, Flyway |
| Cache | Redis, Spring Data Redis, Lettuce |
| Messaging | Kafka, Spring Kafka |
| Search | Elasticsearch Java API Client |
| Jobs | XXL-JOB |
| Rules | Drools/KIE |
| Storage | S3-compatible object storage abstraction |
| Testing | JUnit 5, AssertJ, Mockito, REST Assured, Spring Boot Test, JMeter |

Detailed stack governance:

- `PRD/Design/Technology-Stack-Matrix.md`
- `PRD/Design/Engineering-Baseline.md`

## Core Capabilities

| Starter | Capability Summary |
| --- | --- |
| `common-starter` | Timezone, date/time, distributed ID, error codes, pagination, lightweight context, common tools |
| `json-starter` | Unified JSON abstraction, Jackson by default, Fastjson2 extension option |
| `web-starter` | Unified API response, request ID, validation, exception handling, OpenAPI integration |
| `logging-starter` | Structured logging, masking, async logging, flood protection, dynamic log level |
| `observability-starter` | Metrics, tracing, health, Prometheus, OpenTelemetry/OTLP integration |
| `async-starter` | Managed thread pools, context propagation, rejection policy, graceful shutdown |
| `security-starter` | JWT parsing, Keycloak-oriented resource server integration, internal trusted access model |
| `redis-starter` | Cache, distributed lock, rate limiting, counters, common Redis utilities |
| `database-starter` | Data source governance, transaction support, read/write separation, ShardingSphere-JDBC, outbox |
| `kafka-starter` | Producer, consumer builder, manual/auto commit model, idempotency, retry, DLQ |
| `elasticsearch-starter` | Index, query, paging, bulk operation, alias, runtime governance |
| `feign-starter` | Service-to-service call governance, context propagation, timeout, retry, isolation |
| `xxl-job-starter` | Job execution, idempotency, timeout, logging, retry hooks |
| `audit-starter` | Admin action audit, tamper-resistant audit entry model |
| `field-encryption-starter` | Field-level encryption abstraction and AES-GCM default capability |
| `object-storage-starter` | S3-compatible object storage abstraction, upload/download/delete/presigned URL |
| `drools-starter` | Rule loading, rule execution, version-aware rule evaluation |
| `nacos-starter` | Nacos + Kubernetes configuration support |
| `apollo-starter` | Apollo + Kubernetes configuration support |

## Parent And Dependency Governance

`microservice-framework-parent` contains the framework-level Maven governance model:

- `microservice-framework-parent`: parent for framework internal components
- `microservice-framework-dependencies`: third-party dependency BOM
- `microservice-framework-bom`: framework starter BOM
- `microservice-framework-starter-parent`: parent for business applications

Key principles:

- No private dependency versions inside starters.
- Third-party versions are governed through BOMs.
- Business applications inherit the starter parent and compose starters as needed.
- Nacos and Apollo are mutually exclusive runtime config-center options.

## Build

Install the parent and BOM first, then build the selected starter:

Example:

```bash
cd SourceCode/microservice-framework-parent
mvn clean install

cd ../microservice-framework-common-starter
mvn clean install
```

For demo verification, use `Microservice Demo/SourceCode/microservice-framework-demo`.

```bash
cd "Microservice Demo/SourceCode/microservice-framework-demo"
mvn clean verify
```

## Documentation

| Directory | Purpose |
| --- | --- |
| `PRD/Requirements` | Framework PRD, component catalog, parent PRD, starter PRDs |
| `PRD/Design` | Technical design, technology stack, engineering baseline, integration test strategy |
| `SourceCode` | Parent and starter source code |
| `Microservice Demo/SourceCode` | Standalone demo source code |
| `Microservice Demo/Reports` | JMeter scripts and latest performance reports |

## Open Source Notes

- This project is a general-purpose microservice framework and does not contain company-specific naming.
- The code author is standardized as `Andy Yang`.
- Build artifacts, logs, raw JMeter outputs, and local IDE files are ignored by the root `.gitignore`.
- See the repository-level `LICENSE` for licensing.
- See `CONTRIBUTING.md` for contribution guidelines.
- See `SECURITY.md` for vulnerability reporting.
- See `ROADMAP.md` for the project roadmap.
- See `RELEASE_GUIDE.md` for release publishing.
- See `.github/REPOSITORY_SECURITY_SETTINGS.md` for pull request and branch protection recommendations.

## Community

Microservice Framework is not intended to be a one-time release. The open-source goal is to continuously evolve the framework with the community and strengthen enterprise microservice architecture capabilities over time.

You can contribute by:

- Opening issues for bugs, usage questions, missing capabilities, and design suggestions.
- Submitting pull requests for code, documentation, tests, demo APIs, or JMeter scripts.
- Adding real-world starter acceptance cases and performance test results.
- Joining technical discussions around high concurrency, high availability, security, observability, and engineering governance.
- Sharing production adoption experience to help the framework mature.

If this project is useful to you or your team, donations, sponsorships, or infrastructure support are also welcome. Donations are not required for usage and do not change the open collaboration model; they help support long-term maintenance, test environments, documentation, and community growth.
