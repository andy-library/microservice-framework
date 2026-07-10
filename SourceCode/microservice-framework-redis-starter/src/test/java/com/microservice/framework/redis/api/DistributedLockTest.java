package com.microservice.framework.redis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DistributedLock 接口契约测试
 * <p>
 * 验证接口方法签名和行为语义，使用匿名实现作为测试桩。
 *
 * @author Andy Yang
 */
class DistributedLockTest {

    @Nested
    @DisplayName("tryLock 方法契约")
    class TryLockContract {

        @Test
        @DisplayName("tryLock(key) 返回 boolean 表示是否成功获取锁")
        void tryLockWithKeyReturnsBoolean() {
            DistributedLock lock = new StubDistributedLock(true);
            assertThat(lock.tryLock("test-key")).isTrue();

            DistributedLock failLock = new StubDistributedLock(false);
            assertThat(failLock.tryLock("test-key")).isFalse();
        }

        @Test
        @DisplayName("tryLock(key, timeout) 返回 boolean 表示是否在指定时间内获取锁")
        void tryLockWithTimeoutReturnsBoolean() {
            DistributedLock lock = new StubDistributedLock(true);
            assertThat(lock.tryLock("test-key", 1000)).isTrue();
        }

        @Test
        @DisplayName("tryLock(key, timeout, expire) 返回 boolean 表示是否获取锁并指定过期时间")
        void tryLockWithTimeoutAndExpireReturnsBoolean() {
            DistributedLock lock = new StubDistributedLock(true);
            assertThat(lock.tryLock("test-key", 1000, 30000)).isTrue();
        }
    }

    @Nested
    @DisplayName("unlock 方法契约")
    class UnlockContract {

        @Test
        @DisplayName("unlock(key) 返回 boolean 表示是否成功释放锁")
        void unlockReturnsBoolean() {
            DistributedLock lock = new StubDistributedLock(true);
            assertThat(lock.unlock("test-key")).isTrue();

            DistributedLock failLock = new StubDistributedLock(false);
            assertThat(failLock.unlock("test-key")).isFalse();
        }
    }

    @Nested
    @DisplayName("isLocked 方法契约")
    class IsLockedContract {

        @Test
        @DisplayName("isLocked(key) 返回 boolean 表示锁是否被持有")
        void isLockedReturnsBoolean() {
            DistributedLock locked = new StubDistributedLock(true);
            assertThat(locked.isLocked("test-key")).isTrue();

            DistributedLock unlocked = new StubDistributedLock(false);
            assertThat(unlocked.isLocked("test-key")).isFalse();
        }
    }

    /**
     * 测试桩实现，所有方法返回预设的 boolean 值
     */
    private static class StubDistributedLock implements DistributedLock {

        private final boolean result;

        StubDistributedLock(boolean result) {
            this.result = result;
        }

        @Override
        public boolean tryLock(String key) {
            return result;
        }

        @Override
        public boolean tryLock(String key, long timeout) {
            return result;
        }

        @Override
        public boolean tryLock(String key, long timeout, long expire) {
            return result;
        }

        @Override
        public boolean unlock(String key) {
            return result;
        }

        @Override
        public boolean isLocked(String key) {
            return result;
        }
    }
}
