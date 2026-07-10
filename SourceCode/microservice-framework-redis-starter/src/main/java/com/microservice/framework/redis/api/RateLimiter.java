package com.microservice.framework.redis.api;

/**
 * 限流器 SPI 接口
 * <p>
 * 定义分布式限流的标准契约，基于 Redis 固定窗口算法实现。
 * <p>
 * 应用可通过 Spring SPI 替换默认实现，只需注册一个
 * 实现 {@link RateLimiter} 的 Bean 即可覆盖默认配置。
 *
 * @author Andy Yang
 */
public interface RateLimiter {

    /**
     * 尝试获取一个许可，使用默认限流配置
     * <p>
     * permits 和 period 由 {@code framework.redis.rate-limit.defaultPermits}
     * 和 {@code framework.redis.rate-limit.defaultPeriod} 配置决定。
     *
     * @param key 限流标识键
     * @return 是否成功获取许可
     */
    boolean tryAcquire(String key);

    /**
     * 尝试获取指定数量的许可，使用默认限流配置
     *
     * @param key    限流标识键
     * @param permits 需要获取的许可数量
     * @return 是否成功获取所有许可
     */
    boolean tryAcquire(String key, int permits);

    /**
     * 尝试获取一个许可，指定限流参数
     *
     * @param key    限流标识键
     * @param permits 时间窗口内允许的最大请求数
     * @param period 时间窗口长度（秒）
     * @return 是否成功获取许可
     */
    boolean tryAcquire(String key, int permits, long period);

    /**
     * 获取一个许可，若当前窗口已满则等待直到下一个窗口
     *
     * @param key 限流标识键
     * @return 等待时间（毫秒），0 表示立即获取
     */
    long acquire(String key);

    /**
     * 获取当前时间窗口内剩余的可用许可数
     *
     * @param key 限流标识键
     * @return 剩余可用许可数
     */
    long getAvailablePermits(String key);
}
