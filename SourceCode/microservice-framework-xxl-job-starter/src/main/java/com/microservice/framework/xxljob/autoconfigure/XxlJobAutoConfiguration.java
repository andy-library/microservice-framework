package com.microservice.framework.xxljob.autoconfigure;

import com.microservice.framework.xxljob.XxlJobProperties;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import com.microservice.framework.xxljob.api.JobExecutionContext;
import com.microservice.framework.xxljob.api.JobHandler;
import com.microservice.framework.xxljob.api.JobResult;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    @Bean
    public XxlJobRequiredPropertiesValidator xxlJobRequiredPropertiesValidator(XxlJobProperties properties) {
        return new XxlJobRequiredPropertiesValidator(properties);
    }

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
    @ConditionalOnBean(JobHandler.class)
    @ConditionalOnProperty(prefix = "framework.xxl-job.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentJobHandler idempotentJobHandler(XxlJobProperties properties, JobHandler jobHandler) {
        return new MemoryIdempotentJobHandler(new MemoryIdempotencyStore(), properties, jobHandler);
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 启动期必填配置校验。
     */
    static class XxlJobRequiredPropertiesValidator implements InitializingBean {

        private final XxlJobProperties properties;

        XxlJobRequiredPropertiesValidator(XxlJobProperties properties) {
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() {
            if (!Boolean.TRUE.equals(properties.getEnabled())) {
                return;
            }
            requireText(properties.getAdmin().getAddresses(), "framework.xxl-job.admin.addresses");
            requireText(properties.getAdmin().getAppName(), "framework.xxl-job.admin.app-name");
            requireText(properties.getExecutor().getAppName(), "framework.xxl-job.executor.app-name");
            requirePositive(properties.getExecutor().getPort(), "framework.xxl-job.executor.port");
            requireText(properties.getExecutor().getLogPath(), "framework.xxl-job.executor.log-path");
            requirePositive(properties.getExecutor().getLogRetentionDays(),
                    "framework.xxl-job.executor.log-retention-days");
            requirePositive(properties.getTimeout().getDefaultTimeout(), "framework.xxl-job.timeout.default-timeout");
        }

        private void requireText(String value, String propertyName) {
            if (!StringUtils.hasText(value)) {
                throw new IllegalStateException(propertyName + " must be configured when framework.xxl-job.enabled=true");
            }
        }

        private void requirePositive(Integer value, String propertyName) {
            if (value == null || value < 1) {
                throw new IllegalStateException(propertyName + " must be greater than 0 when framework.xxl-job.enabled=true");
            }
        }
    }

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
            return encode("jobId", context.getJobId())
                    + "|" + encode("executorId", context.getExecutorId())
                    + "|" + encode("param", context.getParam())
                    + "|" + encode("shardIndex", context.getShardIndex())
                    + "|" + encode("shardTotal", context.getShardTotal())
                    + "|" + encode("triggerTime", context.getTriggerTime());
        }

        private String encode(String name, Object value) {
            if (value == null) {
                return name + "=-1:";
            }
            String text = value.toString();
            return name + "=" + text.length() + ":" + text;
        }
    }

    /**
     * 基于内存存储的幂等任务处理器装饰器
     * <p>
     * 实现 {@link IdempotentJobHandler} 接口，在 {@link JobHandler} 基础上
     * 增加 jobId + executorId 的幂等保护。
     */
    static class MemoryIdempotentJobHandler implements IdempotentJobHandler, DisposableBean {

        private final MemoryIdempotencyStore store;
        private final XxlJobProperties properties;
        private final JobHandler delegate;
        private final ExecutorService executionExecutor = Executors.newVirtualThreadPerTaskExecutor();

        MemoryIdempotentJobHandler(MemoryIdempotencyStore store, XxlJobProperties properties, JobHandler delegate) {
            this.store = store;
            this.properties = properties;
            this.delegate = delegate;
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

            Future<JobResult> future = executionExecutor.submit(() -> delegate.execute(context));
            try {
                JobResult result = future.get(properties.getTimeout().getDefaultTimeout(), TimeUnit.SECONDS);
                if (result.isSuccess()) {
                    markProcessed(context);
                }
                return result;
            } catch (TimeoutException ex) {
                future.cancel(true);
                return JobResult.timeout("Execution exceeded timeout of "
                        + properties.getTimeout().getDefaultTimeout() + " seconds");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return JobResult.fail("Execution interrupted");
            } catch (ExecutionException ex) {
                return JobResult.fail("Execution failed: " + ex.getCause().getMessage());
            }
        }

        @Override
        public JobHandler getDelegate() {
            return delegate;
        }

        @Override
        public void destroy() {
            executionExecutor.shutdownNow();
        }
    }
}
