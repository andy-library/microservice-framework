package com.microservice.framework.redis.api;

import java.util.List;

/**
 * Redis 缓存 SPI 接口
 * <p>
 * 定义 Redis 缓存操作的标准契约，提供 TTL 管理、null 值缓存等增强功能。
 * <p>
 * 应用可通过 Spring SPI 替换默认实现，只需注册一个
 * 实现 {@link RedisCache} 的 Bean 即可覆盖默认配置。
 *
 * @author Andy Yang
 */
public interface RedisCache {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，若键不存在返回 null
     */
    <T> T get(String key);

    /**
     * 获取缓存值，指定目标类型
     *
     * @param key   缓存键
     * @param type  目标类型
     * @return 缓存值，若键不存在返回 null
     */
    <T> T get(String key, Class<T> type);

    /**
     * 写入缓存，使用默认 TTL
     * <p>
     * TTL 由 {@code framework.redis.cache.defaultTtl} 配置决定。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 写入缓存，指定 TTL
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间（秒）
     */
    void put(String key, Object value, long ttl);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否成功删除（键存在并被删除时返回 true）
     */
    boolean evict(String key);

    /**
     * 批量删除缓存。
     *
     * @param keys 缓存键集合
     */
    void evictAll(List<String> keys);

    /**
     * 按 glob pattern 删除缓存，生产实现必须使用 SCAN 类方式，禁止 KEYS 全量扫描。
     *
     * @param pattern 缓存键 pattern
     * @return 删除数量
     */
    long evictByPattern(String pattern);
}
