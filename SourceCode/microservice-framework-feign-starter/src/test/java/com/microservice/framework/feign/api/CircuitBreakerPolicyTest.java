package com.microservice.framework.feign.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CircuitBreakerPolicy 测试
 * <p>
 * 验证熔断策略的创建、阈值参数和不可变性。
 *
 * @author Andy Yang
 */
class CircuitBreakerPolicyTest {

    @Nested
    @DisplayName("工厂方法创建")
    class FactoryCreation {

        @Test
        @DisplayName("of() 应创建指定阈值的策略")
        void ofShouldCreatePolicyWithSpecifiedThresholds() {
            CircuitBreakerPolicy policy = CircuitBreakerPolicy.of(60f, 5000L, 80f);
            assertThat(policy.getFailureRateThreshold()).isEqualTo(60f);
            assertThat(policy.getSlowCallDuration()).isEqualTo(5000L);
            assertThat(policy.getSlowCallRateThreshold()).isEqualTo(80f);
        }

        @Test
        @DisplayName("defaults() 应创建默认阈值的策略")
        void defaultsShouldCreatePolicyWithDefaultThresholds() {
            CircuitBreakerPolicy policy = CircuitBreakerPolicy.defaults();
            assertThat(policy.getFailureRateThreshold()).isEqualTo(50f);
            assertThat(policy.getSlowCallDuration()).isEqualTo(3000L);
            assertThat(policy.getSlowCallRateThreshold()).isEqualTo(100f);
        }
    }

    @Nested
    @DisplayName("阈值参数验证")
    class ThresholdValidation {

        @Test
        @DisplayName("failureRateThreshold 为 0 时应抛出 IllegalArgumentException")
        void zeroFailureRateThresholdShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(0f, 3000L, 100f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("failureRateThreshold");
        }

        @Test
        @DisplayName("failureRateThreshold 超过 100 时应抛出 IllegalArgumentException")
        void failureRateThresholdOver100ShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(101f, 3000L, 100f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("failureRateThreshold");
        }

        @Test
        @DisplayName("slowCallDuration 为 0 时应抛出 IllegalArgumentException")
        void zeroSlowCallDurationShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(50f, 0L, 100f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("slowCallDuration");
        }

        @Test
        @DisplayName("slowCallDuration 为负数时应抛出 IllegalArgumentException")
        void negativeSlowCallDurationShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(50f, -1L, 100f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("slowCallDuration");
        }

        @Test
        @DisplayName("slowCallRateThreshold 为 0 时应抛出 IllegalArgumentException")
        void zeroSlowCallRateThresholdShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(50f, 3000L, 0f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("slowCallRateThreshold");
        }

        @Test
        @DisplayName("slowCallRateThreshold 超过 100 时应抛出 IllegalArgumentException")
        void slowCallRateThresholdOver100ShouldThrow() {
            assertThatThrownBy(() -> CircuitBreakerPolicy.of(50f, 3000L, 101f))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("slowCallRateThreshold");
        }
    }

    @Nested
    @DisplayName("熔断器状态枚举")
    class StateEnum {

        @Test
        @DisplayName("State 枚举应包含 CLOSED, OPEN, HALF_OPEN")
        void stateEnumShouldContainAllValues() {
            assertThat(CircuitBreakerPolicy.State.values()).containsExactly(
                    CircuitBreakerPolicy.State.CLOSED,
                    CircuitBreakerPolicy.State.OPEN,
                    CircuitBreakerPolicy.State.HALF_OPEN);
        }
    }

    @Nested
    @DisplayName("不可变性与相等性")
    class ImmutabilityAndEquality {

        @Test
        @DisplayName("相同参数的策略应相等")
        void sameParametersShouldBeEqual() {
            CircuitBreakerPolicy policy1 = CircuitBreakerPolicy.of(50f, 3000L, 100f);
            CircuitBreakerPolicy policy2 = CircuitBreakerPolicy.of(50f, 3000L, 100f);
            assertThat(policy1).isEqualTo(policy2);
            assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
        }

        @Test
        @DisplayName("不同参数的策略应不相等")
        void differentParametersShouldNotBeEqual() {
            CircuitBreakerPolicy policy1 = CircuitBreakerPolicy.of(50f, 3000L, 100f);
            CircuitBreakerPolicy policy2 = CircuitBreakerPolicy.of(60f, 3000L, 100f);
            assertThat(policy1).isNotEqualTo(policy2);
        }

        @Test
        @DisplayName("toString 应包含所有阈值参数")
        void toStringShouldContainAllThresholds() {
            CircuitBreakerPolicy policy = CircuitBreakerPolicy.of(50f, 3000L, 100f);
            assertThat(policy.toString()).contains("failureRateThreshold=50.0");
            assertThat(policy.toString()).contains("slowCallDuration=3000");
            assertThat(policy.toString()).contains("slowCallRateThreshold=100.0");
        }
    }
}
