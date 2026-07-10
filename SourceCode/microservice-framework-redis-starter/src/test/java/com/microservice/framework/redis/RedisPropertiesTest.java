package com.microservice.framework.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisProperties 配置属性绑定和默认值测试
 * <p>
 * 验证所有嵌套属性组的默认值是否正确，
 * 以及属性值是否可通过 setter 正确绑定。
 *
 * @author Andy Yang
 */
class RedisPropertiesTest {

    @Nested
    @DisplayName("LockProperties 默认值和绑定")
    class LockPropertiesTest {

        @Test
        @DisplayName("defaultTimeout 默认值为 3000ms")
        void defaultTimeoutDefaultValue() {
            RedisProperties.LockProperties props = new RedisProperties.LockProperties();
            assertThat(props.getDefaultTimeout()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("defaultExpire 默认值为 30000ms")
        void defaultExpireDefaultValue() {
            RedisProperties.LockProperties props = new RedisProperties.LockProperties();
            assertThat(props.getDefaultExpire()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("setter 可修改 defaultTimeout")
        void setDefaultTimeout() {
            RedisProperties.LockProperties props = new RedisProperties.LockProperties();
            props.setDefaultTimeout(5000L);
            assertThat(props.getDefaultTimeout()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("setter 可修改 defaultExpire")
        void setDefaultExpire() {
            RedisProperties.LockProperties props = new RedisProperties.LockProperties();
            props.setDefaultExpire(60000L);
            assertThat(props.getDefaultExpire()).isEqualTo(60000L);
        }
    }

    @Nested
    @DisplayName("RateLimitProperties 默认值和绑定")
    class RateLimitPropertiesTest {

        @Test
        @DisplayName("defaultPermits 默认值为 100")
        void defaultPermitsDefaultValue() {
            RedisProperties.RateLimitProperties props = new RedisProperties.RateLimitProperties();
            assertThat(props.getDefaultPermits()).isEqualTo(100);
        }

        @Test
        @DisplayName("defaultPeriod 默认值为 1 秒")
        void defaultPeriodDefaultValue() {
            RedisProperties.RateLimitProperties props = new RedisProperties.RateLimitProperties();
            assertThat(props.getDefaultPeriod()).isEqualTo(1L);
        }

        @Test
        @DisplayName("setter 可修改 defaultPermits")
        void setDefaultPermits() {
            RedisProperties.RateLimitProperties props = new RedisProperties.RateLimitProperties();
            props.setDefaultPermits(200);
            assertThat(props.getDefaultPermits()).isEqualTo(200);
        }

        @Test
        @DisplayName("setter 可修改 defaultPeriod")
        void setDefaultPeriod() {
            RedisProperties.RateLimitProperties props = new RedisProperties.RateLimitProperties();
            props.setDefaultPeriod(10L);
            assertThat(props.getDefaultPeriod()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("CounterProperties 默认值和绑定")
    class CounterPropertiesTest {

        @Test
        @DisplayName("defaultInitialValue 默认值为 0")
        void defaultInitialValueDefaultValue() {
            RedisProperties.CounterProperties props = new RedisProperties.CounterProperties();
            assertThat(props.getDefaultInitialValue()).isEqualTo(0L);
        }

        @Test
        @DisplayName("setter 可修改 defaultInitialValue")
        void setDefaultInitialValue() {
            RedisProperties.CounterProperties props = new RedisProperties.CounterProperties();
            props.setDefaultInitialValue(100L);
            assertThat(props.getDefaultInitialValue()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("CacheProperties 默认值和绑定")
    class CachePropertiesTest {

        @Test
        @DisplayName("enabled 默认值为 true")
        void enabledDefaultValue() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            assertThat(props.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("defaultTtl 默认值为 3600 秒")
        void defaultTtlDefaultValue() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            assertThat(props.getDefaultTtl()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("nullValueTtl 默认值为 60 秒")
        void nullValueTtlDefaultValue() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            assertThat(props.getNullValueTtl()).isEqualTo(60L);
        }

        @Test
        @DisplayName("setter 可修改 enabled")
        void setEnabled() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            props.setEnabled(false);
            assertThat(props.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("setter 可修改 defaultTtl")
        void setDefaultTtl() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            props.setDefaultTtl(7200L);
            assertThat(props.getDefaultTtl()).isEqualTo(7200L);
        }

        @Test
        @DisplayName("setter 可修改 nullValueTtl")
        void setNullValueTtl() {
            RedisProperties.CacheProperties props = new RedisProperties.CacheProperties();
            props.setNullValueTtl(120L);
            assertThat(props.getNullValueTtl()).isEqualTo(120L);
        }
    }

    @Nested
    @DisplayName("RedisProperties 嵌套属性初始化")
    class NestedPropertiesInitializationTest {

        @Test
        @DisplayName("所有嵌套属性默认实例已初始化")
        void allNestedPropertiesInitialized() {
            RedisProperties props = new RedisProperties();
            assertThat(props.getLock()).isNotNull();
            assertThat(props.getRateLimit()).isNotNull();
            assertThat(props.getCounter()).isNotNull();
            assertThat(props.getCache()).isNotNull();
        }

        @Test
        @DisplayName("嵌套属性可通过 setter 替换")
        void nestedPropertiesCanBeReplaced() {
            RedisProperties props = new RedisProperties();
            RedisProperties.LockProperties customLock = new RedisProperties.LockProperties();
            customLock.setDefaultTimeout(10000L);
            props.setLock(customLock);
            assertThat(props.getLock().getDefaultTimeout()).isEqualTo(10000L);
        }
    }
}
