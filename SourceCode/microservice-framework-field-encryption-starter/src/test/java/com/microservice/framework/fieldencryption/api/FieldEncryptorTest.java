package com.microservice.framework.fieldencryption.api;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.autoconfigure.FieldEncryptionAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
}
