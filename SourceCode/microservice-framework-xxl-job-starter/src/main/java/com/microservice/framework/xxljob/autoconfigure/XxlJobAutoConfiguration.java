package com.microservice.framework.xxljob.autoconfigure;

import com.microservice.framework.xxljob.XxlJobProperties;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import com.microservice.framework.xxljob.api.JobExecutionContext;
import com.microservice.framework.xxljob.api.JobHandler;
import com.microservice.framework.xxljob.api.JobResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * XXL-JOB Starter 自动配置
 * <p>
 * 根据 {@code framework.xxl-job.enabled} 属性决定是否激活，默认启用。
 * 仅当 {@code com.xxl.job.core.handler.IJobHandler} 类在 classpath 中时激活。
 * <p>
 * 注册以下 Bean：
 * - {@link IdempotentJobHandler}：幂等任务处理器装饰器（当 idempotency.enabled=true）
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnClass(name = "com.xxl.job.core.handler.IJobHandler")
@ConditionalOnProperty(prefix = "framework.xxl-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfiguration {

    /**
     * 注册默认 IdempotentJobHandler Bean
     * <p>
     * 仅当 {@code framework.xxl-job.idempotency.enabled=true} 时激活。
     * 提供基于内存的幂等检查实现。
     * <p>
     * 注意：此 Bean 是一个装饰器工厂，业务可通过
     * {@link IdempotentJobHandler#execute(JobExecutionContext)} 执行幂等保护的任务。
     * 业务需自行注册 {@link JobHandler} 实现并通过此装饰器包装。
     *
     * @param properties XXL-JOB 配置属性
     * @return IdempotentJobHandler 实例（基于内存存储）
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentJobHandler.class)
    @ConditionalOnProperty(prefix = "framework.xxl-job.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentJobHandler idempotentJobHandler(XxlJobProperties properties) {
        return new MemoryIdempotentJobHandler(new MemoryIdempotencyStore(), properties);
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 ConcurrentHashMap 的内存幂等存储
     * <p>
     * 适用于单实例部署场景。生产环境应覆盖为 Redis 或数据库实现。
     */
    static class MemoryIdempotencyStore {

        private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

        /**
         * 判断任务是否为重复执行
         *
         * @param key 任务唯一标识
         * @return true 表示已处理过
         */
        public boolean isDuplicate(String key) {
            return processedKeys.contains(key);
        }

        /**
         * 标记任务为已执行
         *
         * @param key 任务唯一标识
         */
        public void markProcessed(String key) {
            processedKeys.add(key);
        }

        /**
         * 生成任务唯一标识
         *
         * @param context 任务执行上下文
         * @return jobId + "-" + executorId 组合
         */
        public String buildKey(JobExecutionContext context) {
            return context.getJobId() + "-" + context.getExecutorId();
        }
    }

    /**
     * 基于内存存储的幂等任务处理器装饰器
     * <p>
     * 实现 {@link IdempotentJobHandler} 接口，在 {@link JobHandler} 基础上
     * 增加 jobId + executorId 的幂等保护。
     */
    static class MemoryIdempotentJobHandler implements IdempotentJobHandler {

        private final MemoryIdempotencyStore store;
        private final XxlJobProperties properties;

        MemoryIdempotentJobHandler(MemoryIdempotencyStore store, XxlJobProperties properties) {
            this.store = store;
            this.properties = properties;
        }

        @Override
        public boolean isDuplicate(JobExecutionContext context) {
            String key = store.buildKey(context);
            return store.isDuplicate(key);
        }

        @Override
        public void markProcessed(JobExecutionContext context) {
            String key = store.buildKey(context);
            store.markProcessed(key);
        }

        @Override
        public JobResult execute(JobExecutionContext context) {
            // 幂等检查：若任务已执行过，直接返回成功
            if (isDuplicate(context)) {
                return JobResult.success("Idempotent check: duplicate execution skipped for jobId=" + context.getJobId());
            }

            // 此处作为默认实现，不做实际业务执行
            // 业务应注册自己的 JobHandler 并通过 IdempotentJobHandler 装饰
            return JobResult.success();
        }

        @Override
        public JobHandler getDelegate() {
            // 默认装饰器无 delegate，业务通过自定义装饰器包装实际 JobHandler
            return null;
        }
    }
}
