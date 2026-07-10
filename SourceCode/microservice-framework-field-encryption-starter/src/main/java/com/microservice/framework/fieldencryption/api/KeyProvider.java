package com.microservice.framework.fieldencryption.api;

import java.util.Optional;

/**
 * 密钥提供者接口
 * <p>
 * 提供密钥的获取、轮换和停用能力，实现密钥生命周期管理。
 * 支持按标识获取密钥、获取当前活跃密钥、密钥轮换和密钥停用。
 * <p>
 * 密钥隔离：
 * - 每个密钥有唯一标识（keyId），加密后的值包含 keyId 前缀
 * - 解密时通过 keyId 定位对应密钥，确保密钥隔离
 * <p>
 * 密钥轮换：
 * - 调用 rotateKey() 生成新密钥并将旧密钥标记为非活跃
 * - 非活跃密钥仍可用于解密，但不能用于加密新数据
 * - 确保密钥轮换期间业务连续性
 *
 * @author Andy Yang
 */
public interface KeyProvider {

    /**
     * 通过密钥标识获取密钥
     * <p>
     * 根据唯一标识查询对应的密钥元数据。
     *
     * @param keyId 密钥唯一标识
     * @return 对应的密钥元数据，不存在时返回空 Optional
     */
    Optional<EncryptionKey> getKey(String keyId);

    /**
     * 获取当前活跃密钥
     * <p>
     * 活跃密钥用于加密新数据，每次加密操作都应使用活跃密钥。
     *
     * @return 当前活跃密钥
     */
    EncryptionKey getActiveKey();

    /**
     * 轮换密钥
     * <p>
     * 生成新的活跃密钥，并将当前活跃密钥标记为非活跃。
     * 非活跃密钥仍可用于解密旧数据。
     * <p>
     * 轮换过程：
     * 1. 生成新密钥并标记为活跃
     * 2. 将旧活跃密钥标记为非活跃
     * 3. 返回新密钥元数据
     *
     * @return 新的活跃密钥元数据
     */
    EncryptionKey rotateKey();

    /**
     * 停用密钥
     * <p>
     * 将指定密钥标记为非活跃，使其不再可用于加密。
     * 已停用的密钥仍可用于解密旧数据，直到过期清理。
     *
     * @param keyId 要停用的密钥标识
     */
    void deactivateKey(String keyId);
}
