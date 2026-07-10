package com.microservice.framework.fieldencryption.api;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * 加密密钥
 * <p>
 * 不可变对象，封装密钥的元数据信息。
 * 不包含实际密钥材料，仅用于追踪密钥的生命周期状态。
 * 实际密钥材料由 {@link KeyProvider} 管理。
 * <p>
 * 密钥生命周期：
 * - 创建（createdAt）：密钥生成时间
 * - 激活（isActive）：密钥是否处于活跃状态
 * - 过期（expiresAt）：密钥到期时间
 * - 轮换：通过 KeyProvider 进行密钥轮换
 * - 停用：通过 KeyProvider 将密钥标记为非活跃
 *
 * @author Andy Yang
 */
public final class EncryptionKey {

    private final String keyId;
    private final String algorithm;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean isActive;

    /**
     * 创建加密密钥元数据
     *
     * @param keyId      密钥唯一标识
     * @param algorithm  加密算法名称
     * @param createdAt  密钥创建时间
     * @param expiresAt  密钥过期时间（可为 null，表示永不过期）
     * @param isActive   密钥是否处于活跃状态
     */
    public EncryptionKey(String keyId, String algorithm, Instant createdAt,
                         Instant expiresAt, boolean isActive) {
        this.keyId = Objects.requireNonNull(keyId, "keyId must not be null");
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = expiresAt;
        this.isActive = isActive;
    }

    /**
     * 获取密钥唯一标识
     *
     * @return 密钥 ID
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * 获取加密算法名称
     * <p>
     * 默认为 AES/GCM/NoPadding。
     *
     * @return 算法名称
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * 获取密钥创建时间
     *
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 获取密钥过期时间
     * <p>
     * 不存在时返回空 Optional，表示密钥永不过期。
     *
     * @return 过期时间
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * 密钥是否处于活跃状态
     * <p>
     * 活跃密钥可用于加密新数据。
     * 非活跃密钥仅可用于解密旧数据。
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 密钥是否已过期
     * <p>
     * 基于当前时间判断密钥是否超过其过期时间。
     * 永不过期的密钥始终返回 false。
     *
     * @return 是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "EncryptionKey{" +
                "keyId='" + keyId + '\'' +
                ", algorithm='" + algorithm + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EncryptionKey)) {
            return false;
        }
        EncryptionKey that = (EncryptionKey) o;
        return isActive == that.isActive
                && keyId.equals(that.keyId)
                && algorithm.equals(that.algorithm)
                && createdAt.equals(that.createdAt)
                && Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        int result = keyId.hashCode();
        result = 31 * result + algorithm.hashCode();
        result = 31 * result + createdAt.hashCode();
        result = 31 * result + (expiresAt != null ? expiresAt.hashCode() : 0);
        result = 31 * result + Boolean.hashCode(isActive);
        return result;
    }
}
