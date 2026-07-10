# Microservice Framework Redis Starter

Author: Andy Yang

Redis Starter — 提供分布式锁、限流、原子计数器、缓存和排行榜能力，基于 Spring Data Redis。

## 核心能力

| 能力 | API 接口 | 说明 |
| --- | --- | --- |
| 分布式锁 | `DistributedLock` | 基于 Redis SETNX + Lua 释放脚本，确保获取和释放的原子性 |
| 限流 | `RateLimiter` | 基于 Redis 固定窗口 + Lua 脅脚本，按时间窗口控制请求频率 |
| 原子计数器 | `RedisCounter` | 基于 Redis INCR/DECR + Lua 脅脚本，支持原子增减 |
| 缓存增强 | `RedisCache` | TTL 管理、null 值缓存防穿透，可按需启用 |

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.microservice.framework</groupId>
    <artifactId>microservice-framework-redis-starter</artifactId>
</dependency>
```

版本由 `microservice-framework-bom` 统一管理，无需指定。

### 2. 最小配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379

framework:
  redis:
    lock:
      default-timeout: 3000    # 获取锁超时（毫秒）
      default-expire: 30000    # 锁持有时间（毫秒）
    rate-limit:
      default-permits: 100     # 每窗口允许请求数
      default-period: 1        # 窗口时长（秒）
    cache:
      enabled: true            # 是否启用缓存增强
      default-ttl: 3600        # 默认缓存 TTL（秒）
      null-value-ttl: 60       # null 值缓存 TTL（秒）
    counter:
      default-initial-value: 0 # 计数器初始值
```

### 3. 使用示例

```java
@Autowired
private DistributedLock distributedLock;

public void processOrder(String orderId) {
    if (distributedLock.tryLock("order:" + orderId)) {
        try {
            // 业务逻辑
        } finally {
            distributedLock.unlock("order:" + orderId);
        }
    }
}
```

## 配置参考

所有配置前缀为 `framework.redis`。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 Redis Starter |
| `lock.default-timeout` | `3000` | 获取锁默认超时时间（毫秒） |
| `lock.default-expire` | `30000` | 锁默认持有时间（毫秒） |
| `rate-limit.default-permits` | `100` | 每窗口默认允许请求数 |
| `rate-limit.default-period` | `1` | 默认限流窗口时长（秒） |
| `counter.default-initial-value` | `0` | 计数器默认初始值 |
| `cache.enabled` | `true` | 是否启用缓存增强 |
| `cache.default-ttl` | `3600` | 默认缓存 TTL（秒） |
| `cache.null-value-ttl` | `60` | null 值缓存 TTL（秒） |

## 自动注册 Bean

当 Spring Data Redis 在 classpath 上且 `framework.redis.enabled=true` 时，自动注册：

| Bean 名称 | 类型 | 条件 |
| --- | --- | --- |
| `redisTemplate` | `RedisTemplate<String, Object>` | `@ConditionalOnMissingBean(name="redisTemplate")` |
| `stringRedisTemplate` | `StringRedisTemplate` | `@ConditionalOnMissingBean` |
| `redisDistributedLock` | `DistributedLock` | `@ConditionalOnMissingBean` |
| `redisRateLimiter` | `RateLimiter` | `@ConditionalOnMissingBean` |
| `redisCounter` | `RedisCounter` | `@ConditionalOnMissingBean` |
| `redisCache` | `RedisCache` | `@ConditionalOnMissingBean` + `framework.redis.cache.enabled=true` |

用户可通过注册自定义同类型 Bean 覆盖默认实现。

## 集成测试策略

| 状态 | 说明 |
| --- | --- |
| 当前覆盖 | 自动配置条件、属性绑定、API 契约（mock RedisConnectionFactory） |
| 真实中间件 | 暂未包含 |
| 计划方案 | Testcontainers Redis 模块提供真实 Redis 实例，验证分布式锁互斥、限流窗口、缓存 TTL 过期等需要真实 Redis 行为的场景 |
| 执行方式 | Failsafe + `@Tag("integration")` profile，不在默认 `mvn clean verify` 中 |
| 补齐时间 | 下一迭代 |

当前 `mvn clean verify` 仍为默认准入门禁。真实中间件集成测试将在 Dependencies BOM 补齐对应 Testcontainers 模块后落地。
