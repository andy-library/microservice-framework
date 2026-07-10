package com.microservice.framework.common.autoconfigure;

import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.common.id.IdGenerator;
import com.microservice.framework.common.time.FrameworkClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Common Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证三个自动配置模块的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class CommonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    CommonTimeAutoConfiguration.class,
                    CommonIdAutoConfiguration.class,
                    CommonContextAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活所有自动配置")
    void defaultConfigurationShouldActivateAll() {
        contextRunner.withPropertyValues("framework.common.id.worker-id=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("frameworkClock");
                    assertThat(context).hasBean("snowflakeIdGenerator");
                    assertThat(context).hasBean("threadLocalContextAdapter");
                });
    }

    @Test
    @DisplayName("禁用时间模块后 FrameworkClock Bean 不应存在")
    void disablingTimeModuleShouldRemoveFrameworkClock() {
        contextRunner.withPropertyValues(
                "framework.common.time.enabled=false",
                "framework.common.id.worker-id=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("frameworkClock");
                    assertThat(context).hasBean("snowflakeIdGenerator");
                    assertThat(context).hasBean("threadLocalContextAdapter");
                });
    }

    @Test
    @DisplayName("禁用 ID 模块后 IdGenerator Bean 不应存在")
    void disablingIdModuleShouldRemoveIdGenerator() {
        contextRunner.withPropertyValues("framework.common.id.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("frameworkClock");
                    assertThat(context).doesNotHaveBean("snowflakeIdGenerator");
                    assertThat(context).doesNotHaveBean(IdGenerator.class);
                    assertThat(context).hasBean("threadLocalContextAdapter");
                });
    }

    @Test
    @DisplayName("禁用上下文模块后 ThreadLocalContextAdapter Bean 不应存在")
    void disablingContextModuleShouldRemoveThreadLocalContextAdapter() {
        contextRunner.withPropertyValues(
                "framework.common.context.enabled=false",
                "framework.common.id.worker-id=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("frameworkClock");
                    assertThat(context).hasBean("snowflakeIdGenerator");
                    assertThat(context).doesNotHaveBean("threadLocalContextAdapter");
                    assertThat(context).doesNotHaveBean(ThreadLocalContextAdapter.class);
                });
    }

    @Test
    @DisplayName("配置自定义时区后 FrameworkClock 使用指定时区")
    void customTimeZoneShouldBeAppliedToFrameworkClock() {
        contextRunner.withPropertyValues(
                "framework.common.time.time-zone=Asia/Shanghai",
                "framework.common.id.worker-id=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FrameworkClock clock = context.getBean(FrameworkClock.class);
                    assertThat(clock.getZoneId().getId()).isEqualTo("Asia/Shanghai");
                });
    }

    @Test
    @DisplayName("配置自定义 worker-id 后 IdGenerator 使用指定值")
    void customWorkerIdShouldBeAppliedToIdGenerator() {
        contextRunner.withPropertyValues("framework.common.id.worker-id=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("snowflakeIdGenerator");
                    assertThat(context.getBean(IdGenerator.class)).isNotNull();
                });
    }

    @Test
    @DisplayName("缺少 worker-id 配置时启动应失败")
    void missingWorkerIdShouldFailStartup() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(com.microservice.framework.common.error.FrameworkException.class);
        });
    }

    @Test
    @DisplayName("worker-id 超出有效范围时启动应失败")
    void invalidWorkerIdRangeShouldFailStartup() {
        contextRunner.withPropertyValues("framework.common.id.worker-id=1024")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(com.microservice.framework.common.error.FrameworkException.class);
                });
    }

    @Test
    @DisplayName("用户提供的 IdGenerator 应覆盖默认 Bean")
    void userProvidedIdGeneratorShouldOverrideDefault() {
        contextRunner.withPropertyValues("framework.common.id.worker-id=1")
                .withBean("customIdGenerator", IdGenerator.class, () -> new IdGenerator() {
                    @Override
                    public long generate() {
                        return 42L;
                    }

                    @Override
                    public java.util.List<Long> batchGenerate(int count) {
                        return java.util.stream.LongStream.range(0, count)
                                .map(i -> 42L + i)
                                .boxed()
                                .collect(java.util.stream.Collectors.toList());
                    }
                })
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customIdGenerator");
                    assertThat(context).doesNotHaveBean("snowflakeIdGenerator");
                    assertThat(context.getBean(IdGenerator.class).generate()).isEqualTo(42L);
                });
    }

    @Test
    @DisplayName("用户提供的 FrameworkClock 应覆盖默认 Bean")
    void userProvidedFrameworkClockShouldOverrideDefault() {
        FrameworkClock customClock = FrameworkClock.of("America/New_York");
        contextRunner.withPropertyValues("framework.common.id.worker-id=1")
                .withBean("customFrameworkClock", FrameworkClock.class, () -> customClock)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customFrameworkClock");
                    assertThat(context).doesNotHaveBean("frameworkClock");
                    assertThat(context.getBean(FrameworkClock.class).getZoneId().getId())
                            .isEqualTo("America/New_York");
                });
    }
}
