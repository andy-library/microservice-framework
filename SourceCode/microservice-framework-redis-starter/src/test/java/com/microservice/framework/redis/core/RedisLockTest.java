package com.microservice.framework.redis.core;

import com.microservice.framework.redis.RedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisLock 实现测试
 * <p>
 * 使用 Mock StringRedisTemplate 验证锁的获取、释放和检查逻辑。
 *
 * @author Andy Yang
 */
class RedisLockTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisProperties.LockProperties lockProperties;
    private RedisLock redisLock;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        lockProperties = new RedisProperties.LockProperties();
        redisLock = new RedisLock(redisTemplate, lockProperties);
    }

    @Nested
    @DisplayName("tryLock 方法")
    class TryLock {

        @Test
        @DisplayName("tryLock(key) 成功获取锁时返回 true")
        void tryLockWithDefaultTimeoutSuccess() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            boolean result = redisLock.tryLock("order-123");

            assertThat(result).isTrue();
            verify(valueOperations).setIfAbsent(
                    eq("framework:lock:order-123"),
                    anyString(),
                    eq(lockProperties.getDefaultExpire()),
                    any());
        }

        @Test
        @DisplayName("tryLock(key, timeout=0) 获取锁失败时立即返回 false（无等待重试）")
        void tryLockWithZeroTimeoutFailure() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(false);

            boolean result = redisLock.tryLock("order-123", 0);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("tryLock(key, timeout) 使用指定超时时间")
        void tryLockWithCustomTimeout() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            boolean result = redisLock.tryLock("order-123", 5000);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("tryLock(key, timeout, expire) 使用指定超时和过期时间")
        void tryLockWithCustomTimeoutAndExpire() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);

            boolean result = redisLock.tryLock("order-123", 5000, 60000);

            assertThat(result).isTrue();
            verify(valueOperations).setIfAbsent(
                    eq("framework:lock:order-123"),
                    anyString(),
                    eq(60000L),
                    any());
        }

        @Test
        @DisplayName("tryLock 返回 null 时视为获取失败")
        void tryLockWithNullResponse() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(null);

            boolean result = redisLock.tryLock("order-123");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("unlock 方法")
    class Unlock {

        @Test
        @DisplayName("unlock(key) 只使用当前线程成功获取锁时保存的 owner token")
        void unlockShouldUseAcquiredOwnerTokenWithoutReadingCurrentRedisValue() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(redisTemplate.execute(any(), any(), anyString())).thenReturn(1L);

            assertThat(redisLock.tryLock("order-123")).isTrue();
            assertThat(redisLock.unlock("order-123")).isTrue();

            org.mockito.ArgumentCaptor<String> token = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(
                    eq("framework:lock:order-123"), token.capture(), anyLong(), any());
            verify(redisTemplate).execute(any(), eq(java.util.List.of("framework:lock:order-123")),
                    eq(token.getValue()));
            verify(valueOperations, never()).get(anyString());
        }

        @Test
        @DisplayName("unlock(key) 没有当前线程 owner token 时绝不执行删除脚本")
        void unlockWithoutCurrentThreadOwnerTokenMustNotDeleteAnotherOwnerLock() {
            when(valueOperations.get("framework:lock:order-123")).thenReturn("another-owner-token");

            assertThat(redisLock.unlock("order-123")).isFalse();

            verify(redisTemplate, never()).execute(any(), any(), anyString());
        }

        @Test
        @DisplayName("unlock(key) 成功释放锁时返回 true")
        void unlockSuccess() {
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(redisTemplate.execute(any(), any(), anyString())).thenReturn(1L);

            assertThat(redisLock.tryLock("order-123")).isTrue();
            boolean result = redisLock.unlock("order-123");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("unlock(key) 锁不存在时返回 false")
        void unlockWhenLockNotExists() {
            when(valueOperations.get(anyString())).thenReturn(null);

            boolean result = redisLock.unlock("order-123");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("unlock(key) 非持有者释放时返回 false")
        void unlockWhenNotOwner() {
            when(valueOperations.get(anyString())).thenReturn("lock-value-uuid");

            boolean result = redisLock.unlock("order-123");

            assertThat(result).isFalse();
            verify(redisTemplate, never()).execute(any(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("isLocked 方法")
    class IsLocked {

        @Test
        @DisplayName("isLocked(key) 锁被持有时返回 true")
        void isLockedWhenKeyExists() {
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            boolean result = redisLock.isLocked("order-123");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isLocked(key) 锁未被持有时返回 false")
        void isLockedWhenKeyNotExists() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            boolean result = redisLock.isLocked("order-123");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isLocked(key) hasKey 返回 null 时视为未锁定")
        void isLockedWhenNullResponse() {
            when(redisTemplate.hasKey(anyString())).thenReturn(null);

            boolean result = redisLock.isLocked("order-123");

            assertThat(result).isFalse();
        }
    }
}
