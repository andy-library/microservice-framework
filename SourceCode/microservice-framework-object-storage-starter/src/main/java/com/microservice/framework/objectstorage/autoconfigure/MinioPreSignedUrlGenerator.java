package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;

import java.time.Duration;
import java.time.Instant;

/**
 * MinIO 预签名 URL 生成器的占位实现
 * <p>
 * 当 MinIO SDK 存在于类路径时，此实现提供基于 MinIO 的预签名 URL 生成。
 * <p>
 * 注意：此实现为框架内置占位实现，实际 SDK 调用需在运行时通过 MinioClient 完成。
 * 由于 MinIO SDK 为可选依赖，方法体内抛出 {@link ObjectStorageException#OS_INTERNAL_ERROR}
 * 以标识此类需要配合 MinIO SDK 使用。
 *
 * @author Andy Yang
 */
class MinioPreSignedUrlGenerator implements PreSignedUrlGenerator {

    private final ObjectStorageProperties properties;

    MinioPreSignedUrlGenerator(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    private String defaultBucket() {
        return properties.getConnection().getBucket();
    }

    @Override
    public String generateUploadUrl(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "MinioPreSignedUrlGenerator requires MinIO SDK runtime");
    }

    @Override
    public String generateUploadUrl(String key) {
        return generateUploadUrl(defaultBucket(), key);
    }

    @Override
    public String generateDownloadUrl(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "MinioPreSignedUrlGenerator requires MinIO SDK runtime");
    }

    @Override
    public String generateDownloadUrl(String key) {
        return generateDownloadUrl(defaultBucket(), key);
    }

    @Override
    public String generateUrl(String bucket, String key, Duration expiry) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "MinioPreSignedUrlGenerator requires MinIO SDK runtime");
    }

    @Override
    public String generateUrl(String key, Duration expiry) {
        return generateUrl(defaultBucket(), key, expiry);
    }

    @Override
    public String generateUrl(String bucket, String key, Instant expiration) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "MinioPreSignedUrlGenerator requires MinIO SDK runtime");
    }

    @Override
    public String getImplementationName() {
        return "minio";
    }
}
