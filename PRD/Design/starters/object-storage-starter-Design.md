# Object Storage Starter 技术与测试设计

Author: Andy Yang

对应需求：[Object Storage PRD](../../Requirements/starters/object-storage-starter-PRD.md)

## 1. 技术栈

Common、JSON、Logging、AWS SDK v2 S3 API；Security、Database、Observability optional。AWS SDK BOM 与 S3 模块必须由 Dependencies BOM 管理。

## 2. 技术设计

- 自动配置：S3Client、ObjectStorage、Presign、UploadGovernance、Lifecycle。
- 公共 API：`ObjectId`、`ObjectStorageClient`、`UploadRequest/Result`、`DownloadRequest`、`PresignedAccess`、`ContentValidator`。
- 对象标识包含 provider/bucket/key，不向业务持久化永久 URL。
- 默认私有 Bucket；预签名请求限制方法、对象、大小、内容类型和有效期。
- 分片上传状态可恢复；失败执行 abort；业务非原子场景使用临时对象、确认和孤儿清理协议。
- Provider 在应用设计时确定；V1 以 S3 兼容契约为唯一正式适配。

## 3. 测试设计

| 层级 | 关键用例 |
| --- | --- |
| 自动配置 | Provider、Bucket、凭据、预签名上限和用户覆盖 |
| 集成 | Testcontainers MinIO/S3 兼容环境；上传、下载、删除、分片 |
| 安全 | 私有访问、越权/过期预签名、伪造类型、凭据泄漏 |
| 可靠性 | 上传中断、abort、重复请求、业务提交失败、孤儿清理 |
| 契约 | S3 适配器与替身执行同一公共 API 契约 |
| 性能 | 大文件流式传输、分片大小、并发与内存占用 |

通过条件：默认私有；失败上传可清理；非原子流程可补偿；不生成永久公开 URL。
