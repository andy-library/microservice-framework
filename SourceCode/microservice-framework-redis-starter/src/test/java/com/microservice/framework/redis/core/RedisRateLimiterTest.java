package com.microservice.framework.redis.core;

import com.microservice.framework.redis.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisRateLimiter 实现测试
 * <p>
 * 使用 Mock StringRedisTemplate 验证限流逻辑。
 *
 * @author Andy Yang
 */
class RedisRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private RedisProperties.RateLimitProperties rateLimitProperties;
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        rateLimitProperties = new RedisProperties.RateLimitProperties();
        rateLimiter = new RedisRateLimiter(redisTemplate, rateLimitProperties);
    }

    @Nested
    @DisplayName("tryAcquire 方法")
    class TryAcquire {

        @Test
        @DisplayName("tryAcquire(key) 当前计数未超限时返回 true")
        void tryAcquireWithinLimit() {
            // defaultPermits=100, 计数为1 <= 100 则允许
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(1L);

            boolean result = rateLimiter.tryAcquire("api-login");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("tryAcquire(key) 当前计数超限时返回 false")
        void tryAcquireOverLimit() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(101L);

            boolean result = rateLimiter.tryAcquire("api-login");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("tryAcquire(key, permits) 应原子地消耗请求的 permits，并使用默认窗口上限")
        void tryAcquireWithPermitsConsumesRequestedPermitsAtomically() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(1L);

            boolean result = rateLimiter.tryAcquire("api-login", 5);

            assertThat(result).isTrue();
            org.mockito.ArgumentCaptor<RedisScript<Long>> script = org.mockito.ArgumentCaptor.forClass(RedisScript.class);
            verify(redisTemplate).execute(script.capture(), eq(java.util.List.of("framework:rate-limit:api-login")),
                    eq("5"), eq(String.valueOf(rateLimitProperties.getDefaultPermits())),
                    eq(String.valueOf(rateLimitProperties.getDefaultPeriod())));
            assertThat(script.getValue().getScriptAsString())
                    .contains("incrby")
                    .contains("current + requested > limit");
        }

        @Test
        @DisplayName("tryAcquire(key, permits, period) 使用指定限流参数")
        void tryAcquireWithCustomParams() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(10L);

            boolean result = rateLimiter.tryAcquire("api-login", 50, 10);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("tryAcquire 返回 null 时视为限流失败")
        void tryAcquireWithNullResponse() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(null);

            boolean result = rateLimiter.tryAcquire("api-login");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("acquire 方法")
    class Acquire {

        @Test
        @DisplayName("acquire(key) 未超限时返回 0")
        void acquireWithinLimitReturnsZero() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(1L);

            long waitTime = rateLimiter.acquire("api-login");

            assertThat(waitTime).isEqualTo(0L);
        }

        @Test
        @DisplayName("acquire(key) 超限时返回等待时间")
        void acquireOverLimitReturnsWaitTime() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(101L);
            when(redisTemplate.getExpire(anyString())).thenReturn(500L);

            long waitTime = rateLimiter.acquire("api-login");

            assertThat(waitTime).isEqualTo(500000L);
        }

        @Test
        @DisplayName("acquire(key) 超限且 TTL 不可用时返回默认窗口时间")
        void acquireOverLimitWithNoTtlReturnsDefaultPeriod() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(101L);
            when(redisTemplate.getExpire(anyString())).thenReturn(-1L);

            long waitTime = rateLimiter.acquire("api-login");

            assertThat(waitTime).isEqualTo(rateLimitProperties.getDefaultPeriod() * 1000L);
        }
    }

    @Nested
    @DisplayName("getAvailablePermits 方法")
    class GetAvailablePermits {

        @Test
        @DisplayName("getAvailablePermits(key) 返回剩余可用许可数")
        void getAvailablePermitsReturnsRemaining() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(80L);

            long remaining = rateLimiter.getAvailablePermits("api-login");

            assertThat(remaining).isEqualTo(80L);
        }

        @Test
        @DisplayName("getAvailablePermits(key) 返回 null 时使用默认 permits")
        void getAvailablePermitsWithNullUsesDefault() {
            when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                    .thenReturn(null);

            long remaining = rateLimiter.getAvailablePermits("api-login");

            assertThat(remaining).isEqualTo(rateLimitProperties.getDefaultPermits());
        }
    }
}
