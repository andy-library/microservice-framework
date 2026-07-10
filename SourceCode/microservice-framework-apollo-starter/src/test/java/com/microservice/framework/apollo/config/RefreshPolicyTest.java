package com.microservice.framework.apollo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RefreshPolicy 测试
 * <p>
 * 验证刷新策略的创建、不可刷新前缀匹配和策略类型判断。
 *
 * @author Andy Yang
 */
class RefreshPolicyTest {

    @Nested
    @DisplayName("策略创建")
    class CreationTests {

        @Test
        @DisplayName("defaultPolicy() 应创建 ON_CHANGE 策略")
        void defaultPolicyShouldCreateOnChangeStrategy() {
            RefreshPolicy policy = RefreshPolicy.defaultPolicy();

            assertThat(policy.getStrategy()).isEqualTo(RefreshPolicy.Strategy.ON_CHANGE);
            assertThat(policy.getNonRefreshablePrefixes()).isEmpty();
        }

        @Test
        @DisplayName("none() 应创建 NONE 策略")
        void noneShouldCreateNoneStrategy() {
            RefreshPolicy policy = RefreshPolicy.none();

            assertThat(policy.getStrategy()).isEqualTo(RefreshPolicy.Strategy.NONE);
            assertThat(policy.getNonRefreshablePrefixes()).isEmpty();
        }

        @Test
        @DisplayName("自定义策略应正确创建")
        void customPolicyShouldBeCreated() {
            Set<String> prefixes = Set.of("spring.datasource.", "server.port");
            RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);

            assertThat(policy.getStrategy()).isEqualTo(RefreshPolicy.Strategy.ON_CHANGE);
            assertThat(policy.getNonRefreshablePrefixes()).containsExactlyInAnyOrder(
                    "spring.datasource.", "server.port");
        }

        @Test
        @DisplayName("null 不可刷新前缀应使用空集合")
        void nullPrefixesShouldUseEmptySet() {
            RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, null);

            assertThat(policy.getNonRefreshablePrefixes()).isEmpty();
        }

        @Test
        @DisplayName("null strategy 应抛出 NullPointerException")
        void nullStrategyShouldThrowException() {
            assertThatThrownBy(() -> new RefreshPolicy(null, Set.of()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("strategy");
        }

        @Test
        @DisplayName("不可刷新前缀集合应为不可修改的")
        void nonRefreshablePrefixesShouldBeUnmodifiable() {
            Set<String> prefixes = Set.of("prefix.");
            RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);

            assertThatThrownBy(() -> policy.getNonRefreshablePrefixes().add("new."))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("isRefreshable() 判断")
    class IsRefreshableTests {

        @Test
        @DisplayName("ON_CHANGE 策略下普通配置键应可刷新")
        void onChangeStrategyShouldAllowRefreshByDefault() {
            RefreshPolicy policy = RefreshPolicy.defaultPolicy();

            assertThat(policy.isRefreshable("app.feature.enabled")).isTrue();
            assertThat(policy.isRefreshable("logging.level")).isTrue();
        }

        @Test
        @DisplayName("ON_CHANGE 策略下匹配不可刷新前缀的键不应可刷新")
        void onChangeStrategyShouldBlockRefreshForNonRefreshablePrefixes() {
            Set<String> prefixes = Set.of("spring.datasource.", "server.");
            RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);

            assertThat(policy.isRefreshable("spring.datasource.url")).isFalse();
            assertThat(policy.isRefreshable("spring.datasource.hikari.max-size")).isFalse();
            assertThat(policy.isRefreshable("server.port")).isFalse();
            assertThat(policy.isRefreshable("app.feature.enabled")).isTrue();
        }

        @Test
        @DisplayName("NONE 策略下所有配置键都不应可刷新")
        void noneStrategyShouldBlockAllRefresh() {
            RefreshPolicy policy = RefreshPolicy.none();

            assertThat(policy.isRefreshable("app.feature.enabled")).isFalse();
            assertThat(policy.isRefreshable("logging.level")).isFalse();
        }

        @Test
        @DisplayName("MANUAL 策略下配置变更不应自动刷新")
        void manualStrategyShouldBlockAutoRefresh() {
            RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.MANUAL, Set.of());

            assertThat(policy.isRefreshable("app.feature.enabled")).isFalse();
        }
    }

    @Nested
    @DisplayName("相等性判断")
    class EqualityTests {

        @Test
        @DisplayName("相同参数的策略应相等")
        void policiesWithSameParametersShouldBeEqual() {
            Set<String> prefixes = Set.of("spring.datasource.");
            RefreshPolicy p1 = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);
            RefreshPolicy p2 = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);

            assertThat(p1).isEqualTo(p2);
            assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("不同策略类型应不相等")
        void policiesWithDifferentStrategyShouldNotBeEqual() {
            RefreshPolicy p1 = RefreshPolicy.defaultPolicy();
            RefreshPolicy p2 = RefreshPolicy.none();

            assertThat(p1).isNotEqualTo(p2);
        }
    }

    @Test
    @DisplayName("toString 应包含策略类型和前缀信息")
    void toStringShouldContainAllFields() {
        Set<String> prefixes = Set.of("spring.datasource.");
        RefreshPolicy policy = new RefreshPolicy(RefreshPolicy.Strategy.ON_CHANGE, prefixes);

        assertThat(policy.toString()).contains("ON_CHANGE");
        assertThat(policy.toString()).contains("spring.datasource.");
    }
}
