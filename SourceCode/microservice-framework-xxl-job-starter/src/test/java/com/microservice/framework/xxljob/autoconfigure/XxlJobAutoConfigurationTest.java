package com.microservice.framework.xxljob.autoconfigure;

import com.microservice.framework.xxljob.XxlJobProperties;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import com.microservice.framework.xxljob.api.JobExecutionContext;
import com.microservice.framework.xxljob.api.JobHandler;
import com.microservice.framework.xxljob.api.JobResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XXL-JOB Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 * <p>
 * 注意：由于 xxl-job-core 在 optional scope 中可用（Maven 会将其包含在测试 classpath），
 * {@code @ConditionalOnClass(name = "com.xxl.job.core.handler.IJobHandler")} 条件满足。
 *
 * @author Andy Yang
 */
class XxlJobAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XxlJobAutoConfiguration.class))
            .withPropertyValues(
                    "framework.xxl-job.admin.addresses=http://127.0.0.1:8080/xxl-job-admin",
                    "framework.xxl-job.admin.app-name=test-app",
                    "framework.xxl-job.executor.app-name=test-executor");

    @Test
    @DisplayName("默认配置应激活 IdempotentJobHandler")
    void defaultConfigurationShouldActivateIdempotentJobHandler() {
        contextRunner.withBean(JobHandler.class, CountingJobHandler::new).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("idempotentJobHandler");
            IdempotentJobHandler handler = context.getBean(IdempotentJobHandler.class);
            assertThat(handler).isNotNull();
            assertThat(handler.execute(JobExecutionContext.of(1, "executor", "param"))).isEqualTo(JobResult.success("handled"));
            assertThat(context.getBean(CountingJobHandler.class).invocationCount).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("禁用 XXL-JOB Starter 后所有 Bean 不应存在")
    void disablingXxlJobShouldRemoveAllBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(XxlJobAutoConfiguration.class))
                .withPropertyValues("framework.xxl-job.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IdempotentJobHandler.class);
                    assertThat(context).doesNotHaveBean(XxlJobProperties.class);
                });
    }

    @Test
    @DisplayName("禁用幂等保护后 IdempotentJobHandler Bean 不应存在")
    void disablingIdempotencyShouldRemoveIdempotentJobHandler() {
        contextRunner.withPropertyValues("framework.xxl-job.idempotency.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IdempotentJobHandler.class);
                    assertThat(context.getBean(XxlJobProperties.class)).isNotNull();
                });
    }

    @Test
    @DisplayName("启用 XXL-JOB 但缺少必填配置时应使上下文启动失败")
    void enabledXxlJobShouldValidateRequiredConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(XxlJobAutoConfiguration.class))
                .withBean(JobHandler.class, CountingJobHandler::new)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("任务失败后不应标记幂等键，从而允许重试")
    void failedJobShouldNotBeMarkedProcessed() {
        contextRunner.withBean(FailingJobHandler.class, FailingJobHandler::new).run(context -> {
            IdempotentJobHandler handler = context.getBean(IdempotentJobHandler.class);
            JobExecutionContext executionContext = JobExecutionContext.of(1001, "executor-1", "sync");

            assertThat(handler.execute(executionContext).isFail()).isTrue();
            assertThat(handler.execute(executionContext).isFail()).isTrue();
            assertThat(context.getBean(FailingJobHandler.class).invocationCount).isEqualTo(2);
            assertThat(handler.isDuplicate(executionContext)).isFalse();
        });
    }

    @Test
    @DisplayName("幂等键应包含参数、分片和触发身份")
    void idempotencyKeyShouldIncludeParamShardAndTriggerIdentity() {
        contextRunner.withBean(CountingJobHandler.class, CountingJobHandler::new).run(context -> {
            IdempotentJobHandler handler = context.getBean(IdempotentJobHandler.class);
            Instant triggerTime = Instant.parse("2026-07-11T00:00:00Z");

            handler.execute(JobExecutionContext.of(1001, "executor-1", "sync-a", 0, 2, triggerTime));
            handler.execute(JobExecutionContext.of(1001, "executor-1", "sync-b", 0, 2, triggerTime));
            handler.execute(JobExecutionContext.of(1001, "executor-1", "sync-a", 1, 2, triggerTime));
            handler.execute(JobExecutionContext.of(1001, "executor-1", "sync-a", 0, 2, triggerTime.plusSeconds(1)));

            assertThat(context.getBean(CountingJobHandler.class).invocationCount).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("幂等键应安全编码字段避免分隔符碰撞")
    void idempotencyKeyShouldEncodeFieldsWithoutDelimiterCollisions() {
        contextRunner.withBean(CountingJobHandler.class, CountingJobHandler::new).run(context -> {
            IdempotentJobHandler handler = context.getBean(IdempotentJobHandler.class);
            Instant triggerTime = Instant.parse("2026-07-11T00:00:00Z");

            handler.execute(JobExecutionContext.of(1, "2", "3-4", 5, 6, triggerTime));
            handler.execute(JobExecutionContext.of(1, "2-3", "4", 5, 6, triggerTime));

            assertThat(context.getBean(CountingJobHandler.class).invocationCount).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("执行超过默认超时时间应返回超时结果")
    void executionExceedingDefaultTimeoutShouldReturnTimeout() {
        contextRunner.withPropertyValues("framework.xxl-job.timeout.default-timeout=1")
                .withBean(SlowJobHandler.class, SlowJobHandler::new)
                .run(context -> assertThat(context.getBean(IdempotentJobHandler.class)
                        .execute(JobExecutionContext.of(1001, "executor-1", "sync")).isTimeout()).isTrue());
    }

    @Test
    @DisplayName("自定义 IdempotentJobHandler 应覆盖默认 Bean")
    void userProvidedIdempotentJobHandlerShouldOverrideDefault() {
        contextRunner.withBean("customIdempotentJobHandler", IdempotentJobHandler.class,
                () -> new StubIdempotentJobHandler())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customIdempotentJobHandler");
                    assertThat(context).doesNotHaveBean("idempotentJobHandler");
                    assertThat(context.getBean(IdempotentJobHandler.class)).isNotNull();
                });
    }

    @Test
    @DisplayName("XxlJobProperties 应正确绑定自定义属性")
    void xxlJobPropertiesShouldBindCustomPropertiesCorrectly() {
        contextRunner.withPropertyValues(
                "framework.xxl-job.admin.addresses=http://127.0.0.1:8080/xxl-job-admin",
                "framework.xxl-job.admin.app-name=test-app",
                "framework.xxl-job.admin.access-token=test-token",
                "framework.xxl-job.executor.app-name=test-executor",
                "framework.xxl-job.executor.port=8888",
                "framework.xxl-job.executor.log-path=/var/log/xxl-job",
                "framework.xxl-job.executor.log-retention-days=60",
                "framework.xxl-job.idempotency.enabled=true",
                "framework.xxl-job.idempotency.store-type=MEMORY",
                "framework.xxl-job.timeout.default-timeout=600",
                "framework.xxl-job.timeout.timeout-handler=TIMEOUT")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    XxlJobProperties properties = context.getBean(XxlJobProperties.class);
                    assertThat(properties.getAdmin().getAddresses()).isEqualTo("http://127.0.0.1:8080/xxl-job-admin");
                    assertThat(properties.getAdmin().getAppName()).isEqualTo("test-app");
                    assertThat(properties.getAdmin().getAccessToken()).isEqualTo("test-token");
                    assertThat(properties.getExecutor().getAppName()).isEqualTo("test-executor");
                    assertThat(properties.getExecutor().getPort()).isEqualTo(8888);
                    assertThat(properties.getExecutor().getLogPath()).isEqualTo("/var/log/xxl-job");
                    assertThat(properties.getExecutor().getLogRetentionDays()).isEqualTo(60);
                    assertThat(properties.getIdempotency().getEnabled()).isTrue();
                    assertThat(properties.getIdempotency().getStoreType()).isEqualTo(XxlJobProperties.StoreType.MEMORY);
                    assertThat(properties.getTimeout().getDefaultTimeout()).isEqualTo(600);
                    assertThat(properties.getTimeout().getTimeoutHandler()).isEqualTo(XxlJobProperties.TimeoutHandler.TIMEOUT);
                });
    }

    // ========================================================================
    // Stub 实现
    // ========================================================================

    /**
     * Stub IdempotentJobHandler 用于测试用户自定义 Bean 覆盖
     */
    static class StubIdempotentJobHandler implements IdempotentJobHandler {

        @Override
        public boolean isDuplicate(com.microservice.framework.xxljob.api.JobExecutionContext context) {
            return false;
        }

        @Override
        public void markProcessed(com.microservice.framework.xxljob.api.JobExecutionContext context) {
            // no-op
        }

        @Override
        public com.microservice.framework.xxljob.api.JobResult execute(com.microservice.framework.xxljob.api.JobExecutionContext context) {
            return com.microservice.framework.xxljob.api.JobResult.success();
        }

        @Override
        public com.microservice.framework.xxljob.api.JobHandler getDelegate() {
            return null;
        }
    }

    static class CountingJobHandler implements JobHandler {

        private int invocationCount;

        @Override
        public JobResult execute(JobExecutionContext context) {
            invocationCount++;
            return JobResult.success("handled");
        }
    }

    static class FailingJobHandler implements JobHandler {

        private int invocationCount;

        @Override
        public JobResult execute(JobExecutionContext context) {
            invocationCount++;
            return JobResult.fail("retry me");
        }
    }

    static class SlowJobHandler implements JobHandler {

        @Override
        public JobResult execute(JobExecutionContext context) {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return JobResult.success();
        }
    }
}
