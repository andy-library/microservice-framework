package com.microservice.framework.async.autoconfigure;

import com.microservice.framework.async.AsyncProperties;
import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.async.api.ContextPropagatingTaskDecorator;
import com.microservice.framework.async.api.ThreadPoolProfile;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Async Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class AsyncAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AsyncAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活所有 Async 自动配置")
    void defaultConfigurationShouldActivateAll() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("threadPoolProfile");
            assertThat(context).hasBean("asyncTaskExecutor");
            assertThat(context).hasBean("contextPropagatingTaskDecorator");
        });
    }

    @Test
    @DisplayName("禁用 Async Starter 后所有 Bean 不应存在")
    void disablingAsyncShouldRemoveAllBeans() {
        contextRunner.withPropertyValues("framework.async.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AsyncTaskExecutor.class);
                    assertThat(context).doesNotHaveBean(ThreadPoolProfile.class);
                    assertThat(context).doesNotHaveBean(ContextPropagatingTaskDecorator.class);
                });
    }

    @Test
    @DisplayName("自定义线程池参数应被 ThreadPoolProfile 正确应用")
    void customPoolParametersShouldBeAppliedToThreadPoolProfile() {
        contextRunner.withPropertyValues(
                "framework.async.pool.core-size=8",
                "framework.async.pool.max-size=16",
                "framework.async.pool.queue-capacity=512",
                "framework.async.pool.thread-name-prefix=worker-",
                "framework.async.pool.rejection=ABORT")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ThreadPoolProfile profile = context.getBean(ThreadPoolProfile.class);
                    assertThat(profile.getCoreSize()).isEqualTo(8);
                    assertThat(profile.getMaxSize()).isEqualTo(16);
                    assertThat(profile.getQueueCapacity()).isEqualTo(512);
                    assertThat(profile.getThreadNamePrefix()).isEqualTo("worker-");
                    assertThat(profile.getRejectionPolicy()).isEqualTo(AsyncProperties.RejectionPolicy.ABORT);
                });
    }

    @Test
    @DisplayName("禁用上下文传播后 ContextPropagatingTaskDecorator Bean 不应存在")
    void disablingContextPropagationShouldRemoveDecorator() {
        contextRunner.withPropertyValues(
                "framework.async.context.propagation-enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ContextPropagatingTaskDecorator.class);
                });
    }

    @Test
    @DisplayName("AsyncTaskExecutor 应具备监控方法")
    void asyncTaskExecutorShouldHaveMonitoringMethods() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AsyncTaskExecutor executor = context.getBean(AsyncTaskExecutor.class);

            // 初始状态下监控值应合理
            assertThat(executor.getActiveCount()).isGreaterThanOrEqualTo(0);
            assertThat(executor.getPoolSize()).isGreaterThanOrEqualTo(0);
            assertThat(executor.getQueueSize()).isGreaterThanOrEqualTo(0);
            assertThat(executor.getCompletedTaskCount()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    @DisplayName("优雅关停配置应正确绑定")
    void shutdownPropertiesShouldBeBoundCorrectly() {
        contextRunner.withPropertyValues(
                "framework.async.shutdown.await-termination=false",
                "framework.async.shutdown.await-termination-seconds=60")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AsyncProperties properties = context.getBean(AsyncProperties.class);
                    assertThat(properties.getShutdown().getAwaitTermination()).isFalse();
                    assertThat(properties.getShutdown().getAwaitTerminationSeconds()).isEqualTo(60);
                });
    }

    @Test
    @DisplayName("用户提供的 ThreadPoolProfile 应覆盖默认 Bean")
    void userProvidedThreadPoolProfileShouldOverrideDefault() {
        ThreadPoolProfile customProfile = ThreadPoolProfile.of(
                2, 4, 100, "custom-", AsyncProperties.RejectionPolicy.ABORT);

        contextRunner.withBean("customThreadPoolProfile", ThreadPoolProfile.class, () -> customProfile)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customThreadPoolProfile");
                    assertThat(context).doesNotHaveBean("threadPoolProfile");
                    ThreadPoolProfile profile = context.getBean(ThreadPoolProfile.class);
                    assertThat(profile.getCoreSize()).isEqualTo(2);
                    assertThat(profile.getMaxSize()).isEqualTo(4);
                    assertThat(profile.getThreadNamePrefix()).isEqualTo("custom-");
                });
    }
}
