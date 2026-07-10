package com.microservice.framework.redis.core;

import com.microservice.framework.redis.api.RedisCounter;
import com.microservice.framework.redis.RedisProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

/**
 * Redis 原子计数器默认实现
 * <p>
 * 基于 Redis INCR/DECR/GETSET 命令实现，保证原子性。
 * <p>
 * 所有计数器键使用 {@code framework:counter:} 前缀，
 * 与其他 Redis 数据隔离。
 *
 * @author Andy Yang
 */
public class RedisCounterImpl implements RedisCounter {

    private static final String COUNTER_PREFIX = "framework:counter:";

    /**
     * getAndIncrement Lua 脅脚本
     * <p>
     * 逻辑：先 GET 获取当前值，再 INCR 递增，返回递增前的值。
     * 使用 Lua 保证原子性。
     */
    private static final String GET_AND_INCREMENT_SCRIPT =
            "local current = redis.call('get', KEYS[1]) " +
            "if current == false then " +
            "  current = '0' " +
            "end " +
            "redis.call('incr', KEYS[1]) " +
            "return tonumber(current)";

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties.CounterProperties counterProperties;

    private final DefaultRedisScript<Long> getAndIncrementScript;

    /**
     * 创建 RedisCounterImpl 实例
     *
     * @param redisTemplate      Redis 操作模板
     * @param counterProperties  计数器配置属性
     */
    public RedisCounterImpl(StringRedisTemplate redisTemplate,
                            RedisProperties.CounterProperties counterProperties) {
        this.redisTemplate = redisTemplate;
        this.counterProperties = counterProperties;
        this.getAndIncrementScript = new DefaultRedisScript<>(GET_AND_INCREMENT_SCRIPT, Long.class);
    }

    @Override
    public long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public long increment(String key, long delta) {
        requirePositiveDelta(delta);
        String counterKey = COUNTER_PREFIX + key;
        return redisTemplate.opsForValue().increment(counterKey, delta);
    }

    @Override
    public long decrement(String key) {
        return decrement(key, 1);
    }

    @Override
    public long decrement(String key, long delta) {
        requirePositiveDelta(delta);
        String counterKey = COUNTER_PREFIX + key;
        return redisTemplate.opsForValue().increment(counterKey, -delta);
    }

    @Override
    public long get(String key) {
        String counterKey = COUNTER_PREFIX + key;
        String value = redisTemplate.opsForValue().get(counterKey);
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    @Override
    public long reset(String key, long value) {
        String counterKey = COUNTER_PREFIX + key;
        redisTemplate.opsForValue().set(counterKey, String.valueOf(value));
        return value;
    }

    @Override
    public long getAndIncrement(String key) {
        String counterKey = COUNTER_PREFIX + key;
        Long previous = redisTemplate.execute(getAndIncrementScript,
                Collections.singletonList(counterKey));
        return previous != null ? previous : 0L;
    }

    private void requirePositiveDelta(long delta) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be positive");
        }
    }
}
