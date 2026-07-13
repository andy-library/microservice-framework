# Redis Starter 技术与测试设计

Author: Andy Yang

对应需求：[Redis PRD](../../Requirements/starters/redis-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Spring Data Redis、Lettuce、Commons Pool2；版本沿用 Spring Boot BOM。V1 不使用 Redisson 和 Lua。

## 2. 技术设计

- 自动配置：Connection、Codec、Cache、Lock、RateLimit、Counter/Ranking。
- 公共 API：`RedisKey`、`CacheClient`、`DistributedLock`、`RateLimiter`、`CounterClient`、`RankingClient`。
- `CacheClient` 提供显式 get/load/put/evict，不通过隐藏注解自动更新业务缓存。
- Key 由应用、环境、服务、业务命名空间组成；默认要求 TTL。
- 锁使用原子 SET NX PX 与所有权 Token；续租为显式可选能力；删除必须校验所有权。
- 批量删除使用游标扫描与分批 UNLINK/DEL；共享集群通过前缀与权限隔离。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | 单机/哨兵/集群、JSON Codec、非法 TTL、用户覆盖 |
| 集成 | Testcontainers Redis；缓存、锁、限流、计数和排行榜 |
| 并发 | 热点回源合并、锁竞争/过期/误释放、限流准确性 |
| 故障 | 连接中断、超时、故障切换、恢复和受控回源 |
| 安全/架构 | Key 隔离、禁止全表缓存和阻塞全量命令 |
| 性能 | 热点读取、批量删除、锁和计数吞吐基准 |

通过条件：不返回旧缓存作为成功结果；锁所有权安全；故障不造成无界回源。
