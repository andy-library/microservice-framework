# v1.0.0-alpha.1 - AI-Driven Community Preview

Author: Andy Yang

[中文](#中文) | [English](#english)

## 中文

`v1.0.0-alpha.1` 是 Microservice Framework 在完成生产边界加固、公开 PRD 整理和社区治理建设后的首个统一版本预览。

### 核心内容

- Java 21、Spring Boot 3.3.13、Spring Cloud 2023.0.6 技术基线。
- Parent、Dependencies BOM、Framework BOM 和 Starter Parent 统一治理。
- 19 个面向生产场景的 Spring Boot Starter。
- 独立 Demo 应用，通过 Controller API 验证面向应用开发者的公开能力。
- MySQL、Redis、Kafka、Elasticsearch、Nacos 和 Apollo 真实中间件验收入口。
- Maven 全量构建、消费方契约、Dependency Review 和 CodeQL 安全门禁。
- Bouncy Castle 1.84、MinIO Java Client 8.6.0 和 Commons Lang 3.18.0 安全升级。
- 中英文 README、公开 PRD、技术设计、贡献指南和安全策略。

### AI 驱动工程模式

项目源于 Andy Yang 自 JDK 8 时代维护并在多个实际项目中验证的企业级脚手架。本次版本在既有实践上由 AI 参与需求整理、设计、实现、测试、审查和文档维护，维护者负责目标、架构约束、质量标准和最终验收。

### 使用方式

当前制品尚未发布到 Maven Central。请克隆源码并先安装 Parent 与 BOM：

```bash
git clone --branch v1.0.0-alpha.1 https://github.com/andy-library/microservice-framework.git
cd microservice-framework/SourceCode/microservice-framework-parent
mvn clean install
```

随后可以构建所需 Starter，并运行 `Microservice Demo/SourceCode/microservice-framework-demo` 进行验证。

### 发布性质

这是面向社区验证的预发布版本，不构成最终兼容性承诺。欢迎通过 Discussions、Issues 和 Pull Requests 提交真实项目反馈、测试证据和改进建议。

XXL-JOB `2.4.2` 存在上游尚未提供修复版本的低危 SSRF 公告。使用方必须限制执行器回调地址和网络访问，并按照安全策略完成部署评审。

## English

`v1.0.0-alpha.1` is the first version-aligned preview of Microservice Framework after production-boundary hardening, public PRD curation, and community-governance improvements.

### Highlights

- Java 21, Spring Boot 3.3.13, and Spring Cloud 2023.0.6 baseline.
- Governed Parent, Dependencies BOM, Framework BOM, and Starter Parent.
- 19 production-oriented Spring Boot Starters.
- Standalone Demo application with Controller APIs for application-facing capability verification.
- Real-middleware acceptance entry points for MySQL, Redis, Kafka, Elasticsearch, Nacos, and Apollo.
- Maven verification, consumer contracts, Dependency Review, and CodeQL security gates.
- Security updates to Bouncy Castle 1.84, MinIO Java Client 8.6.0, and Commons Lang 3.18.0.
- Bilingual README files, public PRDs, technical designs, contribution guidance, and security policy.

### AI-Driven Engineering Model

The project grew from an enterprise scaffold maintained by Andy Yang since the JDK 8 era and validated across multiple real projects. For this release, AI participated in requirements refinement, design, implementation, testing, review, and documentation, while the maintainer remained responsible for goals, architecture constraints, quality standards, and final acceptance.

### Usage

Artifacts are not yet available from Maven Central. Clone the source and install the Parent and BOMs first:

```bash
git clone --branch v1.0.0-alpha.1 https://github.com/andy-library/microservice-framework.git
cd microservice-framework/SourceCode/microservice-framework-parent
mvn clean install
```

You can then build the required Starters and run `Microservice Demo/SourceCode/microservice-framework-demo` for verification.

### Release Status

This is a community preview and not a final compatibility commitment. Real-world feedback, test evidence, and improvement proposals are welcome through Discussions, Issues, and Pull Requests.

XXL-JOB `2.4.2` has an upstream low-severity SSRF advisory without a patched release. Consumers must restrict executor callback targets and network access and complete a deployment security review.
