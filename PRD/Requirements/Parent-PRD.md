# Microservice Framework Parent PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 文档版本 | v1.0 |
| 文档状态 | 当前需求基线 |
| 实现位置 | `SourceCodes/microservice-framework-parent` |

## 1. 目标

Parent 项目为 Framework 内部组件和业务应用提供统一、可验证、不可隐式绕过的 Maven 构建与版本治理能力。

## 2. 四层制品

### 2.1 `microservice-framework-parent`

- 仅供 Framework 内部组件继承。
- 管理 Java 21、Maven、编码、插件和构建生命周期。
- 执行依赖收敛、危险依赖、重复类和基础质量门禁。
- 不作为业务应用 Parent。
- 不引入运行时 Starter。

### 2.2 `microservice-framework-dependencies`

- 管理批准的第三方 BOM 和依赖版本。
- 不管理 Framework 内部制品。
- 不声明运行时依赖。

### 2.3 `microservice-framework-bom`

- 管理 `Framework-Component-Catalog.md` 定义的 19 个正式 Starter。
- 所有正式 Starter 使用统一平台版本。
- 不传递引入任何 Starter。

### 2.4 `microservice-framework-starter-parent`

- 作为业务应用唯一 Parent。
- 导入 Dependencies BOM 和 Framework BOM。
- 提供业务应用编译、测试和可执行 JAR 构建能力。
- 不隐式引入运行时 Starter。

## 3. 基础治理能力

- Java 21 和最低 Maven 版本检查。
- UTF-8 和统一编译参数。
- Compiler、Surefire、Failsafe、Enforcer、Flatten、Source、JavaDoc 和 Spring Boot 插件版本管理。
- 依赖收敛、上界依赖和重复类检查。
- 禁止动态版本、危险依赖和未批准仓库。
- Framework Starter 与业务应用采用不同打包行为。
- Starter、Dependencies BOM 和 Framework BOM 职责边界检查。

## 4. 技术栈与实现要求

- Java 21、Maven、Spring Boot 3.3.x、Spring Cloud 2023.0.x。
- Maven Enforcer 承担 Java/Maven 版本、依赖收敛、危险依赖、动态版本和仓库治理。
- Maven Invoker 或等价机制承担真实消费方正面与负面契约测试，并必须绑定到 `verify` 生命周期。
- Framework Parent 与 Starter Parent 必须分离，避免业务应用继承 Framework 内部构建行为。
- BOM 只管理版本，不声明运行时依赖；Parent 只管理构建，不隐式引入 Starter。
- 所有规则必须提供可执行、可定位、可修复的失败信息。

## 5. 契约测试

Parent 必须自动验证：

1. Framework Starter 能够继承 Parent 并构建。
2. 业务应用能够继承 Starter Parent。
3. 业务应用能够无版本声明引用正式 Starter。
4. Parent 与 Starter Parent 不引入运行时 Starter。
5. Framework BOM 管理且仅管理正式 Starter。
6. Dependencies BOM 不管理 Framework 内部坐标。
7. Starter 不生成可执行 Fat JAR。
8. 业务应用能够生成可执行 Fat JAR。
9. 受保护版本覆盖、依赖收敛冲突和危险依赖能够被阻断。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 四层职责 | 检查各制品 Effective POM 与依赖树 | 依赖方向符合本文，无职责串层 |
| BOM 完整性 | 对照冻结目录解析 Framework BOM | 恰好管理 19 个正式 Starter，不传递引入 |
| Parent 纯净性 | 最小应用仅继承两个 Parent 分别构建 | 均不隐式获得运行时 Starter |
| 正面契约 | 执行全部消费方正面 Fixture | 无版本引用、Starter 构建、应用打包均通过 |
| 负面契约 | 执行版本覆盖、收敛冲突、危险依赖 Fixture | 每个 Fixture 均在预期规则处失败 |
| 生命周期绑定 | 仅执行 `mvn clean verify` | 自动运行全部契约，无需额外脚本 |
| 全量回归 | 构建 Parent、现有 Starter 和 Demo | 全部通过，依赖边界无回归 |
| IDEA 使用 | 分别导入 Parent、Starter 和 Demo | 可解析、测试、调试与运行 |

## 8. 交付物

四层 Parent/BOM 制品、Enforcer 规则、消费方契约 Fixture、自动验收脚本、版本清单、构建治理说明和真实 Demo。
