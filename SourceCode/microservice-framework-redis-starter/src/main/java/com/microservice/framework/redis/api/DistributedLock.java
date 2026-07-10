package com.microservice.framework.redis.api;

/**
 * 分布式锁 SPI 接口
 * <p>
 * 定义分布式锁的标准契约，基于 Redis SETEX 实现。
 * <p>
 * 应用可通过 Spring SPI 替换默认实现，只需注册一个
 * 实现 {@link DistributedLock} 的 Bean 即可覆盖默认配置。
 *
 * @author Andy Yang
 */
public interface DistributedLock {

    /**
     * 尝试获取分布式锁，使用默认超时和过期时间
     * <p>
     * 超时时间和锁持有时间由 {@code framework.redis.lock.defaultTimeout}
     * 和 {@code framework.redis.lock.defaultExpire} 配置决定。
     *
     * @param key 锁的标识键
     * @return 是否成功获取锁
     */
    boolean tryLock(String key);

    /**
     * 尝试获取分布式锁，指定获取超时时间，使用默认过期时间
     *
     * @param key     锁的标识键
     * @param timeout 获取锁的最大等待时间（毫秒），0 表示不等待
     * @return 是否成功获取锁
     */
    boolean tryLock(String key, long timeout);

    /**
     * 尝试获取分布式锁，指定获取超时时间和锁持有时间
     *
     * @param key     锁的标识键
     * @param timeout 获取锁的最大等待时间（毫秒），0 表示不等待
     * @param expire  锁的自动过期时间（毫秒），防止死锁
     * @return 是否成功获取锁
     */
    boolean tryLock(String key, long timeout, long expire);

    /**
     * 释放分布式锁
     * <p>
     * 仅释放由当前持有者获取的锁，非持有者释放无效。
     *
     * @param key 锁的标识键
     * @return 是否成功释放锁
     */
    boolean unlock(String key);

    /**
     * 检查锁是否已被持有
     *
     * @param key 锁的标识键
     * @return 锁是否处于锁定状态
     */
    boolean isLocked(String key);
}
