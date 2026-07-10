package com.microservice.framework.async.api;

import com.microservice.framework.async.AsyncProperties;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置档案
 * <p>
 * 不可变配置对象，封装线程池的核心参数和拒绝策略。
 * 通过 {@link AsyncProperties.PoolProperties} 创建，
 * 也可通过 {@link #of} 静态工厂方法自定义创建。
 * <p>
 * 配置档案用于：
 * - 创建 {@link java.util.concurrent.ThreadPoolExecutor} 时提供参数
 * - 在监控面板中展示线程池配置
 * - 比较不同线程池的配置差异
 *
 * @author Andy Yang
 */
public final class ThreadPoolProfile {

    private final int coreSize;
    private final int maxSize;
    private final int queueCapacity;
    private final String threadNamePrefix;
    private final AsyncProperties.RejectionPolicy rejectionPolicy;

    /**
     * 从 {@link AsyncProperties.PoolProperties} 创建线程池配置档案
     *
     * @param poolProperties 线程池配置属性
     * @return 线程池配置档案
     */
    public static ThreadPoolProfile of(AsyncProperties.PoolProperties poolProperties) {
        return new ThreadPoolProfile(
                poolProperties.getCoreSize(),
                poolProperties.getMaxSize(),
                poolProperties.getQueueCapacity(),
                poolProperties.getThreadNamePrefix(),
                poolProperties.getRejection());
    }

    /**
     * 通过静态工厂方法创建自定义线程池配置档案
     *
     * @param coreSize         核心线程数
     * @param maxSize          最大线程数
     * @param queueCapacity    队列容量
     * @param threadNamePrefix 线程名前缀
     * @param rejectionPolicy  拒绝策略
     * @return 线程池配置档案
     */
    public static ThreadPoolProfile of(int coreSize, int maxSize, int queueCapacity,
                                       String threadNamePrefix,
                                       AsyncProperties.RejectionPolicy rejectionPolicy) {
        validate(coreSize, maxSize, queueCapacity, threadNamePrefix);
        return new ThreadPoolProfile(coreSize, maxSize, queueCapacity, threadNamePrefix, rejectionPolicy);
    }

    private ThreadPoolProfile(int coreSize, int maxSize, int queueCapacity,
                              String threadNamePrefix,
                              AsyncProperties.RejectionPolicy rejectionPolicy) {
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.queueCapacity = queueCapacity;
        this.threadNamePrefix = threadNamePrefix;
        this.rejectionPolicy = rejectionPolicy;
    }

    private static void validate(int coreSize, int maxSize, int queueCapacity, String threadNamePrefix) {
        if (coreSize < 1) {
            throw new IllegalArgumentException("coreSize must be at least 1, but was: " + coreSize);
        }
        if (maxSize < coreSize) {
            throw new IllegalArgumentException("maxSize must be at least coreSize (" + coreSize + "), but was: " + maxSize);
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must be at least 0, but was: " + queueCapacity);
        }
        if (threadNamePrefix == null || threadNamePrefix.isBlank()) {
            throw new IllegalArgumentException("threadNamePrefix must not be null or blank");
        }
    }

    /**
     * 获取核心线程数
     *
     * @return 核心线程数
     */
    public int getCoreSize() {
        return coreSize;
    }

    /**
     * 获取最大线程数
     *
     * @return 最大线程数
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * 获取队列容量
     *
     * @return 队列容量
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * 获取线程名前缀
     *
     * @return 线程名前缀
     */
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * 获取拒绝策略枚举
     *
     * @return 拒绝策略
     */
    public AsyncProperties.RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    /**
     * 将拒绝策略枚举转换为 JDK {@link RejectedExecutionHandler}
     *
     * @return 对应的 RejectedExecutionHandler 实现
     */
    public RejectedExecutionHandler toRejectedExecutionHandler() {
        switch (rejectionPolicy) {
            case ABORT:
                return new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS:
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD_OLDEST:
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                throw new IllegalStateException("Unknown rejection policy: " + rejectionPolicy);
        }
    }

    @Override
    public String toString() {
        return "ThreadPoolProfile{coreSize=" + coreSize
                + ", maxSize=" + maxSize
                + ", queueCapacity=" + queueCapacity
                + ", threadNamePrefix='" + threadNamePrefix + "'"
                + ", rejectionPolicy=" + rejectionPolicy
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThreadPoolProfile)) {
            return false;
        }
        ThreadPoolProfile that = (ThreadPoolProfile) o;
        return coreSize == that.coreSize
                && maxSize == that.maxSize
                && queueCapacity == that.queueCapacity
                && threadNamePrefix.equals(that.threadNamePrefix)
                && rejectionPolicy == that.rejectionPolicy;
    }

    @Override
    public int hashCode() {
        int result = coreSize;
        result = 31 * result + maxSize;
        result = 31 * result + queueCapacity;
        result = 31 * result + threadNamePrefix.hashCode();
        result = 31 * result + rejectionPolicy.hashCode();
        return result;
    }
}
