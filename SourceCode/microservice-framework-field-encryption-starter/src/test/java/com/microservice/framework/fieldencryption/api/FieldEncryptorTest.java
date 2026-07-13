package com.microservice.framework.fieldencryption.api;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.autoconfigure.FieldEncryptionAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FieldEncryptor 接口测试
 * <p>
 * 通过 ApplicationContextRunner 验证 FieldEncryptor 接口的行为，
 * 确保默认实现满足加密/解密和检测的接口契约。
 *
 * @author Andy Yang
 */
class FieldEncryptorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FieldEncryptionAutoConfiguration.class));

    @Test
    @DisplayName("encrypt 加密后的值应包含密钥前缀")
    void encryptedValueShouldContainKeyPrefix() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            String encrypted = encryptor.encrypt("sensitive-data");

            assertThat(encrypted).isNotNull();
            assertThat(encrypted).startsWith("key-");
            assertThat(encrypted).contains(":");
        });
    }

    @Test
    @DisplayName("encrypt 和 decrypt 应能还原原始值")
    void encryptAndDecryptShouldRestoreOriginalValue() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            String original = "sensitive-data-12345";
            String encrypted = encryptor.encrypt(original);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(original);
        });
    }

    @Test
    @DisplayName("isEncrypted 应正确识别加密后的值")
    void isEncryptedShouldIdentifyEncryptedValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            String encrypted = encryptor.encrypt("sensitive-data");

            assertThat(encryptor.isEncrypted(encrypted)).isTrue();
            assertThat(encryptor.isEncrypted("plain-text")).isFalse();
            assertThat(encryptor.isEncrypted(null)).isFalse();
        });
    }

    @Test
    @DisplayName("encrypt null 值应返回 null")
    void encryptNullShouldReturnNull() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            assertThat(encryptor.encrypt(null)).isNull();
        });
    }

    @Test
    @DisplayName("decrypt null 值应返回 null")
    void decryptNullShouldReturnNull() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            assertThat(encryptor.decrypt(null)).isNull();
        });
    }

    @Test
    @DisplayName("不同明文应产生不同密文")
    void differentPlaintextsShouldProduceDifferentCiphertexts() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
            String encrypted1 = encryptor.encrypt("data-1");
            String encrypted2 = encryptor.encrypt("data-2");

            assertThat(encrypted1).isNotEqualTo(encrypted2);
        });
    }

    @Test
    @DisplayName("密钥轮换后旧密文仍可解密")
    void ciphertextFromOldKeyShouldStillBeDecryptableAfterRotation() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            KeyProvider keyProvider = context.getBean(KeyProvider.class);
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);

            // Encrypt with current key
            String original = "data-before-rotation";
            String encrypted = encryptor.encrypt(original);

            // Rotate key
            EncryptionKey newKey = keyProvider.rotateKey();

            // Old ciphertext should still be decryptable
            String decrypted = encryptor.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(original);

            // New encryption should use new key
            String newEncrypted = encryptor.encrypt("data-after-rotation");
            assertThat(newEncrypted).startsWith(newKey.getKeyId());
        });
    }

    @Test
    @DisplayName("已知 keyId 不应允许伪造 AES 密文")
    void knownKeyIdMustNotAllowForgedCiphertext() throws Exception {
        String keyId = "key-public-id";
        EncryptionKey key = new EncryptionKey(
                keyId, "AES/GCM/NoPadding", java.time.Instant.now(), null, true);
        KeyProvider keyProvider = new KeyProvider() {
            @Override
            public java.util.Optional<EncryptionKey> getKey(String requestedKeyId) {
                return keyId.equals(requestedKeyId) ? java.util.Optional.of(key) : java.util.Optional.empty();
            }

            @Override
            public EncryptionKey getActiveKey() {
                return key;
            }

            @Override
            public EncryptionKey rotateKey() {
                return key;
            }

            @Override
            public void deactivateKey(String requestedKeyId) {
            }
        };

        contextRunner
                .withBean(KeyProvider.class, () -> keyProvider)
                .withBean(KeyMaterialProvider.class, () -> requestedKeyId -> new byte[32])
                .run(context -> {
                    FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);
                    String forged = encryptWithKeyDerivedFromId("forged", keyId);

                    assertThatThrownBy(() -> encryptor.decrypt(forged))
                            .isInstanceOf(RuntimeException.class);
                });
    }

    @Test
    @DisplayName("停用活跃密钥后不得继续加密")
    void deactivatingActiveKeyMustBlockNewEncryption() {
        contextRunner.run(context -> {
            KeyProvider keyProvider = context.getBean(KeyProvider.class);
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);

            keyProvider.deactivateKey(keyProvider.getActiveKey().getKeyId());

            assertThatThrownBy(() -> encryptor.encrypt("must-not-encrypt"))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    private static String encryptWithKeyDerivedFromId(String plaintext, String keyId) throws Exception {
        byte[] nonce = new byte[12];
        byte[] derivedKey = java.security.MessageDigest.getInstance("SHA-256")
                .digest(keyId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(derivedKey, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, nonce));
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] payload = new byte[nonce.length + cipherText.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        System.arraycopy(cipherText, 0, payload, nonce.length, cipherText.length);
        return keyId + ":" + java.util.Base64.getEncoder().encodeToString(payload);
    }
}
