# Audit Starter 技术与测试设计

Author: Andy Yang

对应需求：[Audit PRD](../../PRD/starters/audit-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring AOP、Spring Transaction；Database Outbox 或 Kafka 可靠投递；Security optional/default for B 端。

## 2. 技术设计

- 自动配置：AuditCapture、AuditReliability、AuditProtection。
- 公共 API：`@AuditedOperation`、`AuditEvent`、`AuditActorResolver`、`AuditDiffExtractor`、`AuditSink`、`AuditRiskClassifier`。
- AOP 只捕获声明操作；事件包含操作者、动作、对象、摘要、结果、来源、RequestId/TraceId。
- 普通审计通过 Outbox/可靠消息投递；高风险操作使用同步确认策略，失败关闭。
- 审计存储采用追加写；提供哈希链/签名扩展和访问审计事件。
- B 端强制模式由依赖组合与启动校验控制，普通属性不可关闭。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| AOP/单元 | 成功、失败、拒绝、差异提取、操作者解析 |
| 自动配置 | B 端强制、普通应用按需、非法关闭、用户 Sink |
| 可靠性 | Outbox 原子性、消息失败、重试、重复和恢复 |
| 安全 | 篡改、删除、未授权查询/导出、敏感字段脱敏 |
| 集成 | Database/Kafka/Security 组合验证应用 |
| 性能 | 审计捕获开销与异步投递吞吐 |

通过条件：高风险失败关闭；普通审计最终可落地；记录不可被普通应用篡改或关闭。
