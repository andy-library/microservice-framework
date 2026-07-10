package com.microservice.framework.redis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RateLimiter 接口契约测试
 * <p>
 * 验证接口方法签名和行为语义，使用匿名实现作为测试桩。
 *
 * @author Andy Yang
 */
class RateLimiterTest {

    @Nested
    @DisplayName("tryAcquire 方法契约")
    class TryAcquireContract {

        @Test
        @DisplayName("tryAcquire(key) 返回 boolean 表示是否获取许可")
        void tryAcquireWithKeyReturnsBoolean() {
            RateLimiter limiter = new StubRateLimiter(true, 0L, 50L);
            assertThat(limiter.tryAcquire("test-key")).isTrue();

            RateLimiter failLimiter = new StubRateLimiter(false, 0L, 0L);
            assertThat(failLimiter.tryAcquire("test-key")).isFalse();
        }

        @Test
        @DisplayName("tryAcquire(key, permits) 返回 boolean 表示是否获取指定数量的许可")
        void tryAcquireWithPermitsReturnsBoolean() {
            RateLimiter limiter = new StubRateLimiter(true, 0L, 50L);
            assertThat(limiter.tryAcquire("test-key", 5)).isTrue();
        }

        @Test
        @DisplayName("tryAcquire(key, permits, period) 返回 boolean 表示是否在指定窗口内获取许可")
        void tryAcquireWithPermitsAndPeriodReturnsBoolean() {
            RateLimiter limiter = new StubRateLimiter(true, 0L, 50L);
            assertThat(limiter.tryAcquire("test-key", 100, 1)).isTrue();
        }
    }

    @Nested
    @DisplayName("acquire 方法契约")
    class AcquireContract {

        @Test
        @DisplayName("acquire(key) 返回等待时间，0 表示立即获取")
        void acquireReturnsWaitTime() {
            RateLimiter immediateLimiter = new StubRateLimiter(true, 0L, 50L);
            assertThat(immediateLimiter.acquire("test-key")).isEqualTo(0L);

            RateLimiter waitingLimiter = new StubRateLimiter(false, 500L, 0L);
            assertThat(waitingLimiter.acquire("test-key")).isEqualTo(500L);
        }
    }

    @Nested
    @DisplayName("getAvailablePermits 方法契约")
    class GetAvailablePermitsContract {

        @Test
        @DisplayName("getAvailablePermits(key) 返回剩余可用许可数")
        void getAvailablePermitsReturnsCount() {
            RateLimiter limiter = new StubRateLimiter(true, 0L, 50L);
            assertThat(limiter.getAvailablePermits("test-key")).isEqualTo(50L);
        }
    }

    /**
     * 测试桩实现
     */
    private static class StubRateLimiter implements RateLimiter {

        private final boolean acquireResult;
        private final long waitTime;
        private final long availablePermits;

        StubRateLimiter(boolean acquireResult, long waitTime, long availablePermits) {
            this.acquireResult = acquireResult;
            this.waitTime = waitTime;
            this.availablePermits = availablePermits;
        }

        @Override
        public boolean tryAcquire(String key) {
            return acquireResult;
        }

        @Override
        public boolean tryAcquire(String key, int permits) {
            return acquireResult;
        }

        @Override
        public boolean tryAcquire(String key, int permits, long period) {
            return acquireResult;
        }

        @Override
        public long acquire(String key) {
            return waitTime;
        }

        @Override
        public long getAvailablePermits(String key) {
            return availablePermits;
        }
    }
}
