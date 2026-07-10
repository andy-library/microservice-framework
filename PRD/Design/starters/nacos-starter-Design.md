# Nacos Starter 技术与测试设计

Author: Andy Yang

对应需求：[Nacos PRD](../../PRD/starters/nacos-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring Boot Config Data、Spring Cloud Alibaba Nacos Config、Kubernetes ConfigMap/Secret/环境变量/Config Tree。Nacos 版本必须先由 Dependencies BOM 锁定。

## 2. 技术设计

- 自动配置：`NacosConfigAutoConfiguration`、`KubernetesConfigAutoConfiguration`、`ConfigGovernanceAutoConfiguration`。
- 内部契约：`ConfigSourceDescriptor`、`ConfigChangeEvent`、`ConfigValidator`、`RefreshPolicy`、`SensitiveConfigMasker`。
- 使用 Boot Environment/Config Data 表达来源和优先级；禁止自行维护第二套配置容器。
- 可刷新项通过允许列表注册；变更先绑定并校验，再原子替换；失败保留最后有效值。
- Classpath 检测 Apollo Starter 并使用失败分析器阻断。
- Nacos 服务发现不在 V1 自动启用。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 来源组合、优先级、必填校验、刷新开关、Apollo 互斥 |
| 集成 | Testcontainers/受控 Nacos 发布配置、修改、删除与恢复 |
| K8s 契约 | ConfigMap、Secret、环境变量、Config Tree 冲突与来源诊断 |
| 故障 | 首次不可用、运行中断、非法刷新、恢复后更新 |
| 安全 | Secret 不出现在日志、异常和诊断输出 |
| 消费方 | 最小应用加载静态及动态配置并验证最后有效值 |

通过条件：优先级确定；互斥生效；运行中断保留有效配置并告警；首次缺失关键配置失败。
