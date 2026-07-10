package com.microservice.framework.feign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FeignProperties 默认值测试
 * <p>
 * 验证所有嵌套配置的默认值是否符合 PRD 规范。
 *
 * @author Andy Yang
 */
class FeignPropertiesTest {

    private final FeignProperties properties = new FeignProperties();

    @Nested
    @DisplayName("连接配置默认值")
    class ConnectionDefaults {

        @Test
        @DisplayName("默认 timeout 应为 5000")
        void defaultTimeoutShouldBe5000() {
            assertThat(properties.getConnection().getTimeout()).isEqualTo(5000);
        }

        @Test
        @DisplayName("默认 connectTimeout 应为 2000")
        void defaultConnectTimeoutShouldBe2000() {
            assertThat(properties.getConnection().getConnectTimeout()).isEqualTo(2000);
        }

        @Test
        @DisplayName("默认 readTimeout 应为 5000")
        void defaultReadTimeoutShouldBe5000() {
            assertThat(properties.getConnection().getReadTimeout()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("重试配置默认值")
    class RetryDefaults {

        @Test
        @DisplayName("默认 enabled 应为 true")
        void defaultEnabledShouldBeTrue() {
            assertThat(properties.getRetry().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 maxRetries 应为 3")
        void defaultMaxRetriesShouldBe3() {
            assertThat(properties.getRetry().getMaxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("默认 retryInterval 应为 100")
        void defaultRetryIntervalShouldBe100() {
            assertThat(properties.getRetry().getRetryInterval()).isEqualTo(100);
        }

        @Test
        @DisplayName("默认 retryOnStatuses 应为 [502, 503]")
        void defaultRetryOnStatusesShouldBe502And503() {
            assertThat(properties.getRetry().getRetryOnStatuses())
                    .containsExactlyInAnyOrder(502, 503);
        }

        @Test
        @DisplayName("默认 retryOnExceptions 应为空")
        void defaultRetryOnExceptionsShouldBeEmpty() {
            assertThat(properties.getRetry().getRetryOnExceptions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("熔断配置默认值")
    class CircuitBreakerDefaults {

        @Test
        @DisplayName("默认 enabled 应为 false")
        void defaultEnabledShouldBeFalse() {
            assertThat(properties.getCircuitBreaker().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("默认 failureRateThreshold 应为 50")
        void defaultFailureRateThresholdShouldBe50() {
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(50f);
        }

        @Test
        @DisplayName("默认 slowCallDuration 应为 3000")
        void defaultSlowCallDurationShouldBe3000() {
            assertThat(properties.getCircuitBreaker().getSlowCallDuration()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("默认 slowCallRateThreshold 应为 100")
        void defaultSlowCallRateThresholdShouldBe100() {
            assertThat(properties.getCircuitBreaker().getSlowCallRateThreshold()).isEqualTo(100f);
        }
    }

    @Nested
    @DisplayName("上下文传播配置默认值")
    class ContextDefaults {

        @Test
        @DisplayName("默认 propagationEnabled 应为 true")
        void defaultPropagationEnabledShouldBeTrue() {
            assertThat(properties.getContext().isPropagationEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 propagateKeys 应为 [requestId, traceId, userId]")
        void defaultPropagateKeysShouldContainStandardKeys() {
            assertThat(properties.getContext().getPropagateKeys())
                    .containsExactlyInAnyOrder("requestId", "traceId", "userId");
        }
    }

    @Nested
    @DisplayName("服务身份配置默认值")
    class ServiceIdentityDefaults {

        @Test
        @DisplayName("默认 enabled 应为 true")
        void defaultEnabledShouldBeTrue() {
            assertThat(properties.getServiceIdentity().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("默认 serviceName 应为 null")
        void defaultServiceNameShouldBeNull() {
            assertThat(properties.getServiceIdentity().getServiceName()).isNull();
        }
    }
}
