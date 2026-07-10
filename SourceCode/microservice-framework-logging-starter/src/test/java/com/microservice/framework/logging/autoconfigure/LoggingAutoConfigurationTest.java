package com.microservice.framework.logging.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Logging AutoConfiguration 集成测试
 */
class LoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LoggingBaseAutoConfiguration.class,
                    LoggingProductionSafetyAutoConfiguration.class,
                    MaskingLoggingAutoConfiguration.class,
                    FloodProtectionAutoConfiguration.class,
                    TraceSamplingAutoConfiguration.class));

    // ==================== LoggingBaseAutoConfiguration ====================

    @Test
    @DisplayName("LoggingBaseAutoConfiguration: 上下文启动成功")
    void testLoggingBase_ContextStarts() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    @DisplayName("LoggingBaseAutoConfiguration: 配置属性可用")
    void testLoggingBase_PropertiesAvailable() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.enabled=true",
                        "framework.logging.level=INFO")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    // ==================== MaskingLoggingAutoConfiguration ====================

    @Test
    @DisplayName("MaskingLoggingAutoConfiguration: 脱敏配置可用")
    void testMasking_ConfigAvailable() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.masking.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("MaskingLoggingAutoConfiguration: 可禁用脱敏")
    void testMasking_CanBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.masking.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    // ==================== FloodProtectionAutoConfiguration ====================

    @Test
    @DisplayName("FloodProtectionAutoConfiguration: 限流配置可用")
    void testFloodProtection_ConfigAvailable() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.flood-protection.enabled=true",
                        "framework.logging.flood-protection.rate=1000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("FloodProtectionAutoConfiguration: 可禁用限流")
    void testFloodProtection_CanBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.flood-protection.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("FloodProtectionAutoConfiguration: 用户自定义 RateLimitingTurboFilter 时不应创建第二个 Bean")
    void floodProtectionShouldBackOffWhenCustomFilterExists() {
        com.microservice.framework.logging.core.filter.RateLimitingTurboFilter customFilter =
                new com.microservice.framework.logging.core.filter.RateLimitingTurboFilter();

        contextRunner
                .withBean("customRateLimitingTurboFilter",
                        com.microservice.framework.logging.core.filter.RateLimitingTurboFilter.class,
                        () -> customFilter)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(
                            com.microservice.framework.logging.core.filter.RateLimitingTurboFilter.class);
                    assertThat(context.getBean(
                            com.microservice.framework.logging.core.filter.RateLimitingTurboFilter.class))
                            .isSameAs(customFilter);
                });
    }

    // ==================== TraceSamplingAutoConfiguration ====================

    @Test
    @DisplayName("TraceSamplingAutoConfiguration: 采样配置可用")
    void testTraceSampling_ConfigAvailable() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.trace-sampling.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("TraceSamplingAutoConfiguration: 可禁用采样")
    void testTraceSampling_CanBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.trace-sampling.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    // ==================== 配置合法性校验 ====================

    @Test
    @DisplayName("配置合法性校验: async.queue-size 小于 1 时启动失败")
    void invalidAsyncQueueSizeShouldFailFast() {
        contextRunner
                .withPropertyValues("framework.logging.async.queue-size=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("配置合法性校验: flood-protection.rate 小于 1 时启动失败")
    void invalidFloodProtectionRateShouldFailFast() {
        contextRunner
                .withPropertyValues("framework.logging.flood-protection.rate=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("配置合法性校验: 自定义脱敏规则缺少 regex 时启动失败")
    void customMaskingRuleWithoutRegexShouldFailFast() {
        contextRunner
                .withPropertyValues(
                        "framework.logging.masking.custom-rules[0].name=EMAIL",
                        "framework.logging.masking.custom-rules[0].mask=***")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("配置合法性校验: 非法未采样日志级别时启动失败")
    void invalidUnsampledTraceLevelShouldFailFast() {
        contextRunner
                .withPropertyValues("framework.logging.trace-sampling.level-for-unsampled=VERBOSE")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("生产环境不允许关闭异步日志")
    void prodShouldRejectDisabledAsyncLogging() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.logging.async.enabled=false",
                        "framework.logging.masking.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.logging.async.enabled cannot be disabled in prod profile");
                });
    }

    @Test
    @DisplayName("生产环境不允许配置异步日志丢弃阈值")
    void prodShouldRejectAsyncDiscardingThreshold() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.logging.async.discarding-threshold=10",
                        "framework.logging.masking.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.logging.async.discarding-threshold must be 0 in prod profile");
                });
    }

    @Test
    @DisplayName("生产环境不允许关闭日志风暴防护")
    void prodShouldRejectDisabledFloodProtection() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.logging.flood-protection.enabled=false",
                        "framework.logging.masking.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.logging.flood-protection.enabled cannot be disabled in prod profile");
                });
    }

    @Test
    @DisplayName("生产环境不允许关闭日志脱敏")
    void prodShouldRejectDisabledMasking() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.logging.masking.enabled cannot be disabled in prod profile");
                });
    }

    @Test
    @DisplayName("生产推荐配置应通过日志准入校验")
    void prodShouldAcceptRecommendedLoggingBoundaries() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.logging.async.enabled=true",
                        "framework.logging.async.queue-size=256",
                        "framework.logging.async.discarding-threshold=0",
                        "framework.logging.flood-protection.enabled=true",
                        "framework.logging.masking.enabled=true")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
