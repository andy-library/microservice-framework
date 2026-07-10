package com.microservice.framework.fieldencryption.api;

/**
 * 字段加密器接口
 * <p>
 * 提供字段级别的加密和解密能力。
 * 支持加密、解密和判断值是否已加密。
 * <p>
 * 应用可通过此接口：
 * - 对敏感字段值进行加密存储
 * - 从存储中解密字段值还原原始数据
 * - 判断字段值是否已加密（避免重复加密）
 *
 * @author Andy Yang
 */
public interface FieldEncryptor {

    /**
     * 加密字段值
     * <p>
     * 使用当前活跃密钥对字段值进行加密，
     * 加密后的值包含密钥标识前缀，便于解密时定位密钥。
     *
     * @param plaintext 原始明文值
     * @return 加密后的密文值
     */
    String encrypt(String plaintext);

    /**
     * 解密字段值
     * <p>
     * 根据密文中的密钥标识前缀定位对应密钥，
     * 使用该密钥解密还原原始明文值。
     *
     * @param ciphertext 加密后的密文值
     * @return 解密后的明文值
     */
    String decrypt(String ciphertext);

    /**
     * 判断字段值是否已加密
     * <p>
     * 通过密钥标识前缀判断值是否已经过加密处理，
     * 避免对已加密的值重复加密。
     *
     * @param value 待判断的值
     * @return 是否已加密
     */
    boolean isEncrypted(String value);
}
