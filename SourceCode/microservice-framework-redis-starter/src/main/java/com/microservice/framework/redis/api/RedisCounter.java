package com.microservice.framework.redis.api;

/**
 * 原子计数器 SPI 接口
 * <p>
 * 定义分布式原子计数器的标准契约，基于 Redis INCR/DECR 实现。
 * <p>
 * 应用可通过 Spring SPI 替换默认实现，只需注册一个
 * 实现 {@link RedisCounter} 的 Bean 即可覆盖默认配置。
 *
 * @author Andy Yang
 */
public interface RedisCounter {

    /**
     * 递增计数器，步长为 1
     *
     * @param key 计数器标识键
     * @return 递增后的值
     */
    long increment(String key);

    /**
     * 递增计数器，指定步长
     *
     * @param key   计数器标识键
     * @param delta 递增量，必须为正数
     * @return 递增后的值
     */
    long increment(String key, long delta);

    /**
     * 递减计数器，步长为 1
     *
     * @param key 计数器标识键
     * @return 递减后的值
     */
    long decrement(String key);

    /**
     * 递减计数器，指定步长
     *
     * @param key   计数器标识键
     * @param delta 递减量，必须为正数
     * @return 递减后的值
     */
    long decrement(String key, long delta);

    /**
     * 获取计数器当前值
     *
     * @param key 计数器标识键
     * @return 当前计数值，若键不存在返回 0
     */
    long get(String key);

    /**
     * 重置计数器为指定值
     *
     * @param key   计数器标识键
     * @param value 重置后的值
     * @return 重置后的值
     */
    long reset(String key, long value);

    /**
     * 递增计数器并返回递增前的值
     *
     * @param key 计数器标识键
     * @return 递增前的值
     */
    long getAndIncrement(String key);
}
