package com.microservice.framework.xxljob.autoconfigure;

import com.microservice.framework.xxljob.XxlJobProperties;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
            .withConfiguration(AutoConfigurations.of(
                    XxlJobAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活 IdempotentJobHandler")
    void defaultConfigurationShouldActivateIdempotentJobHandler() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("idempotentJobHandler");
            IdempotentJobHandler handler = context.getBean(IdempotentJobHandler.class);
            assertThat(handler).isNotNull();
        });
    }

    @Test
    @DisplayName("禁用 XXL-JOB Starter 后所有 Bean 不应存在")
    void disablingXxlJobShouldRemoveAllBeans() {
        contextRunner.withPropertyValues("framework.xxl-job.enabled=false")
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
}
