package com.microservice.framework.async;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Async Starter 配置属性
 * <p>
 * 聚合线程池、上下文传播和优雅关停配置组，
 * 所有属性前缀为 {@code framework.async}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.async")
public class AsyncProperties {

    /**
     * 线程池配置
     */
    @NestedConfigurationProperty
    private PoolProperties pool = new PoolProperties();

    /**
     * 上下文传播配置
     */
    @NestedConfigurationProperty
    private ContextProperties context = new ContextProperties();

    /**
     * 优雅关停配置
     */
    @NestedConfigurationProperty
    private ShutdownProperties shutdown = new ShutdownProperties();

    // Getters and Setters

    public PoolProperties getPool() {
        return pool;
    }

    public void setPool(PoolProperties pool) {
        this.pool = pool;
    }

    public ContextProperties getContext() {
        return context;
    }

    public void setContext(ContextProperties context) {
        this.context = context;
    }

    public ShutdownProperties getShutdown() {
        return shutdown;
    }

    public void setShutdown(ShutdownProperties shutdown) {
        this.shutdown = shutdown;
    }

    /**
     * 线程池配置
     */
    public static class PoolProperties {

        /**
         * 核心线程数，默认 4
         * <p>
         * 线程池常驻线程数量，即使空闲也不会被回收。
         */
        @Min(1)
        private Integer coreSize = 4;

        /**
         * 最大线程数，默认 8
         * <p>
         * 线程池允许的最大线程数量，当队列满后创建新线程直到此上限。
         */
        @Min(1)
        private Integer maxSize = 8;

        /**
         * 队列容量，默认 256
         * <p>
         * 当核心线程全部忙碌时，新任务先进入等待队列。
         * 队列满后才会创建额外线程（直到 maxSize）。
         */
        @Min(0)
        private Integer queueCapacity = 256;

        /**
         * 线程名前缀，默认 "async-"
         * <p>
         * 便于在日志和监控中识别异步线程池线程。
         */
        @NotBlank
        private String threadNamePrefix = "async-";

        /**
         * 拒绝策略，默认 CALLER_RUNS
         * <p>
         * 当线程池和队列都满时的新任务处理策略：
         * - ABORT：直接抛出 RejectedExecutionException
         * - CALLER_RUNS：由提交任务的线程自行执行
         * - DISCARD_OLDEST：丢弃队列中最旧的任务
         */
        private RejectionPolicy rejection = RejectionPolicy.CALLER_RUNS;

        // Getters and Setters

        public Integer getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(Integer coreSize) {
            this.coreSize = coreSize;
        }

        public Integer getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(Integer maxSize) {
            this.maxSize = maxSize;
        }

        public Integer getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(Integer queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public RejectionPolicy getRejection() {
            return rejection;
        }

        public void setRejection(RejectionPolicy rejection) {
            this.rejection = rejection;
        }
    }

    /**
     * 上下文传播配置
     */
    public static class ContextProperties {

        /**
         * 是否启用上下文传播，默认 true
         * <p>
         * 启用后，异步线程将自动继承父线程的 FrameworkContext，
         * 确保请求级上下文（如 requestId、userId）跨线程传播。
         */
        private Boolean propagationEnabled = true;

        // Getters and Setters

        public Boolean getPropagationEnabled() {
            return propagationEnabled;
        }

        public void setPropagationEnabled(Boolean propagationEnabled) {
            this.propagationEnabled = propagationEnabled;
        }
    }

    /**
     * 优雅关停配置
     */
    public static class ShutdownProperties {

        /**
         * 是否等待任务完成后再关停，默认 true
         * <p>
         * 启用后，Spring 容器关闭时将等待线程池中的所有任务执行完毕。
         */
        private Boolean awaitTermination = true;

        /**
         * 最大等待时间（秒），默认 30
         * <p>
         * 超过此时间后强制关停线程池，未完成的任务将被中断。
         */
        @Min(1)
        private Integer awaitTerminationSeconds = 30;

        // Getters and Setters

        public Boolean getAwaitTermination() {
            return awaitTermination;
        }

        public void setAwaitTermination(Boolean awaitTermination) {
            this.awaitTermination = awaitTermination;
        }

        public Integer getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(Integer awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }

    /**
     * 拒绝策略枚举
     */
    public enum RejectionPolicy {
        /**
         * 直接抛出异常
         */
        ABORT,

        /**
         * 由提交任务的线程自行执行
         */
        CALLER_RUNS,

        /**
         * 丢弃队列中最旧的任务
         */
        DISCARD_OLDEST
    }
}
