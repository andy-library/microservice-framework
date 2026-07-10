package com.microservice.framework.redis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisCache 接口契约测试
 * <p>
 * 验证接口方法签名和行为语义，使用匿名实现作为测试桩。
 *
 * @author Andy Yang
 */
class RedisCacheTest {

    @Nested
    @DisplayName("get 方法契约")
    class GetContract {

        @Test
        @DisplayName("get(key) 返回缓存值或 null")
        void getReturnsValueOrNull() {
            RedisCache cache = new StubRedisCache("cached-value");
            assertThat(cache.<String>get("test-key")).isEqualTo("cached-value");

            RedisCache emptyCache = new StubRedisCache(null);
            assertThat(emptyCache.<String>get("test-key")).isNull();
        }

        @Test
        @DisplayName("get(key, type) 返回指定类型的缓存值")
        void getWithTypeReturnsTypedValue() {
            RedisCache cache = new StubRedisCache("typed-value");
            assertThat(cache.get("test-key", String.class)).isEqualTo("typed-value");
        }
    }

    @Nested
    @DisplayName("put 方法契约")
    class PutContract {

        @Test
        @DisplayName("put(key, value) 使用默认 TTL 写入缓存")
        void putWithDefaultTtl() {
            RedisCache cache = new StubRedisCache(null);
            cache.put("test-key", "value");
            assertThat(cache.<String>get("test-key")).isEqualTo("value");
        }

        @Test
        @DisplayName("put(key, value, ttl) 指定 TTL 写入缓存")
        void putWithExplicitTtl() {
            RedisCache cache = new StubRedisCache(null);
            cache.put("test-key", "value", 60);
            assertThat(cache.<String>get("test-key")).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("evict 方法契约")
    class EvictContract {

        @Test
        @DisplayName("evict(key) 返回 boolean 表示是否成功删除")
        void evictReturnsBoolean() {
            RedisCache cache = new StubRedisCache(null);
            assertThat(cache.evict("test-key")).isTrue();
        }
    }

    /**
     * 测试桩实现，记录最后一次写入的值
     */
    private static class StubRedisCache implements RedisCache {

        private Object storedValue;

        StubRedisCache(Object initialValue) {
            this.storedValue = initialValue;
        }

        @Override
        public <T> T get(String key) {
            return (T) storedValue;
        }

        @Override
        public <T> T get(String key, Class<T> type) {
            if (storedValue == null) {
                return null;
            }
            if (type.isInstance(storedValue)) {
                return type.cast(storedValue);
            }
            return (T) storedValue;
        }

        @Override
        public void put(String key, Object value) {
            this.storedValue = value;
        }

        @Override
        public void put(String key, Object value, long ttl) {
            this.storedValue = value;
        }

        @Override
        public boolean evict(String key) {
            this.storedValue = null;
            return true;
        }

        @Override
        public void evictAll(java.util.List<String> keys) {
            this.storedValue = null;
        }

        @Override
        public long evictByPattern(String pattern) {
            boolean existed = storedValue != null;
            this.storedValue = null;
            return existed ? 1 : 0;
        }
    }
}
