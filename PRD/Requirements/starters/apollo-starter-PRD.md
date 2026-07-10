# Apollo Starter PRD

Author: Andy Yang

| 属性 | 内容 |
| --- | --- |
| 制品 | `microservice-framework-apollo-starter` |
| 引入策略 | 与 Nacos Starter 二选一，所有应用必须选择其一 |
| 能力组合 | Apollo + Kubernetes 原生配置 |

## 1. 定位与边界

Apollo Starter 为选择 Apollo 的应用提供与 Nacos Starter 一致的配置治理契约，包括 Kubernetes 原生配置、来源优先级、动态刷新和敏感配置保护。

组件不建设 Apollo 平台，不允许与 Nacos Starter 同时引入。

## 2. 技术栈与依赖

- Spring Boot Config Data/Environment、Apollo Java Client。
- Kubernetes ConfigMap、Secret、环境变量与 Config Tree。
- 依赖 Common、JSON、Logging；与 Nacos Starter 构建期互斥。

## 3. 核心能力

1. Apollo AppId、Cluster、Namespace 和环境映射治理。
2. 多来源优先级、动态刷新、变更校验、失败回滚和告警。
3. Kubernetes 配置与 Secret 接入。
4. 配置来源诊断、敏感值隐藏、关键配置保护。
5. 与 Nacos Starter 共享一致的应用侧配置契约。

## 4. 实现要求

- Apollo Namespace 组合必须显式声明，禁止隐式加载未知 Namespace。
- 动态刷新按配置项允许列表执行；刷新失败保留最后有效值并告警。
- Apollo 短暂不可用时使用本地缓存或最后有效配置；首次启动缺失必需配置必须失败。
- Secret 和 Apollo Token 不得出现在日志与诊断输出。
- 检测到 Nacos Starter 必须在构建或启动阶段失败。

## 5. 配置与扩展

- `framework.apollo.*`
- `framework.config.kubernetes.*`
- `framework.config.precedence`
- `framework.config.refresh.*`
- 与 Nacos Starter 共用配置校验和来源契约。

## 6. 验收标准与方法

| 场景 | 验收方法 | 通过条件 |
| --- | --- | --- |
| Namespace 治理 | 配置允许与未声明 Namespace | 仅允许项加载，违规项被拒绝 |
| 多来源优先级 | 制造 Apollo 与 K8s 冲突值 | 结果符合规范且来源可诊断 |
| 动态刷新 | 更新正常、非法和危险配置 | 正常项原子生效，非法项保留旧值并告警 |
| Apollo 中断 | 断开 Apollo 后运行应用 | 使用最后有效配置，业务不中断并报警 |
| Secret 安全 | 扫描日志、异常和诊断 | 无敏感明文 |
| 互斥 | 同时引入 Apollo 与 Nacos Starter | 构建或启动失败 |

## 7. 交付物

自动配置、配置元数据、Namespace 规范、故障演练用例、共享配置契约测试和最小应用。
