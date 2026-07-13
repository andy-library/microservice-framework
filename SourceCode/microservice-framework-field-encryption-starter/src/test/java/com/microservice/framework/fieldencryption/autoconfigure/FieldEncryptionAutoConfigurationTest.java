package com.microservice.framework.fieldencryption.autoconfigure;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.api.FieldEncryptor;
import com.microservice.framework.fieldencryption.api.KeyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Field Encryption Starter 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证自动配置的激活/禁用条件、
 * 属性绑定以及用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class FieldEncryptionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FieldEncryptionAutoConfiguration.class));

    @Test
    @DisplayName("默认配置应激活 Field Encryption 自动配置")
    void defaultConfigurationShouldActivateFieldEncryption() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("keyProvider");
            assertThat(context).hasBean("fieldEncryptor");
        });
    }

    @Test
    @DisplayName("禁用 Field Encryption Starter 后所有 Bean 不应存在")
    void disablingFieldEncryptionShouldRemoveAllBeans() {
        contextRunner.withPropertyValues("framework.field-encryption.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(KeyProvider.class);
                    assertThat(context).doesNotHaveBean(FieldEncryptor.class);
                });
    }

    @Test
    @DisplayName("自定义算法配置应正确绑定")
    void customAlgorithmPropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.field-encryption.algorithm.name=AES/CBC/PKCS5Padding",
                "framework.field-encryption.algorithm.key-size=128")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionProperties properties = context.getBean(FieldEncryptionProperties.class);
                    assertThat(properties.getAlgorithm().getName()).isEqualTo("AES/CBC/PKCS5Padding");
                    assertThat(properties.getAlgorithm().getKeySize()).isEqualTo(128);
                });
    }

    @Test
    @DisplayName("自定义密钥配置应正确绑定")
    void customKeyPropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.field-encryption.key.rotation-days=30",
                "framework.field-encryption.key.key-id-prefix=enc-")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionProperties properties = context.getBean(FieldEncryptionProperties.class);
                    assertThat(properties.getKey().getRotationDays()).isEqualTo(30);
                    assertThat(properties.getKey().getKeyIdPrefix()).isEqualTo("enc-");
                });
    }

    @Test
    @DisplayName("自定义字段配置应正确绑定")
    void customFieldPropertiesShouldBindCorrectly() {
        contextRunner.withPropertyValues(
                "framework.field-encryption.field.encrypted-fields[0]=phone",
                "framework.field-encryption.field.encrypted-fields[1]=email",
                "framework.field-encryption.field.detect-encrypted=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FieldEncryptionProperties properties = context.getBean(FieldEncryptionProperties.class);
                    assertThat(properties.getField().getEncryptedFields()).containsExactly("phone", "email");
                    assertThat(properties.getField().getDetectEncrypted()).isFalse();
                });
    }

    @Test
    @DisplayName("用户提供的 KeyProvider 应覆盖默认 Bean")
    void userProvidedKeyProviderShouldOverrideDefault() {
        KeyProvider customKeyProvider = new KeyProvider() {
            @Override
            public java.util.Optional<com.microservice.framework.fieldencryption.api.EncryptionKey> getKey(String keyId) {
                return java.util.Optional.empty();
            }

            @Override
            public com.microservice.framework.fieldencryption.api.EncryptionKey getActiveKey() {
                return new com.microservice.framework.fieldencryption.api.EncryptionKey(
                        "custom-key-1", "AES/GCM", java.time.Instant.now(), null, true);
            }

            @Override
            public com.microservice.framework.fieldencryption.api.EncryptionKey rotateKey() {
                return getActiveKey();
            }

            @Override
            public void deactivateKey(String keyId) {
            }
        };

        contextRunner.withBean("customKeyProvider", KeyProvider.class, () -> customKeyProvider)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customKeyProvider");
                    assertThat(context).doesNotHaveBean("keyProvider");
                    assertThat(context.getBean(KeyProvider.class).getActiveKey().getKeyId()).isEqualTo("custom-key-1");
                });
    }

    @Test
    @DisplayName("用户提供的 FieldEncryptor 应覆盖默认 Bean")
    void userProvidedFieldEncryptorShouldOverrideDefault() {
        FieldEncryptor customEncryptor = new FieldEncryptor() {
            @Override
            public String encrypt(String plaintext) {
                return "ENC(" + plaintext + ")";
            }

            @Override
            public String decrypt(String ciphertext) {
                return ciphertext.replace("ENC(", "").replace(")", "");
            }

            @Override
            public boolean isEncrypted(String value) {
                return value != null && value.startsWith("ENC(");
            }
        };

        contextRunner.withBean("customFieldEncryptor", FieldEncryptor.class, () -> customEncryptor)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("customFieldEncryptor");
                    assertThat(context).doesNotHaveBean("fieldEncryptor");
                    assertThat(context.getBean(FieldEncryptor.class).encrypt("test")).isEqualTo("ENC(test)");
                });
    }

    @Test
    @DisplayName("FieldEncryptor 加密和解密应能还原原始值")
    void fieldEncryptorEncryptDecryptShouldRestoreOriginalValue() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            FieldEncryptor encryptor = context.getBean(FieldEncryptor.class);

            String original = "my-secret-value";
            String encrypted = encryptor.encrypt(original);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(encrypted).isNotEqualTo(original);
            assertThat(decrypted).isEqualTo(original);
            assertThat(encryptor.isEncrypted(encrypted)).isTrue();
        });
    }

    @Test
    @DisplayName("prod 环境未提供密钥材料时应拒绝启动")
    void prodWithoutExternalKeyMaterialMustFailClosed() {
        contextRunner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }
}
