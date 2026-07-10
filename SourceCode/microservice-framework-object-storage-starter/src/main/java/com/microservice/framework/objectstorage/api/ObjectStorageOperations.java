package com.microservice.framework.objectstorage.api;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Object Storage 核心操作接口
 * <p>
 * 提供对象存储的基础 CRUD 操作，包括上传、下载、删除、查询、
 * 复制和移动。所有 S3-compatible 存储服务（AWS S3、MinIO 等）
 * 的实现均基于此接口。
 * <p>
 * 实现类必须：
 * <ul>
 *   <li>所有方法参数非 null，遇到 null 参数抛出 {@link ObjectStorageException}</li>
 *   <li>返回值非 null（{@link #list} 返回空列表而非 null）</li>
 *   <li>异常统一使用 {@link ObjectStorageException} 包装底层 SDK 错误</li>
 * </ul>
 *
 * @author Andy Yang
 */
public interface ObjectStorageOperations {

    /**
     * 上传对象到指定存储桶
     *
     * @param bucket      存储桶名称，非 null
     * @param key         对象键，非 null
     * @param inputStream 对象内容流，非 null
     * @param contentType 内容类型（如 "application/pdf"），可为 null 则使用默认类型
     * @return 上传后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或上传失败
     */
    StorageObject upload(String bucket, String key, InputStream inputStream, String contentType);

    /**
     * 上传对象到默认存储桶
     *
     * @param key         对象键，非 null
     * @param inputStream 对象内容流，非 null
     * @param contentType 内容类型，可为 null
     * @return 上传后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或上传失败
     */
    StorageObject upload(String key, InputStream inputStream, String contentType);

    /**
     * 下载指定对象
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return 对象内容流
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    InputStream download(String bucket, String key);

    /**
     * 下载指定对象（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return 对象内容流
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    InputStream download(String key);

    /**
     * 删除指定对象
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @throws ObjectStorageException 参数为 null 或删除失败
     */
    void delete(String bucket, String key);

    /**
     * 删除指定对象（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @throws ObjectStorageException 参数为 null 或删除失败
     */
    void delete(String key);

    /**
     * 判断指定对象是否存在
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return true 表示对象存在，false 表示不存在
     * @throws ObjectStorageException 参数为 null
     */
    boolean exists(String bucket, String key);

    /**
     * 判断指定对象是否存在（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return true 表示对象存在，false 表示不存在
     * @throws ObjectStorageException 参数为 null
     */
    boolean exists(String key);

    /**
     * 列出指定存储桶中的对象
     *
     * @param bucket 存储桶名称，非 null
     * @param prefix 对象键前缀过滤，可为 null 则列出全部
     * @return 对象列表，非 null（可能为空列表）
     * @throws ObjectStorageException 参数为 null 或列举失败
     */
    List<StorageObject> list(String bucket, String prefix);

    /**
     * 列出默认存储桶中的对象
     *
     * @param prefix 对象键前缀过滤，可为 null 则列出全部
     * @return 对象列表，非 null（可能为空列表）
     * @throws ObjectStorageException 列举失败
     */
    List<StorageObject> list(String prefix);

    /**
     * 复制对象到新位置
     *
     * @param sourceBucket 源存储桶，非 null
     * @param sourceKey    源对象键，非 null
     * @param destBucket   目标存储桶，非 null
     * @param destKey      目标对象键，非 null
     * @return 复制后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或复制失败
     */
    StorageObject copy(String sourceBucket, String sourceKey, String destBucket, String destKey);

    /**
     * 复制对象到新位置（源和目标使用默认存储桶）
     *
     * @param sourceKey 源对象键，非 null
     * @param destKey   目标对象键，非 null
     * @return 复制后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或复制失败
     */
    StorageObject copy(String sourceKey, String destKey);

    /**
     * 移动对象到新位置（复制 + 删除源对象）
     *
     * @param sourceBucket 源存储桶，非 null
     * @param sourceKey    源对象键，非 null
     * @param destBucket   目标存储桶，非 null
     * @param destKey      目标对象键，非 null
     * @return 移动后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或移动失败
     */
    StorageObject move(String sourceBucket, String sourceKey, String destBucket, String destKey);

    /**
     * 移动对象到新位置（源和目标使用默认存储桶）
     *
     * @param sourceKey 源对象键，非 null
     * @param destKey   目标对象键，非 null
     * @return 移动后的存储对象元数据
     * @throws ObjectStorageException 参数为 null 或移动失败
     */
    StorageObject move(String sourceKey, String destKey);

    /**
     * 获取对象的元数据
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return 对象元数据，非 null
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    Map<String, String> getMetadata(String bucket, String key);

    /**
     * 获取对象的元数据（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return 对象元数据，非 null
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    Map<String, String> getMetadata(String key);

    /**
     * 获取对象的大小（字节数）
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return 对象大小（字节）
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    long getSize(String bucket, String key);

    /**
     * 获取对象的大小（字节数，使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return 对象大小（字节）
     * @throws ObjectStorageException 参数为 null 或对象不存在
     */
    long getSize(String key);

    /**
     * 返回实现标识名称（如 "s3"、"minio"）
     *
     * @return 实现名称，非 null
     */
    String getImplementationName();
}
