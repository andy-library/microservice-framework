# Nacos Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-nacos-starter` |
| 引入策略 | 与 Apollo Starter 二选一，所有应用必须选择其一 |
| 能力组合 | Nacos + Kubernetes 原生配置 |

## 1. 定位与边界

Nacos Starter 统一应用从 Nacos、Kubernetes ConfigMap、Secret、环境变量和 Config Tree 获取配置的方式，并提供来源、优先级、刷新和失败治理。

组件不建设 Nacos 平台，不允许与 Apollo Starter 同时引入；服务发现默认使用 Kubernetes DNS，Nacos 服务发现仅作为未来显式扩展能力。

## 2. 技术栈与依赖

- Spring Boot Config Data API、Spring Cloud Alibaba Nacos Config。
- Kubernetes ConfigMap、Secret、环境变量与 Config Tree。
- 依赖 Common、JSON、Logging；与 Apollo Starter 构建期互斥。

## 3. 核心能力

1. 统一配置来源、优先级、命名规则和环境隔离。
2. Nacos 配置加载、动态刷新、变更校验、失败回滚和告警。
3. Kubernetes 配置、Secret 和基础安全配置接入。
4. 配置来源诊断、敏感值隐藏、关键配置保护和变更事件。
5. 必填配置、类型、范围和互斥规则校验。

## 4. 实现要求

- 固定优先级必须文档化并可诊断；同名冲突禁止静默覆盖。
- 动态刷新只允许标记为可刷新的配置；线程池、连接池等危险配置必须校验后原子生效或拒绝。
- Nacos 暂时不可用时可使用已加载配置继续运行并立即告警；首次启动缺失必需配置必须失败。
- Secret 不得出现在日志、诊断端点和异常详情。
- 检测到 Apollo Starter 必须在构建或启动阶段失败。

## 5. 配置与扩展

- `framework.nacos.*`
- `framework.config.kubernetes.*`
- `framework.config.precedence`
- `framework.config.refresh.*`
- 内部共享：配置校验、来源描述、敏感字段和变更事件契约。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| 多来源优先级 | 为各来源设置冲突值 | 最终值符合规范且来源可查询 |
| 动态刷新 | 修改允许和禁止刷新的配置 | 允许项原子生效，禁止项被拒绝并告警 |
| Nacos 中断 | 启动后断开 Nacos | 业务继续使用最后有效配置并报警 |
| 首次启动失败 | 缺失必填配置或 Nacos 无可用配置 | 启动失败，错误指出缺失项 |
| Secret 安全 | 检查日志、异常和诊断输出 | 无明文 Secret |
| 互斥 | 同时引入 Nacos 与 Apollo Starter | 构建或启动失败 |

## 7. 交付物

自动配置、配置元数据、优先级规范、动态刷新安全规范、故障演练用例和最小应用。
