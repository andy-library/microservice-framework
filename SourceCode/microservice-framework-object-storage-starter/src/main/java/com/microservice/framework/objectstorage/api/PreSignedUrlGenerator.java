package com.microservice.framework.objectstorage.api;

import java.time.Duration;
import java.time.Instant;

/**
 * 预签名 URL 生成接口
 * <p>
 * 提供预签名（Pre-signed）URL 的生成能力，允许浏览器或客户端
 * 直接通过 HTTP 请求访问对象存储，无需经过应用服务器中转。
 * <p>
 * 实现类必须：
 * <ul>
 *   <li>所有方法参数非 null，遇到 null 参数抛出 {@link ObjectStorageException}</li>
 *   <li>返回值非 null</li>
 *   <li>异常统一使用 {@link ObjectStorageException} 包装底层 SDK 错误</li>
 * </ul>
 *
 * @author Andy Yang
 */
public interface PreSignedUrlGenerator {

    /**
     * 生成预签名上传 URL
     * <p>
     * 获取的 URL 允许客户端直接向对象存储上传文件，
     * 使用默认有效期。
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return 预签名上传 URL，非 null
     * @throws ObjectStorageException 参数为 null 或生成失败
     */
    String generateUploadUrl(String bucket, String key);

    /**
     * 生成预签名上传 URL（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return 预签名上传 URL，非 null
     * @throws ObjectStorageException 参数为 null 或生成失败
     */
    String generateUploadUrl(String key);

    /**
     * 生成预签名下载 URL
     * <p>
     * 获取的 URL 允许客户端直接从对象存储下载文件，
     * 使用默认有效期。
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @return 预签名下载 URL，非 null
     * @throws ObjectStorageException 参数为 null 或生成失败
     */
    String generateDownloadUrl(String bucket, String key);

    /**
     * 生成预签名下载 URL（使用默认存储桶）
     *
     * @param key 对象键，非 null
     * @return 预签名下载 URL，非 null
     * @throws ObjectStorageException 参数为 null 或生成失败
     */
    String generateDownloadUrl(String key);

    /**
     * 生成预签名 URL（指定有效期）
     * <p>
     * 可用于上传或下载，有效期由参数指定。
     *
     * @param bucket 存储桶名称，非 null
     * @param key    对象键，非 null
     * @param expiry 有效期时长，非 null，必须为正数
     * @return 预签名 URL，非 null
     * @throws ObjectStorageException 参数为 null 或有效期非法或生成失败
     */
    String generateUrl(String bucket, String key, Duration expiry);

    /**
     * 生成预签名 URL（使用默认存储桶，指定有效期）
     *
     * @param key    对象键，非 null
     * @param expiry 有效期时长，非 null，必须为正数
     * @return 预签名 URL，非 null
     * @throws ObjectStorageException 参数为 null 或有效期非法或生成失败
     */
    String generateUrl(String key, Duration expiry);

    /**
     * 生成预签名 URL（指定过期时间点）
     * <p>
     * 可用于上传或下载，过期时间由参数指定。
     *
     * @param bucket       存储桶名称，非 null
     * @param key          对象键，非 null
     * @param expiration   URL 过期的绝对时间点，非 null
     * @return 预签名 URL，非 null
     * @throws ObjectStorageException 参数为 null 或生成失败
     */
    String generateUrl(String bucket, String key, Instant expiration);

    /**
     * 返回实现标识名称（如 "s3"、"minio"）
     *
     * @return 实现名称，非 null
     */
    String getImplementationName();
}
