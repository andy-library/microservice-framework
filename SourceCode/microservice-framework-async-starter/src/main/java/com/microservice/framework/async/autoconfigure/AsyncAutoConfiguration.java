package com.microservice.framework.async.autoconfigure;

import com.microservice.framework.async.AsyncProperties;
import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.async.api.ContextPropagatingTaskDecorator;
import com.microservice.framework.async.api.ThreadPoolProfile;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async Starter 自动配置
 * <p>
 * 根据 {@code framework.async.enabled} 属性决定是否激活，默认启用。
 * 注册以下 Bean：
 * - {@link ThreadLocalContextAdapter}：当容器中不存在时自动创建（上下文传播基础）
 * - {@link AsyncTaskExecutor}：治理线程池执行器，带监控能力
 * - {@link ContextPropagatingTaskDecorator}：上下文传播装饰器（当 context.propagationEnabled=true）
 * - {@link ThreadPoolProfile}：线程池配置档案
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(AsyncProperties.class)
@ConditionalOnProperty(prefix = "framework.async", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncAutoConfiguration {

    /**
     * 当容器中不存在 ThreadLocalContextAdapter 时自动创建
     * <p>
     * Common Starter 的 CommonContextAutoConfiguration 也会注册此 Bean，
     * 此处作为兜底，确保上下文传播始终可用。
     *
     * @return ThreadLocalContextAdapter 实例
     */
    @Bean
    @ConditionalOnMissingBean(ThreadLocalContextAdapter.class)
    public ThreadLocalContextAdapter threadLocalContextAdapter() {
        return new ThreadLocalContextAdapter();
    }

    /**
     * 注册 ThreadPoolProfile Bean
     *
     * @param properties Async 配置属性
     * @return ThreadPoolProfile 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolProfile threadPoolProfile(AsyncProperties properties) {
        return ThreadPoolProfile.of(properties.getPool());
    }

    /**
     * 注册 AsyncTaskExecutor Bean
     * <p>
     * 基于 {@link ThreadPoolTaskExecutor} 实现，配置线程池参数、拒绝策略和优雅关停。
     * 当 {@code context.propagationEnabled=true} 且 {@link ThreadLocalContextAdapter} 可用时
     * 自动附加上下文传播装饰器。
     *
     * @param profile        线程池配置档案
     * @param properties     Async 配置属性
     * @param contextAdapter ThreadLocal 上下文适配器（可为 null，当上下文传播禁用或适配器不存在）
     * @return AsyncTaskExecutor 实例
     */
    @Bean
    @ConditionalOnMissingBean(AsyncTaskExecutor.class)
    public AsyncTaskExecutor asyncTaskExecutor(ThreadPoolProfile profile,
                                                AsyncProperties properties,
                                                @Nullable @Autowired ThreadLocalContextAdapter contextAdapter) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(profile.getCoreSize());
        executor.setMaxPoolSize(profile.getMaxSize());
        executor.setQueueCapacity(profile.getQueueCapacity());
        executor.setThreadNamePrefix(profile.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(profile.toRejectedExecutionHandler());

        // 优雅关停配置
        AsyncProperties.ShutdownProperties shutdown = properties.getShutdown();
        executor.setWaitForTasksToCompleteOnShutdown(shutdown.getAwaitTermination());
        executor.setAwaitTerminationSeconds(shutdown.getAwaitTerminationSeconds());

        // 上下文传播装饰器
        if (properties.getContext().getPropagationEnabled() && contextAdapter != null) {
            executor.setTaskDecorator(new ContextPropagatingTaskDecorator(contextAdapter));
        }

        executor.initialize();

        return new DefaultAsyncTaskExecutor(executor);
    }

    /**
     * 注册 ContextPropagatingTaskDecorator Bean
     * <p>
     * 仅当 {@code framework.async.context.propagation-enabled=true} 时激活。
     *
     * @param contextAdapter ThreadLocal 上下文适配器
     * @return ContextPropagatingTaskDecorator 实例
     */
    @Bean
    @ConditionalOnMissingBean(ContextPropagatingTaskDecorator.class)
    @ConditionalOnProperty(prefix = "framework.async.context", name = "propagation-enabled", havingValue = "true", matchIfMissing = true)
    public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator(ThreadLocalContextAdapter contextAdapter) {
        return new ContextPropagatingTaskDecorator(contextAdapter);
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 ThreadPoolTaskExecutor 的默认 AsyncTaskExecutor 实现
     */
    static class DefaultAsyncTaskExecutor implements AsyncTaskExecutor {

        private final ThreadPoolTaskExecutor delegate;

        DefaultAsyncTaskExecutor(ThreadPoolTaskExecutor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable task) {
            delegate.execute(task);
        }

        @Override
        public int getActiveCount() {
            return delegate.getActiveCount();
        }

        @Override
        public int getPoolSize() {
            return delegate.getPoolSize();
        }

        @Override
        public int getQueueSize() {
            return delegate.getThreadPoolExecutor().getQueue().size();
        }

        @Override
        public long getCompletedTaskCount() {
            return delegate.getThreadPoolExecutor().getCompletedTaskCount();
        }
    }
}
