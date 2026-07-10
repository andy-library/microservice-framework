package com.microservice.framework.redis.core;

import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.RedisProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存默认实现
 * <p>
 * 基于 {@link RedisTemplate} 实现，提供 TTL 管理和 null 值缓存防穿透能力。
 * <p>
 * null 值缓存：当缓存值为 null 时，写入一个特殊标记，
 * 使用较短的 TTL（由 {@code framework.redis.cache.nullValueTtl} 控制），
 * 防止缓存穿透攻击。
 *
 * @author Andy Yang
 */
public class RedisCacheImpl implements RedisCache {

    private static final String CACHE_PREFIX = "framework:cache:";
    private static final String NULL_VALUE_MARKER = "##NULL##";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisProperties.CacheProperties cacheProperties;

    /**
     * 创建 RedisCacheImpl 实例
     *
     * @param redisTemplate     Redis 操作模板
     * @param cacheProperties   缓存配置属性
     */
    public RedisCacheImpl(RedisTemplate<String, Object> redisTemplate,
                         RedisProperties.CacheProperties cacheProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public <T> T get(String key) {
        requireKey(key);
        String cacheKey = CACHE_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return null;
        }
        if (NULL_VALUE_MARKER.equals(value)) {
            return null;
        }
        return (T) value;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        requireKey(key);
        Objects.requireNonNull(type, "type must not be null");
        String cacheKey = CACHE_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return null;
        }
        if (NULL_VALUE_MARKER.equals(value)) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new IllegalStateException("Cached value type mismatch: expected " + type.getName()
                + " but was " + value.getClass().getName());
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, cacheProperties.getDefaultTtl());
    }

    @Override
    public void put(String key, Object value, long ttl) {
        requireKey(key);
        if (ttl <= 0) {
            throw new IllegalArgumentException("TTL must be positive");
        }
        String cacheKey = CACHE_PREFIX + key;
        if (value == null) {
            // null 值使用较短 TTL 防穿透
            redisTemplate.opsForValue().set(cacheKey, NULL_VALUE_MARKER,
                    cacheProperties.getNullValueTtl(), TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(cacheKey, value, ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean evict(String key) {
        requireKey(key);
        String cacheKey = CACHE_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.delete(cacheKey));
    }

    @Override
    public void evictAll(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(this::requireKey);
        List<String> cacheKeys = keys.stream()
                .map(key -> CACHE_PREFIX + key)
                .toList();
        redisTemplate.delete(cacheKeys);
    }

    @Override
    public long evictByPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("pattern must not be blank");
        }
        if ("*".equals(pattern) || "**".equals(pattern)) {
            throw new IllegalArgumentException("global wildcard pattern is not allowed");
        }
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(CACHE_PREFIX + pattern)
                .count(cacheProperties.getScanBatchSize())
                .build();
        try (var cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        if (keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0L : deleted;
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }
}
