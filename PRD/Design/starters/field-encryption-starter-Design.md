# Field Encryption Starter 技术与测试设计

Author: Andy Yang

对应需求：[Field Encryption PRD](../../PRD/starters/field-encryption-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、Java Cryptography Architecture；默认 AES-GCM，KMS 通过 `KeyProvider` SPI 接入。不得引入自定义密码算法。

## 2. 技术设计

- 自动配置：Crypto、KeyProvider、Rotation、DatabaseAdapter optional。
- 公共 API：`FieldEncryptor`、`CipherText`、`KeyProvider`、`KeyDescriptor`、`EncryptionPolicy`、`ReEncryptionService`。
- 密文信封包含格式版本、算法、Key ID/版本、Nonce 和认证密文，使用稳定文本编码。
- 默认随机 AES-GCM；确定性检索采用独立受审策略和 Key，不与随机加密混用。
- KeyProvider 仅返回受控密钥句柄/材料；缓存限时且可清除；日志只记录 Key ID。
- 轮换时新写使用当前 Key，读取按密文 Key 版本解析，重加密为可恢复批任务。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 加密契约 | 往返、随机性、AAD、类型、格式版本 |
| 安全 | 密文篡改、错误 Key、Nonce、密钥/明文泄漏扫描 |
| 自动配置 | 默认 Provider、KMS 替身、缺失 Key、用户覆盖 |
| 轮换 | 新旧 Key 并存、吊销、批量重加密中断与恢复 |
| 并发 | Key 缓存、并发轮换和大量加解密 |
| 性能 | 不同载荷加解密与 KeyProvider 缓存基准 |

通过条件：损坏或未知密文失败关闭；默认相同明文生成不同密文；密钥不泄漏。
