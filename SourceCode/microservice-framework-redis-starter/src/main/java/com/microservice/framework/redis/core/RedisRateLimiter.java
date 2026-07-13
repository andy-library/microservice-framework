package com.microservice.framework.redis.core;

import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.RedisProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

/**
 * Redis 限流器默认实现
 * <p>
 * 基于 Redis 固定窗口算法 + Lua 脚本实现，确保计数和 TTL 设置的原子性。
 * <p>
 * 每次请求时，Lua 脅脚本在 Redis 中执行：
 * 1. 如果 key 不存在，创建并设置初始计数和 TTL
 * 2. 如果 key 存在，递增计数
 * 3. 返回当前计数和剩余 permits
 * <p>
 * 判断逻辑：当前计数 <= permits 则允许，否则拒绝。
 *
 * @author Andy Yang
 */
public class RedisRateLimiter implements RateLimiter {

    private static final String RATE_LIMIT_PREFIX = "framework:rate-limit:";

    /**
     * 限流 Lua 脅脚本
     * <p>
     * 参数：KEYS[1] = 限流 key，ARGV[1] = requested，ARGV[2] = limit，ARGV[3] = period（秒）
     * <p>
     * 逻辑：
     * 1. 读取当前计数
     * 2. 如果 current + requested 超过 limit，拒绝且不递增
     * 3. 否则使用 INCRBY 原子消耗 requested 个许可
     * 4. 如果是首次创建，设置 TTL 为 period 秒
     */
    private static final String RATE_LIMIT_SCRIPT =
            "local requested = tonumber(ARGV[1]) " +
            "local limit = tonumber(ARGV[2]) " +
            "local period = tonumber(ARGV[3]) " +
            "local current = tonumber(redis.call('get', KEYS[1]) or '0') " +
            "if current + requested > limit then " +
            "  return 0 " +
            "end " +
            "local updated = redis.call('incrby', KEYS[1], requested) " +
            "if current == 0 then " +
            "  redis.call('expire', KEYS[1], period) " +
            "end " +
            "return updated";

    /**
     * 查询剩余 permits 的 Lua 脅脚本
     * <p>
     * 参数：KEYS[1] = 限流 key，ARGV[1] = permits
     * <p>
     * 逻辑：获取当前计数，返回 permits - current（若 key 不存在返回 permits）
     */
    private static final String AVAILABLE_PERMITS_SCRIPT =
            "local current = redis.call('get', KEYS[1]) " +
            "if current == false then " +
            "  return tonumber(ARGV[1]) " +
            "end " +
            "local remaining = tonumber(ARGV[1]) - tonumber(current) " +
            "if remaining < 0 then return 0 end " +
            "return remaining";

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties.RateLimitProperties rateLimitProperties;

    private final DefaultRedisScript<Long> rateLimitScript;
    private final DefaultRedisScript<Long> availablePermitsScript;

    /**
     * 创建 RedisRateLimiter 实例
     *
     * @param redisTemplate        Redis 操作模板
     * @param rateLimitProperties  限流配置属性
     */
    public RedisRateLimiter(StringRedisTemplate redisTemplate,
                            RedisProperties.RateLimitProperties rateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimitScript = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
        this.availablePermitsScript = new DefaultRedisScript<>(AVAILABLE_PERMITS_SCRIPT, Long.class);
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1, rateLimitProperties.getDefaultPermits(), rateLimitProperties.getDefaultPeriod());
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        return tryAcquire(key, permits, rateLimitProperties.getDefaultPermits(), rateLimitProperties.getDefaultPeriod());
    }

    @Override
    public boolean tryAcquire(String key, int permits, long period) {
        return tryAcquire(key, 1, permits, period);
    }

    private boolean tryAcquire(String key, int requestedPermits, int limit, long period) {
        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        Long current = redisTemplate.execute(rateLimitScript,
                Collections.singletonList(rateLimitKey),
                String.valueOf(requestedPermits), String.valueOf(limit), String.valueOf(period));
        return isAllowed(current, limit);
    }

    @Override
    public long acquire(String key) {
        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        int permits = rateLimitProperties.getDefaultPermits();
        long period = rateLimitProperties.getDefaultPeriod();
        Long current = redisTemplate.execute(rateLimitScript,
                Collections.singletonList(rateLimitKey),
                "1", String.valueOf(permits), String.valueOf(period));
        if (isAllowed(current, permits)) {
            return 0L;
        }
        // 返回大致等待时间：剩余窗口时间的比例
        Long ttl = redisTemplate.getExpire(rateLimitKey);
        return ttl != null && ttl > 0 ? ttl * 1000L : period * 1000L;
    }

    @Override
    public long getAvailablePermits(String key) {
        String rateLimitKey = RATE_LIMIT_PREFIX + key;
        Long remaining = redisTemplate.execute(availablePermitsScript,
                Collections.singletonList(rateLimitKey),
                String.valueOf(rateLimitProperties.getDefaultPermits()));
        return remaining != null ? remaining : rateLimitProperties.getDefaultPermits();
    }

    private boolean isAllowed(Long current, int limit) {
        return current != null && current > 0 && current <= limit;
    }
}
