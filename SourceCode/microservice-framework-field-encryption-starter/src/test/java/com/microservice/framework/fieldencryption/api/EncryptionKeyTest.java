package com.microservice.framework.fieldencryption.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EncryptionKey 测试
 * <p>
 * 验证不可变性、过期判断、活跃状态和 Optional 返回值。
 *
 * @author Andy Yang
 */
class EncryptionKeyTest {

    @Test
    @DisplayName("EncryptionKey 创建时应正确存储所有字段")
    void encryptionKeyCreationShouldStoreAllFields() {
        Instant now = Instant.now();
        Instant expires = now.plus(java.time.Duration.ofDays(90));

        EncryptionKey key = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, expires, true);

        assertThat(key.getKeyId()).isEqualTo("key-1");
        assertThat(key.getAlgorithm()).isEqualTo("AES/GCM/NoPadding");
        assertThat(key.getCreatedAt()).isEqualTo(now);
        assertThat(key.getExpiresAt()).contains(expires);
        assertThat(key.isActive()).isTrue();
    }

    @Test
    @DisplayName("EncryptionKey 永不过期密钥应返回空 expiresAt")
    void encryptionKeyWithNoExpiryShouldReturnEmptyExpiresAt() {
        Instant now = Instant.now();
        EncryptionKey key = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, null, true);

        assertThat(key.getExpiresAt()).isEmpty();
        assertThat(key.isExpired()).isFalse();
    }

    @Test
    @DisplayName("EncryptionKey 过期判断应正确工作")
    void encryptionKeyExpiredCheckShouldWork() {
        Instant past = Instant.now().minus(java.time.Duration.ofDays(1));
        Instant future = Instant.now().plus(java.time.Duration.ofDays(90));

        EncryptionKey expiredKey = new EncryptionKey("key-1", "AES/GCM/NoPadding", past, past, true);
        assertThat(expiredKey.isExpired()).isTrue();

        EncryptionKey validKey = new EncryptionKey("key-2", "AES/GCM/NoPadding", past, future, true);
        assertThat(validKey.isExpired()).isFalse();
    }

    @Test
    @DisplayName("EncryptionKey isActive 应正确返回状态")
    void encryptionKeyIsActiveShouldReturnCorrectState() {
        Instant now = Instant.now();
        EncryptionKey activeKey = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, null, true);
        EncryptionKey inactiveKey = new EncryptionKey("key-2", "AES/GCM/NoPadding", now, null, false);

        assertThat(activeKey.isActive()).isTrue();
        assertThat(inactiveKey.isActive()).isFalse();
    }

    @Test
    @DisplayName("EncryptionKey 必填字段为 null 时应抛出 NullPointerException")
    void encryptionKeyRequiredNullFieldsShouldThrowNullPointerException() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new EncryptionKey(null, "AES/GCM", now, null, true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EncryptionKey("key-1", null, now, null, true))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EncryptionKey("key-1", "AES/GCM", null, null, true))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("EncryptionKey equals 和 hashCode 应正确工作")
    void encryptionKeyEqualsAndHashCodeShouldWork() {
        Instant now = Instant.now();
        Instant expires = now.plus(java.time.Duration.ofDays(90));

        EncryptionKey key1 = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, expires, true);
        EncryptionKey key2 = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, expires, true);
        EncryptionKey key3 = new EncryptionKey("key-2", "AES/GCM/NoPadding", now, expires, true);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        assertThat(key1).isNotEqualTo(key3);
    }

    @Test
    @DisplayName("EncryptionKey toString 应包含关键字段")
    void encryptionKeyToStringShouldContainKeyFields() {
        Instant now = Instant.now();
        EncryptionKey key = new EncryptionKey("key-1", "AES/GCM/NoPadding", now, null, true);

        String str = key.toString();
        assertThat(str).contains("key-1");
        assertThat(str).contains("AES/GCM/NoPadding");
        assertThat(str).contains("isActive=true");
    }
}
