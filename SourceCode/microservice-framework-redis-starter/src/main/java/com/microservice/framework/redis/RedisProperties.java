package com.microservice.framework.redis;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Redis Starter 配置属性
 * <p>
 * 聚合分布式锁、限流、原子计数器、缓存等配置组，
 * 所有属性前缀为 {@code framework.redis}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.redis")
public class RedisProperties {

    /**
     * 分布式锁配置
     */
    @NestedConfigurationProperty
    private LockProperties lock = new LockProperties();

    /**
     * 限流配置
     */
    @NestedConfigurationProperty
    private RateLimitProperties rateLimit = new RateLimitProperties();

    /**
     * 原子计数器配置
     */
    @NestedConfigurationProperty
    private CounterProperties counter = new CounterProperties();

    /**
     * 缓存配置
     */
    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    // Getters and Setters

    public LockProperties getLock() {
        return lock;
    }

    public void setLock(LockProperties lock) {
        this.lock = lock;
    }

    public RateLimitProperties getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitProperties rateLimit) {
        this.rateLimit = rateLimit;
    }

    public CounterProperties getCounter() {
        return counter;
    }

    public void setCounter(CounterProperties counter) {
        this.counter = counter;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    /**
     * 分布式锁配置
     */
    public static class LockProperties {

        /**
         * 默认获取锁超时时间（毫秒），默认 3000
         * <p>
         * 当调用 {@code tryLock(key)} 不指定超时时，使用此默认值。
         */
        @Min(0)
        private long defaultTimeout = 3000L;

        /**
         * 默认锁持有时间（毫秒），默认 30000
         * <p>
         * 锁自动过期时间，防止死锁。当调用 {@code tryLock(key)} 不指定过期时间时，使用此默认值。
         */
        @Min(0)
        private long defaultExpire = 30000L;

        // Getters and Setters

        public long getDefaultTimeout() {
            return defaultTimeout;
        }

        public void setDefaultTimeout(long defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        public long getDefaultExpire() {
            return defaultExpire;
        }

        public void setDefaultExpire(long defaultExpire) {
            this.defaultExpire = defaultExpire;
        }
    }

    /**
     * 限流配置
     */
    public static class RateLimitProperties {

        /**
         * 默认 permits 数量，默认 100
         * <p>
         * 每个时间窗口内允许的最大请求次数。
         */
        @Min(1)
        private int defaultPermits = 100;

        /**
         * 默认时间窗口（秒），默认 1
         * <p>
         * 限流周期，在此时长内最多允许 {@code defaultPermits} 次请求。
         */
        @Min(1)
        private long defaultPeriod = 1L;

        // Getters and Setters

        public int getDefaultPermits() {
            return defaultPermits;
        }

        public void setDefaultPermits(int defaultPermits) {
            this.defaultPermits = defaultPermits;
        }

        public long getDefaultPeriod() {
            return defaultPeriod;
        }

        public void setDefaultPeriod(long defaultPeriod) {
            this.defaultPeriod = defaultPeriod;
        }
    }

    /**
     * 原子计数器配置
     */
    public static class CounterProperties {

        /**
         * 计数器默认初始值，默认 0
         */
        private long defaultInitialValue = 0L;

        // Getters and Setters

        public long getDefaultInitialValue() {
            return defaultInitialValue;
        }

        public void setDefaultInitialValue(long defaultInitialValue) {
            this.defaultInitialValue = defaultInitialValue;
        }
    }

    /**
     * 缓存配置
     */
    public static class CacheProperties {

        /**
         * 是否启用缓存增强，默认 true
         * <p>
         * 启用后将注册 {@code RedisCache} Bean，提供 TTL 管理和 null 值缓存等增强功能。
         */
        @NotNull
        private Boolean enabled = true;

        /**
         * 默认缓存 TTL（秒），默认 3600
         * <p>
         * 当 {@code put} 不指定 TTL 时，使用此默认过期时间。
         */
        @Min(0)
        private long defaultTtl = 3600L;

        /**
         * null 值缓存 TTL（秒），默认 60
         * <p>
         * 为防止缓存穿透，null 值会被缓存较短时间。
         */
        @Min(0)
        private long nullValueTtl = 60L;

        /**
         * SCAN 批量大小，默认 500。
         * <p>
         * 用于批量删除/失效类操作时控制单次扫描数量，避免生产环境大批量操作压垮 Redis。
         */
        @Min(1)
        private long scanBatchSize = 500L;

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public long getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(long defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public long getNullValueTtl() {
            return nullValueTtl;
        }

        public void setNullValueTtl(long nullValueTtl) {
            this.nullValueTtl = nullValueTtl;
        }

        public long getScanBatchSize() {
            return scanBatchSize;
        }

        public void setScanBatchSize(long scanBatchSize) {
            this.scanBatchSize = scanBatchSize;
        }
    }
}
