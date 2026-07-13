package com.microservice.framework.fieldencryption.autoconfigure;

import com.microservice.framework.fieldencryption.FieldEncryptionProperties;
import com.microservice.framework.fieldencryption.api.EncryptionKey;
import com.microservice.framework.fieldencryption.api.FieldEncryptor;
import com.microservice.framework.fieldencryption.api.KeyMaterialProvider;
import com.microservice.framework.fieldencryption.api.KeyProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
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
     * Provides local key material only outside production. Production deployments
     * must provide a KMS-, vault-, or HSM-backed {@link KeyMaterialProvider}.
     */
    @Bean
    @ConditionalOnMissingBean(KeyMaterialProvider.class)
    public KeyMaterialProvider localKeyMaterialProvider(FieldEncryptionProperties properties,
                                                        Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "A KeyMaterialProvider backed by real secret material is required in prod");
        }
        return new LocalKeyMaterialProvider(properties);
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
    public FieldEncryptor fieldEncryptor(KeyProvider keyProvider,
                                         KeyMaterialProvider keyMaterialProvider,
                                         FieldEncryptionProperties properties) {
        return new DefaultFieldEncryptor(keyProvider, keyMaterialProvider, properties);
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
            EncryptionKey key = activeKey;
            if (key == null || !key.isActive() || key.isExpired()) {
                throw new IllegalStateException("No active encryption key is available");
            }
            return key;
        }

        @Override
        public synchronized EncryptionKey rotateKey() {
            // Deactivate current active key
            EncryptionKey oldActive = activeKey;
            if (oldActive != null) {
                EncryptionKey deactivatedOld = new EncryptionKey(
                        oldActive.getKeyId(), oldActive.getAlgorithm(),
                        oldActive.getCreatedAt(), oldActive.getExpiresAt().orElse(null), false);
                keyStore.put(deactivatedOld.getKeyId(), deactivatedOld);
            }

            // Generate new active key
            EncryptionKey newActive = generateNewKey();
            keyStore.put(newActive.getKeyId(), newActive);
            activeKey = newActive;

            return newActive;
        }

        @Override
        public synchronized void deactivateKey(String keyId) {
            EncryptionKey key = keyStore.get(keyId);
            if (key != null) {
                EncryptionKey deactivated = new EncryptionKey(
                        key.getKeyId(), key.getAlgorithm(),
                        key.getCreatedAt(), key.getExpiresAt().orElse(null), false);
                keyStore.put(deactivated.getKeyId(), deactivated);
                if (keyId.equals(activeKey != null ? activeKey.getKeyId() : null)) {
                    activeKey = null;
                }
            }
        }
    }

    /**
     * In-memory random key material for development and tests only.
     */
    static class LocalKeyMaterialProvider implements KeyMaterialProvider {

        private final ConcurrentHashMap<String, byte[]> materialByKeyId = new ConcurrentHashMap<>();
        private final SecureRandom secureRandom = new SecureRandom();
        private final int keySizeBytes;

        LocalKeyMaterialProvider(FieldEncryptionProperties properties) {
            int keySizeBits = properties.getAlgorithm().getKeySize();
            if (keySizeBits % Byte.SIZE != 0) {
                throw new IllegalArgumentException("AES key size must be a multiple of 8 bits");
            }
            this.keySizeBytes = keySizeBits / Byte.SIZE;
        }

        @Override
        public byte[] getKeyMaterial(String keyId) {
            byte[] material = materialByKeyId.computeIfAbsent(keyId, ignored -> {
                byte[] generated = new byte[keySizeBytes];
                secureRandom.nextBytes(generated);
                return generated;
            });
            return Arrays.copyOf(material, material.length);
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
        private final KeyMaterialProvider keyMaterialProvider;
        private final FieldEncryptionProperties properties;
        private final SecureRandom secureRandom = new SecureRandom();

        DefaultFieldEncryptor(KeyProvider keyProvider, KeyMaterialProvider keyMaterialProvider,
                              FieldEncryptionProperties properties) {
            this.keyProvider = keyProvider;
            this.keyMaterialProvider = keyMaterialProvider;
            this.properties = properties;
        }

        @Override
        public String encrypt(String plaintext) {
            if (plaintext == null) {
                return null;
            }
            EncryptionKey activeKey = keyProvider.getActiveKey();
            if (!activeKey.isActive() || activeKey.isExpired()) {
                throw new IllegalStateException("Active encryption key is not usable");
            }
            try {
                // Generate nonce (12 bytes for GCM)
                byte[] nonce = new byte[12];
                secureRandom.nextBytes(nonce);

                byte[] aesKey = getKeyMaterial(activeKey.getKeyId());

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

                byte[] aesKey = getKeyMaterial(key.getKeyId());

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

        private byte[] getKeyMaterial(String keyId) {
            byte[] material = keyMaterialProvider.getKeyMaterial(keyId);
            int expectedLength = properties.getAlgorithm().getKeySize() / Byte.SIZE;
            if (material == null || material.length != expectedLength) {
                throw new IllegalStateException("Key material for " + keyId
                        + " must contain exactly " + expectedLength + " bytes");
            }
            return Arrays.copyOf(material, material.length);
        }
    }
}
