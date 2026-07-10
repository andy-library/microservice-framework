package com.microservice.framework.fieldencryption;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Field Encryption Starter 配置属性
 * <p>
 * 聚合算法、密钥和字段配置组，
 * 所有属性前缀为 {@code framework.field-encryption}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.field-encryption")
public class FieldEncryptionProperties {

    /**
     * 是否启用字段加密，默认 true
     */
    private Boolean enabled = true;

    /**
     * 算法配置
     */
    @NestedConfigurationProperty
    private AlgorithmProperties algorithm = new AlgorithmProperties();

    /**
     * 密钥配置
     */
    @NestedConfigurationProperty
    private KeyProperties key = new KeyProperties();

    /**
     * 字段配置
     */
    @NestedConfigurationProperty
    private FieldProperties field = new FieldProperties();

    // Getters and Setters

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public AlgorithmProperties getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmProperties algorithm) {
        this.algorithm = algorithm;
    }

    public KeyProperties getKey() {
        return key;
    }

    public void setKey(KeyProperties key) {
        this.key = key;
    }

    public FieldProperties getField() {
        return field;
    }

    public void setField(FieldProperties field) {
        this.field = field;
    }

    /**
     * 算法配置
     */
    public static class AlgorithmProperties {

        /**
         * 加密算法名称，默认 AES/GCM/NoPadding
         * <p>
         * 支持的算法取决于 JDK 提供的 Cipher 实现。
         * AES/GCM 是推荐的加密算法，提供认证加密能力。
         */
        @NotBlank
        private String name = "AES/GCM/NoPadding";

        /**
         * 密钥长度（位），默认 256
         * <p>
         * AES 支持 128、192、256 三种密钥长度。
         * 256 位密钥提供最高安全等级。
         */
        @Min(128)
        private Integer keySize = 256;

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getKeySize() {
            return keySize;
        }

        public void setKeySize(Integer keySize) {
            this.keySize = keySize;
        }
    }

    /**
     * 密钥配置
     */
    public static class KeyProperties {

        /**
         * 密钥轮换周期（天），默认 90
         * <p>
         * 到达轮换周期后自动生成新密钥，
         * 旧密钥标记为非活跃但仍可用于解密。
         */
        @Min(1)
        private Integer rotationDays = 90;

        /**
         * 密钥标识前缀，默认 "key-"
         * <p>
         * 加密后的值以 keyId 前缀开头，
         * 解密时通过前缀定位对应密钥。
         */
        @NotBlank
        private String keyIdPrefix = "key-";

        // Getters and Setters

        public Integer getRotationDays() {
            return rotationDays;
        }

        public void setRotationDays(Integer rotationDays) {
            this.rotationDays = rotationDays;
        }

        public String getKeyIdPrefix() {
            return keyIdPrefix;
        }

        public void setKeyIdPrefix(String keyIdPrefix) {
            this.keyIdPrefix = keyIdPrefix;
        }
    }

    /**
     * 字段配置
     */
    public static class FieldProperties {

        /**
         * 需要加密的字段名称列表
         * <p>
         * 配置哪些字段需要进行加密处理。
         * 支持通过注解或配置两种方式指定加密字段。
         */
        private List<String> encryptedFields = new ArrayList<>();

        /**
         * 是否自动检测已加密字段，默认 true
         * <p>
         * 启用后将通过密钥前缀自动识别已加密的字段值，
         * 避免对已加密值重复加密。
         */
        private Boolean detectEncrypted = true;

        // Getters and Setters

        public List<String> getEncryptedFields() {
            return encryptedFields;
        }

        public void setEncryptedFields(List<String> encryptedFields) {
            this.encryptedFields = encryptedFields;
        }

        public Boolean getDetectEncrypted() {
            return detectEncrypted;
        }

        public void setDetectEncrypted(Boolean detectEncrypted) {
            this.detectEncrypted = detectEncrypted;
        }
    }
}
