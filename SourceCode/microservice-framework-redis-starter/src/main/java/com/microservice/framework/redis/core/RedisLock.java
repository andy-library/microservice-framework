package com.microservice.framework.redis.core;

import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.RedisProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁默认实现
 * <p>
 * 基于 Redis SET + Lua 脚本实现，确保获取锁和释放锁的原子性。
 * <p>
 * 获取锁：使用 {@code SET key value NX PX expire} 命令，
 * 其中 value 为 UUID 标识当前持有者，防止误释放他人持有的锁。
 * <p>
 * 释放锁：使用 Lua 装脚本先比较 value 再删除，保证原子性。
 *
 * @author Andy Yang
 */
public class RedisLock implements DistributedLock {

    private static final String LOCK_PREFIX = "framework:lock:";

    /**
     * 释放锁的 Lua 装脚本
     * <p>
     * 逻辑：如果 key 的值等于传入的 value，则删除 key 并返回 1；
     * 否则返回 0，表示当前请求者不是锁持有者，不能释放。
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;
    private final RedisProperties.LockProperties lockProperties;

    private final DefaultRedisScript<Long> unlockScript;

    /**
     * 创建 RedisLock 实例
     *
     * @param redisTemplate  Redis 操作模板
     * @param lockProperties 锁配置属性
     */
    public RedisLock(StringRedisTemplate redisTemplate, RedisProperties.LockProperties lockProperties) {
        this.redisTemplate = redisTemplate;
        this.lockProperties = lockProperties;
        this.unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    }

    @Override
    public boolean tryLock(String key) {
        return tryLock(key, lockProperties.getDefaultTimeout(), lockProperties.getDefaultExpire());
    }

    @Override
    public boolean tryLock(String key, long timeout) {
        return tryLock(key, timeout, lockProperties.getDefaultExpire());
    }

    @Override
    public boolean tryLock(String key, long timeout, long expire) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + timeout;

        while (true) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expire, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            // 短暂等待后重试，避免频繁请求 Redis
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    @Override
    public boolean unlock(String key) {
        String lockKey = LOCK_PREFIX + key;
        // 获取当前锁的值以判断是否为持有者
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (currentValue == null) {
            return false;
        }
        Long result = redisTemplate.execute(unlockScript,
                Collections.singletonList(lockKey), currentValue);
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
}
