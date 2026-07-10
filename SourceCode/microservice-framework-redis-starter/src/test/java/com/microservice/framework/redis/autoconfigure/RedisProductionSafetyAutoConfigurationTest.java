package com.microservice.framework.redis.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis production safety tests.
 */
class RedisProductionSafetyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisProductionSafetyAutoConfiguration.class));

    @Nested
    @DisplayName("生产准入校验")
    class ProductionSafety {

        @Test
        @DisplayName("生产环境不允许关闭缓存能力")
        void prodShouldRejectDisabledCache() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.cache.enabled=false")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.cache.enabled cannot be disabled in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制缓存模式删除 SCAN 批量大小")
        void prodShouldRejectExcessiveScanBatchSize() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.cache.scan-batch-size=5000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.cache.scan-batch-size must not exceed 1000 in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制 null 值缓存 TTL")
        void prodShouldRejectExcessiveNullValueTtl() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.cache.null-value-ttl=600")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.cache.null-value-ttl must not exceed 300s in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制默认获取锁等待时间")
        void prodShouldRejectExcessiveLockTimeout() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.lock.default-timeout=10000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.lock.default-timeout must not exceed 5000ms in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制默认锁持有时间")
        void prodShouldRejectExcessiveLockExpire() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.lock.default-expire=120000")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.lock.default-expire must not exceed 60000ms in prod profile");
                    });
        }

        @Test
        @DisplayName("生产环境应限制默认限流窗口")
        void prodShouldRejectExcessiveRateLimitPeriod() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.rate-limit.default-period=120")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasMessageContaining("framework.redis.rate-limit.default-period must not exceed 60s in prod profile");
                    });
        }

        @Test
        @DisplayName("生产推荐配置应通过准入校验")
        void prodShouldAcceptRecommendedBoundaries() {
            contextRunner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "framework.redis.cache.enabled=true",
                            "framework.redis.cache.default-ttl=3600",
                            "framework.redis.cache.null-value-ttl=60",
                            "framework.redis.cache.scan-batch-size=1000",
                            "framework.redis.lock.default-timeout=3000",
                            "framework.redis.lock.default-expire=30000",
                            "framework.redis.rate-limit.default-period=1")
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }
}
