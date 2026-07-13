# Database Starter 技术与测试设计

Author: Andy Yang

对应需求：[Database PRD](../../Requirements/starters/database-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring JDBC、Spring Transaction、HikariCP、ShardingSphere-JDBC、Flyway。数据库驱动、ShardingSphere 与 Flyway 必须由 Dependencies BOM 管理。

## 2. 技术设计

- 自动配置拆分：DataSource、ShardingSphere、Transaction、Migration、Idempotency、Outbox。
- 默认使用 ShardingSphere-JDBC 读写分离；`framework.database.sharding.enabled=false`。
- 公共 API：`RoutingHint`、`TransactionTemplateFacade`、`IdempotencyStore`、`OutboxPublisher`；不暴露业务 Repository。
- 事务内、写后读和显式强一致读取路由主库；普通查询路由读库。
- Outbox 与业务写同事务，发布器使用抢占/租约、重试和幂等发送。
- Flyway 迁移遵守 Expand/Contract；Starter 不提供业务表结构。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 默认读写分离、关闭分片、非法规则、用户 DataSource 覆盖 |
| 集成 | Testcontainers 主/读数据库；路由、事务、迁移和分片 |
| 一致性 | 写后读、事务内读、读库延迟、主库/读库故障 |
| Outbox | 原子落库、发布失败、重复发布、并发抢占和恢复 |
| 架构 | 禁止跨服务共享模型和未受管数据库驱动版本 |
| 性能 | 连接池、路由、批量写和 Outbox 发布吞吐基准 |

通过条件：路由符合语义；事务与 Outbox 原子性成立；任何部分失败不返回伪成功。
