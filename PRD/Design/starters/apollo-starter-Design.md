# Apollo Starter 技术与测试设计

Author: Andy Yang

对应需求：[Apollo PRD](../../Requirements/starters/apollo-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring Boot Environment/Config Data、Apollo Java Client、Kubernetes 原生配置。Apollo Client 版本必须先由 Dependencies BOM 锁定。

## 2. 技术设计

- 自动配置：`ApolloConfigAutoConfiguration`、`KubernetesConfigAutoConfiguration`、`ConfigGovernanceAutoConfiguration`。
- AppId、环境、Cluster、Namespace 必须显式绑定；仅加载允许 Namespace。
- 与 Nacos 共用内部配置治理契约，但不形成 Starter 间依赖；公共内部代码可落入 `config-core`。
- Apollo 变更监听器执行绑定、校验、原子应用和失败回滚。
- Apollo 本地缓存只作为最后有效配置来源；来源可诊断，敏感值隐藏。
- Classpath 检测 Nacos Starter 并阻断启动。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | AppId/Namespace 校验、优先级、Nacos 互斥、用户扩展 |
| 集成 | 受控 Apollo 环境发布、刷新、删除和回滚 |
| K8s 契约 | 与 Nacos Starter 使用同一来源优先级测试套件 |
| 故障 | Apollo 不可用、本地缓存、非法变更、恢复 |
| 安全 | Token、Secret 和敏感配置输出扫描 |
| 消费方 | 最小应用验证 Namespace 允许列表和动态刷新 |

通过条件：与 Nacos 配置治理语义一致；互斥生效；非法变更不污染运行配置。
