# Common Starter 技术与测试设计

Author: Andy Yang

对应需求：[Common PRD](../../Requirements/starters/common-starter-PRD.md)

## 1. 技术栈

Java 21、Spring Boot Core/Autoconfigure、Jakarta Validation、Java Time、`SecureRandom`。仅依赖 Boot 基础能力，不使用 Hutool/Guava 作为公共 API。

## 2. 技术设计

- 包：`time`、`id`、`error`、`page`、`money`、`context`、`util`、`autoconfigure`。
- 公共 API：`FrameworkClock`、`IdGenerator`、`FrameworkErrorCode`、`PageRequest/PageResult`、`Money`、`FrameworkContext/ContextSnapshot`。
- 默认 ID：雪花类 `SnowflakeIdGenerator`；节点号显式配置；时钟回拨在容忍窗口内等待，超出窗口失败。
- 时间：`FrameworkClock` 基于配置 `ZoneId`；禁止直接暴露系统默认时区。
- 上下文使用不可变快照和 `ThreadLocal` 适配器，提供 `try/finally` 清理接口。
- 自动配置拆分为 Time、ID、Context 三类；值对象和错误码无需自动配置。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 单元 | 时区转换、金额精度、分页边界、错误码格式、上下文嵌套与清理 |
| 并发 | 多线程/多节点千万级 ID 无重复；回拨、序列溢出、节点冲突 |
| 自动配置 | 默认值、非法时区/节点号、用户自定义 `IdGenerator` 覆盖 |
| 架构 | 依赖树不含其他 Starter、Web、数据库和远程客户端 |
| 消费方 | 最小应用生成 ID、读取 Clock、传播并清理上下文 |
| 性能 | ID、上下文快照和 Money 热路径 JMH 基准 |

通过条件：`mvn clean verify` 全通过；ID 零重复；上下文零串扰；非法安全配置启动失败。
