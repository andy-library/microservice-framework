package com.microservice.framework.redis.core;

import com.microservice.framework.redis.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.Cursor;

import java.util.List;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("evictByPattern(pattern) 应在有界 scan 批次内删除，而不是收集全部键")
    @SuppressWarnings("unchecked")
    void evictByPatternShouldDeleteInBoundedScanBatches() {
        RedisProperties.CacheProperties properties = new RedisProperties.CacheProperties();
        properties.setScanBatchSize(2);
        cache = new RedisCacheImpl(redisTemplate, properties);
        Cursor<String> cursor = mock(Cursor.class);
        when(redisTemplate.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true, true, true, false);
        when(cursor.next()).thenReturn(
                "framework:cache:user:1", "framework:cache:user:2", "framework:cache:user:3",
                "framework:cache:user:4", "framework:cache:user:5");
        when(redisTemplate.delete(any(Collection.class))).thenAnswer(invocation ->
                (long) invocation.<Collection<String>>getArgument(0).size());

        long deleted = cache.evictByPattern("user:*");

        org.mockito.ArgumentCaptor<Collection<String>> batches = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate, org.mockito.Mockito.times(3)).delete(batches.capture());
        assertThat(batches.getAllValues()).allSatisfy(batch -> assertThat(batch).hasSizeLessThanOrEqualTo(2));
        assertThat(deleted).isEqualTo(5L);
    }
}
