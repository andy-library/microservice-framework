# Parent 技术与测试设计

Author: Andy Yang

对应需求：[Parent PRD](../PRD/Parent-PRD.md)

## 1. 技术设计

### 1.1 制品与职责

| 制品 | 设计 |
| --- | --- |
| `microservice-framework-parent` | 聚合内部 Parent 模块；锁定 Java、插件和 Enforcer 规则 |
| `microservice-framework-dependencies` | 只管理第三方 BOM、版本属性和依赖坐标 |
| `microservice-framework-bom` | 恰好管理 19 个正式 Starter |
| `microservice-framework-starter-parent` | 业务应用 Parent；导入两个 BOM 并提供可执行 JAR 构建配置 |

Framework Starter 继承根 Parent；业务应用继承 Starter Parent。四层制品均不得声明运行时 Starter 依赖。

### 1.2 构建实现

- 保持 Java 21、Boot 3.3.13、Cloud 2023.0.6 和当前 Maven 插件版本。
- Enforcer 在 `validate` 阶段执行版本、收敛、上界、重复类、危险依赖、动态版本和仓库规则。
- Invoker 的 `install`、`run` 明确绑定 `integration-test`/`verify` 生命周期，所有正负 Fixture 由单次 `mvn clean verify` 执行。
- Dependencies BOM 补齐设计基线列明的第三方技术；Framework BOM 删除旧坐标并恰好管理 19 个冻结 Starter。
- Starter Parent 激活 Spring Boot `repackage`；根 Parent 和 Starter 不激活。
- 发布 Profile 生成 Sources 与 JavaDoc；Flatten 处理 `${revision}`。

### 1.3 失败诊断

每条治理规则使用独立执行 ID 和可执行错误信息。负面 Fixture 通过 `invoker.properties` 声明预期失败，不通过脚本绕过 Maven 生命周期。

## 2. 测试设计

### 2.1 契约矩阵

| Fixture | 验证内容 | 预期 |
| --- | --- | --- |
| `framework-starter-inherits-parent` | Starter 继承根 Parent | 普通 JAR 构建成功 |
| `business-parent-resolution` | 业务应用继承 Starter Parent | Parent 与两个 BOM 可解析 |
| `business-app-resolves-starters` | 无版本引用 19 Starter | 坐标均由 BOM 管理 |
| `framework-bom-resolution` | BOM 内容 | 恰好 19 Starter，无旧坐标 |
| `no-runtime-starter-inheritance` | Parent 纯净性 | 依赖树无 Starter |
| `app-fat-jar-packaging` | 业务应用打包 | 生成可执行 Fat JAR |
| `protected-version-override-fails` | 受保护版本覆盖 | 构建失败 |
| `convergence-violation-fails` | 依赖不收敛 | 构建失败 |
| `banned-dependency-fails` | 危险依赖 | 构建失败 |

### 2.2 执行与通过条件

1. `mvn clean verify`：全部正面 Fixture 成功、负面 Fixture 在预期规则失败，整体构建成功。
2. 解析四层 Effective POM 和依赖树：无运行时 Starter 传递引入。
3. 使用验证应用执行 `java -jar`：能够启动。
4. 分别以受支持最低 Maven 3.9.0 与当前 CI Maven 执行契约。
5. 对 Parent、Logging、Observability、Demo 执行全量回归。
