package com.microservice.framework.redis.core;

import com.microservice.framework.redis.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheImplTest {

    private RedisCacheImpl cache;
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        RedisProperties.CacheProperties properties = new RedisProperties.CacheProperties();
        cache = new RedisCacheImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("put(key, value, ttl) 应拒绝非正数 TTL")
    void putShouldRejectNonPositiveTtl() {
        assertThatThrownBy(() -> cache.put("user:1", "value", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TTL");
    }

    @Test
    @DisplayName("get(key) 应拒绝空白 key")
    void getShouldRejectBlankKey() {
        assertThatThrownBy(() -> cache.get(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");
    }

    @Test
    @DisplayName("get(key, type) 类型不匹配时应明确失败")
    void getWithTypeShouldFailWhenTypeMismatches() {
        when(valueOperations.get("framework:cache:user:1")).thenReturn(123);

        assertThatThrownBy(() -> cache.get("user:1", String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.class.getName());
    }

    @Test
    @DisplayName("evictByPattern(pattern) 应拒绝空白模式")
    void evictByPatternShouldRejectBlankPattern() {
        assertThatThrownBy(() -> cache.evictByPattern(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    @DisplayName("evictAll(keys) 应先校验全部 key，避免部分删除")
    void evictAllShouldValidateAllKeysBeforeDeleting() {
        assertThatThrownBy(() -> cache.evictAll(List.of("user:1", " ", "user:2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key");

        verify(redisTemplate, never()).delete(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    @DisplayName("evictByPattern(pattern) 应拒绝全量通配模式")
    void evictByPatternShouldRejectGlobalWildcardPattern() {
        assertThatThrownBy(() -> cache.evictByPattern("*"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("global wildcard");
    }
}
