package com.microservice.framework.fieldencryption.autoconfigure;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.api.EncryptionKey;
import com.microservice.framework.fieldencryption.api.FieldEncryptor;
import com.microservice.framework.fieldencryption.api.KeyProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Field Encryption Starter 自动配置
 * <p>
 * 根据 {@code framework.field-encryption.enabled} 属性决定是否激活，默认启用。
 * 注册以下 Bean：
 * - {@link KeyProvider}：密钥生命周期管理
 * - {@link FieldEncryptor}：字段级加密/解密
 * <p>
 * 当未提供自定义实现时，注册基于内存的默认实现：
 * - {@link DefaultKeyProvider}：基于 ConcurrentHashMap 的密钥存储
 * - {@link DefaultFieldEncryptor}：基于 AES/GCM 的加密/解密（当 JDK 支持时）
 *
 * @author Andy Yang
 */
@AutoConfiguration
@EnableConfigurationProperties(FieldEncryptionProperties.class)
@ConditionalOnProperty(prefix = "framework.field-encryption", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FieldEncryptionAutoConfiguration {

    /**
     * 注册 KeyProvider Bean
     * <p>
     * 当容器中不存在 KeyProvider 时，注册基于内存的默认实现。
     * 默认实现自动生成初始活跃密钥。
     *
     * @param properties 字段加密配置属性
     * @return KeyProvider 实例
     */
    @Bean
    @ConditionalOnMissingBean(KeyProvider.class)
    public KeyProvider keyProvider(FieldEncryptionProperties properties) {
        return new DefaultKeyProvider(properties);
    }

    /**
     * 注册 FieldEncryptor Bean
     * <p>
     * 当容器中不存在 FieldEncryptor 时，注册默认实现。
     * 默认实现使用 KeyProvider 获取密钥进行加密/解密。
     *
     * @param keyProvider 密钥提供者
     * @param properties  字段加密配置属性
     * @return FieldEncryptor 实例
     */
    @Bean
    @ConditionalOnMissingBean(FieldEncryptor.class)
    public FieldEncryptor fieldEncryptor(KeyProvider keyProvider, FieldEncryptionProperties properties) {
        return new DefaultFieldEncryptor(keyProvider, properties);
    }

    // ========================================================================
    // 默认内部实现
    // ========================================================================

    /**
     * 基于 ConcurrentHashMap 的默认 KeyProvider 实现
     * <p>
     * 仅用于开发和测试环境。生产环境应替换为安全的密钥管理服务（如 KMS）。
     */
    static class DefaultKeyProvider implements KeyProvider {

        private final ConcurrentHashMap<String, EncryptionKey> keyStore = new ConcurrentHashMap<>();
        private final AtomicLong keyCounter = new AtomicLong(0);
        private volatile EncryptionKey activeKey;
        private final FieldEncryptionProperties properties;
        private final SecureRandom secureRandom = new SecureRandom();

        DefaultKeyProvider(FieldEncryptionProperties properties) {
            this.properties = properties;
            // Initialize with an active key
            this.activeKey = generateNewKey();
            keyStore.put(activeKey.getKeyId(), activeKey);
        }

        private EncryptionKey generateNewKey() {
            String keyId = properties.getKey().getKeyIdPrefix() + keyCounter.incrementAndGet();
            Instant now = Instant.now();
            Instant expiresAt = now.plus(java.time.Duration.ofDays(properties.getKey().getRotationDays()));
            return new EncryptionKey(keyId, properties.getAlgorithm().getName(), now, expiresAt, true);
        }

        @Override
        public Optional<EncryptionKey> getKey(String keyId) {
            return Optional.ofNullable(keyStore.get(keyId));
        }

        @Override
        public EncryptionKey getActiveKey() {
            return activeKey;
        }

        @Override
        public EncryptionKey rotateKey() {
            // Deactivate current active key
            EncryptionKey oldActive = activeKey;
            EncryptionKey deactivatedOld = new EncryptionKey(
                    oldActive.getKeyId(), oldActive.getAlgorithm(),
                    oldActive.getCreatedAt(), oldActive.getExpiresAt().orElse(null), false);
            keyStore.put(deactivatedOld.getKeyId(), deactivatedOld);

            // Generate new active key
            EncryptionKey newActive = generateNewKey();
            keyStore.put(newActive.getKeyId(), newActive);
            activeKey = newActive;

            return newActive;
        }

        @Override
        public void deactivateKey(String keyId) {
            EncryptionKey key = keyStore.get(keyId);
            if (key != null) {
                EncryptionKey deactivated = new EncryptionKey(
                        key.getKeyId(), key.getAlgorithm(),
                        key.getCreatedAt(), key.getExpiresAt().orElse(null), false);
                keyStore.put(deactivated.getKeyId(), deactivated);
            }
        }
    }

    /**
     * 基于 AES/GCM 的默认 FieldEncryptor 实现
     * <p>
     * 使用 JDK 提供的 AES/GCM/NoPadding 加密算法。
     * 加密后的值格式为：keyIdPrefix + Base64(GCM_nonce | ciphertext | authTag)。
     */
    static class DefaultFieldEncryptor implements FieldEncryptor {

        private final KeyProvider keyProvider;
        private final FieldEncryptionProperties properties;
        private final SecureRandom secureRandom = new SecureRandom();

        DefaultFieldEncryptor(KeyProvider keyProvider, FieldEncryptionProperties properties) {
            this.keyProvider = keyProvider;
            this.properties = properties;
        }

        @Override
        public String encrypt(String plaintext) {
            if (plaintext == null) {
                return null;
            }
            EncryptionKey activeKey = keyProvider.getActiveKey();
            try {
                // Generate nonce (12 bytes for GCM)
                byte[] nonce = new byte[12];
                secureRandom.nextBytes(nonce);

                // Generate AES key from keyId (deterministic for same keyId)
                byte[] aesKey = deriveKeyMaterial(activeKey.getKeyId(), properties.getAlgorithm().getKeySize());

                java.security.spec.AlgorithmParameterSpec gcmSpec =
                        new javax.crypto.spec.GCMParameterSpec(128, nonce);
                javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(properties.getAlgorithm().getName());
                javax.crypto.spec.SecretKeySpec keySpec =
                        new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
                cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

                byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

                // Combine nonce + ciphertext (GCM includes auth tag)
                byte[] combined = new byte[nonce.length + ciphertext.length];
                System.arraycopy(nonce, 0, combined, 0, nonce.length);
                System.arraycopy(ciphertext, 0, combined, nonce.length, ciphertext.length);

                // Format: keyPrefix + Base64(combined)
                return activeKey.getKeyId() + ":" + Base64.getEncoder().encodeToString(combined);
            } catch (Exception e) {
                throw new RuntimeException("Encryption failed for keyId: " + activeKey.getKeyId(), e);
            }
        }

        @Override
        public String decrypt(String ciphertext) {
            if (ciphertext == null) {
                return null;
            }
            // Parse keyId from ciphertext format: keyId:Base64(combined)
            int colonIndex = ciphertext.indexOf(':');
            if (colonIndex < 0) {
                throw new IllegalArgumentException("Invalid ciphertext format: missing keyId prefix");
            }
            String keyId = ciphertext.substring(0, colonIndex);
            String encodedData = ciphertext.substring(colonIndex + 1);

            Optional<EncryptionKey> keyOpt = keyProvider.getKey(keyId);
            if (keyOpt.isEmpty()) {
                throw new IllegalArgumentException("Unknown keyId: " + keyId);
            }
            EncryptionKey key = keyOpt.get();

            try {
                byte[] combined = Base64.getDecoder().decode(encodedData);

                // Extract nonce (first 12 bytes)
                byte[] nonce = new byte[12];
                System.arraycopy(combined, 0, nonce, 0, 12);

                // Extract ciphertext (remaining bytes, includes auth tag)
                byte[] cipherData = new byte[combined.length - 12];
                System.arraycopy(combined, 12, cipherData, 0, cipherData.length);

                byte[] aesKey = deriveKeyMaterial(key.getKeyId(), properties.getAlgorithm().getKeySize());

                java.security.spec.AlgorithmParameterSpec gcmSpec =
                        new javax.crypto.spec.GCMParameterSpec(128, nonce);
                javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(properties.getAlgorithm().getName());
                javax.crypto.spec.SecretKeySpec keySpec =
                        new javax.crypto.spec.SecretKeySpec(aesKey, "AES");
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, gcmSpec);

                byte[] plaintextBytes = cipher.doFinal(cipherData);
                return new String(plaintextBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Decryption failed for keyId: " + keyId, e);
            }
        }

        @Override
        public boolean isEncrypted(String value) {
            if (value == null) {
                return false;
            }
            String prefix = properties.getKey().getKeyIdPrefix();
            if (value.startsWith(prefix) && value.contains(":")) {
                int colonIndex = value.indexOf(':');
                String keyId = value.substring(0, colonIndex);
                return keyProvider.getKey(keyId).isPresent();
            }
            return false;
        }

        /**
         * 从 keyId 派生密钥材料
         * <p>
         * 使用 SHA-256 哈希将 keyId 映射为固定长度的 AES 密钥。
         * 这是简化的实现，生产环境应使用 KMS 提供的真实密钥材料。
         */
        private byte[] deriveKeyMaterial(String keyId, int keySizeBits) {
            int keySizeBytes = keySizeBits / 8;
            byte[] hash;
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                hash = digest.digest(keyId.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }

            // For AES-256, SHA-256 produces exactly 32 bytes
            // For AES-128, truncate to 16 bytes
            byte[] keyMaterial = new byte[keySizeBytes];
            if (hash.length >= keySizeBytes) {
                System.arraycopy(hash, 0, keyMaterial, 0, keySizeBytes);
            } else {
                // Pad with repeated hash for key sizes > 32 bytes
                int offset = 0;
                while (offset < keySizeBytes) {
                    int copyLen = Math.min(hash.length, keySizeBytes - offset);
                    System.arraycopy(hash, 0, keyMaterial, offset, copyLen);
                    offset += copyLen;
                }
            }
            return keyMaterial;
        }
    }
}
