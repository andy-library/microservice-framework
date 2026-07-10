package com.microservice.framework.redis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisCounter 接口契约测试
 * <p>
 * 验证接口方法签名和行为语义，使用匿名实现作为测试桩。
 *
 * @author Andy Yang
 */
class RedisCounterTest {

    @Nested
    @DisplayName("increment 方法契约")
    class IncrementContract {

        @Test
        @DisplayName("increment(key) 返回递增后的值")
        void incrementReturnsIncrementedValue() {
            RedisCounter counter = new StubRedisCounter(10L);
            assertThat(counter.increment("test-key")).isEqualTo(10L);
        }

        @Test
        @DisplayName("increment(key, delta) 返回指定步长递增后的值")
        void incrementWithDeltaReturnsIncrementedValue() {
            RedisCounter counter = new StubRedisCounter(15L);
            assertThat(counter.increment("test-key", 5)).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("decrement 方法契约")
    class DecrementContract {

        @Test
        @DisplayName("decrement(key) 返回递减后的值")
        void decrementReturnsDecrementedValue() {
            RedisCounter counter = new StubRedisCounter(8L);
            assertThat(counter.decrement("test-key")).isEqualTo(8L);
        }

        @Test
        @DisplayName("decrement(key, delta) 返回指定步长递减后的值")
        void decrementWithDeltaReturnsDecrementedValue() {
            RedisCounter counter = new StubRedisCounter(5L);
            assertThat(counter.decrement("test-key", 5)).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("get 方法契约")
    class GetContract {

        @Test
        @DisplayName("get(key) 返回当前计数值")
        void getReturnsCurrentValue() {
            RedisCounter counter = new StubRedisCounter(42L);
            assertThat(counter.get("test-key")).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("reset 方法契约")
    class ResetContract {

        @Test
        @DisplayName("reset(key, value) 返回重置后的值")
        void resetReturnsResetValue() {
            RedisCounter counter = new StubRedisCounter(0L);
            assertThat(counter.reset("test-key", 0)).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("getAndIncrement 方法契约")
    class GetAndIncrementContract {

        @Test
        @DisplayName("getAndIncrement(key) 返回递增前的值")
        void getAndIncrementReturnsPreviousValue() {
            RedisCounter counter = new StubRedisCounter(9L);
            assertThat(counter.getAndIncrement("test-key")).isEqualTo(9L);
        }
    }

    /**
     * 测试桩实现，所有方法返回预设值
     */
    private static class StubRedisCounter implements RedisCounter {

        private final long value;

        StubRedisCounter(long value) {
            this.value = value;
        }

        @Override
        public long increment(String key) {
            return value;
        }

        @Override
        public long increment(String key, long delta) {
            return value;
        }

        @Override
        public long decrement(String key) {
            return value;
        }

        @Override
        public long decrement(String key, long delta) {
            return value;
        }

        @Override
        public long get(String key) {
            return value;
        }

        @Override
        public long reset(String key, long value) {
            return value;
        }

        @Override
        public long getAndIncrement(String key) {
            return value;
        }
    }
}
