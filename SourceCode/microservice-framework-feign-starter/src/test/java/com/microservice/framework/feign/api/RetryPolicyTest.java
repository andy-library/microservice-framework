package com.microservice.framework.feign.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RetryPolicy 测试
 * <p>
 * 验证重试策略的创建、参数验证和不可变性。
 *
 * @author Andy Yang
 */
class RetryPolicyTest {

    @Nested
    @DisplayName("工厂方法创建")
    class FactoryCreation {

        @Test
        @DisplayName("of() 应创建指定参数的策略")
        void ofShouldCreatePolicyWithSpecifiedParameters() {
            RetryPolicy policy = RetryPolicy.of(5, 200, Set.of(500, 502, 503));
            assertThat(policy.getMaxRetries()).isEqualTo(5);
            assertThat(policy.getRetryInterval()).isEqualTo(200);
            assertThat(policy.getRetryOnStatuses()).containsExactlyInAnyOrder(500, 502, 503);
        }

        @Test
        @DisplayName("of() 含异常类名应创建指定参数的策略")
        void ofWithExceptionsShouldCreatePolicyWithAllParameters() {
            RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502, 503),
                    Set.of("java.io.IOException", "java.net.SocketTimeoutException"));
            assertThat(policy.getMaxRetries()).isEqualTo(3);
            assertThat(policy.getRetryInterval()).isEqualTo(100);
            assertThat(policy.getRetryOnStatuses()).containsExactlyInAnyOrder(502, 503);
            assertThat(policy.getRetryOnExceptions())
                    .containsExactlyInAnyOrder("java.io.IOException", "java.net.SocketTimeoutException");
        }

        @Test
        @DisplayName("defaults() 应创建默认参数的策略")
        void defaultsShouldCreatePolicyWithDefaultParameters() {
            RetryPolicy policy = RetryPolicy.defaults();
            assertThat(policy.getMaxRetries()).isEqualTo(3);
            assertThat(policy.getRetryInterval()).isEqualTo(100);
            assertThat(policy.getRetryOnStatuses()).containsExactlyInAnyOrder(502, 503);
            assertThat(policy.getRetryOnExceptions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("参数验证")
    class ParameterValidation {

        @Test
        @DisplayName("maxRetries 为负数时应抛出 IllegalArgumentException")
        void negativeMaxRetriesShouldThrow() {
            assertThatThrownBy(() -> RetryPolicy.of(-1, 100, Set.of(502)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxRetries");
        }

        @Test
        @DisplayName("retryInterval 为负数时应抛出 IllegalArgumentException")
        void negativeRetryIntervalShouldThrow() {
            assertThatThrownBy(() -> RetryPolicy.of(3, -1, Set.of(502)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("retryInterval");
        }

        @Test
        @DisplayName("maxRetries=0 应合法（不重试）")
        void zeroMaxRetriesShouldBeValid() {
            RetryPolicy policy = RetryPolicy.of(0, 100, Set.of(502));
            assertThat(policy.getMaxRetries()).isZero();
        }
    }

    @Nested
    @DisplayName("不可变性与空值处理")
    class ImmutabilityAndNullHandling {

        @Test
        @DisplayName("retryOnStatuses 应不可修改")
        void retryOnStatusesShouldBeUnmodifiable() {
            RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502, 503));
            assertThatThrownBy(() -> policy.getRetryOnStatuses().add(500))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("retryOnExceptions 应不可修改")
        void retryOnExceptionsShouldBeUnmodifiable() {
            RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502),
                    Set.of("java.io.IOException"));
            assertThatThrownBy(() -> policy.getRetryOnExceptions().add("NewException"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("null retryOnStatuses 应返回空 Set")
        void nullRetryOnStatusesShouldReturnEmptySet() {
            RetryPolicy policy = RetryPolicy.of(3, 100, null);
            assertThat(policy.getRetryOnStatuses()).isEmpty();
        }

        @Test
        @DisplayName("null retryOnExceptions 应返回空 Set")
        void nullRetryOnExceptionsShouldReturnEmptySet() {
            RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502), null);
            assertThat(policy.getRetryOnExceptions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同参数的策略应相等")
        void sameParametersShouldBeEqual() {
            RetryPolicy policy1 = RetryPolicy.of(3, 100, Set.of(502, 503));
            RetryPolicy policy2 = RetryPolicy.of(3, 100, Set.of(502, 503));
            assertThat(policy1).isEqualTo(policy2);
            assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
        }

        @Test
        @DisplayName("不同参数的策略应不相等")
        void differentParametersShouldNotBeEqual() {
            RetryPolicy policy1 = RetryPolicy.of(3, 100, Set.of(502, 503));
            RetryPolicy policy2 = RetryPolicy.of(5, 100, Set.of(502, 503));
            assertThat(policy1).isNotEqualTo(policy2);
        }

        @Test
        @DisplayName("toString 应包含所有参数")
        void toStringShouldContainAllParameters() {
            RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502, 503));
            assertThat(policy.toString()).contains("maxRetries=3");
            assertThat(policy.toString()).contains("retryInterval=100");
            assertThat(policy.toString()).contains("retryOnStatuses");
        }
    }
}
