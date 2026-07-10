package com.microservice.framework.fieldencryption;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FieldEncryptionProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class FieldEncryptionPropertiesTest {

    @Test
    @DisplayName("默认启用状态应为 true")
    void defaultEnabledShouldBeTrue() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        assertThat(properties.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("默认算法配置应与设计规格一致")
    void defaultAlgorithmPropertiesShouldMatchSpecification() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        FieldEncryptionProperties.AlgorithmProperties algorithm = properties.getAlgorithm();

        assertThat(algorithm.getName()).isEqualTo("AES/GCM/NoPadding");
        assertThat(algorithm.getKeySize()).isEqualTo(256);
    }

    @Test
    @DisplayName("默认密钥配置应与设计规格一致")
    void defaultKeyPropertiesShouldMatchSpecification() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        FieldEncryptionProperties.KeyProperties key = properties.getKey();

        assertThat(key.getRotationDays()).isEqualTo(90);
        assertThat(key.getKeyIdPrefix()).isEqualTo("key-");
    }

    @Test
    @DisplayName("默认字段配置应与设计规格一致")
    void defaultFieldPropertiesShouldMatchSpecification() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();
        FieldEncryptionProperties.FieldProperties field = properties.getField();

        assertThat(field.getEncryptedFields()).isEmpty();
        assertThat(field.getDetectEncrypted()).isTrue();
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        FieldEncryptionProperties properties = new FieldEncryptionProperties();

        properties.setEnabled(false);
        assertThat(properties.getEnabled()).isFalse();

        properties.getAlgorithm().setName("AES/CBC/PKCS5Padding");
        properties.getAlgorithm().setKeySize(128);
        assertThat(properties.getAlgorithm().getName()).isEqualTo("AES/CBC/PKCS5Padding");
        assertThat(properties.getAlgorithm().getKeySize()).isEqualTo(128);

        properties.getKey().setRotationDays(30);
        properties.getKey().setKeyIdPrefix("enc-");
        assertThat(properties.getKey().getRotationDays()).isEqualTo(30);
        assertThat(properties.getKey().getKeyIdPrefix()).isEqualTo("enc-");

        properties.getField().setEncryptedFields(Arrays.asList("phone", "email", "idCard"));
        properties.getField().setDetectEncrypted(false);
        assertThat(properties.getField().getEncryptedFields()).containsExactly("phone", "email", "idCard");
        assertThat(properties.getField().getDetectEncrypted()).isFalse();
    }
}
