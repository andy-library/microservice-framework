package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.StorageObject;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AWS S3 Object Storage 操作的占位实现
 * <p>
 * 当 AWS S3 SDK 存在于类路径时，此实现提供基于 S3 的对象存储操作。
 * <p>
 * 注意：此实现为框架内置占位实现，实际 SDK 调用需在运行时通过 S3Client 完成。
 * 由于 AWS S3 SDK 为可选依赖，此处仅声明构造函数和接口方法签名，
 * 方法体内抛出 {@link ObjectStorageException#OS_INTERNAL_ERROR} 以标识
 * 此类需要配合 S3 SDK 使用。
 *
 * @author Andy Yang
 */
class S3ObjectStorageOperations implements ObjectStorageOperations {

    private final ObjectStorageProperties properties;

    S3ObjectStorageOperations(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    private String defaultBucket() {
        return properties.getConnection().getBucket();
    }

    @Override
    public StorageObject upload(String bucket, String key, InputStream inputStream, String contentType) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public StorageObject upload(String key, InputStream inputStream, String contentType) {
        return upload(defaultBucket(), key, inputStream, contentType);
    }

    @Override
    public InputStream download(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public InputStream download(String key) {
        return download(defaultBucket(), key);
    }

    @Override
    public void delete(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public void delete(String key) {
        delete(defaultBucket(), key);
    }

    @Override
    public boolean exists(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public boolean exists(String key) {
        return exists(defaultBucket(), key);
    }

    @Override
    public List<StorageObject> list(String bucket, String prefix) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public List<StorageObject> list(String prefix) {
        return list(defaultBucket(), prefix);
    }

    @Override
    public StorageObject copy(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public StorageObject copy(String sourceKey, String destKey) {
        return copy(defaultBucket(), sourceKey, defaultBucket(), destKey);
    }

    @Override
    public StorageObject move(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public StorageObject move(String sourceKey, String destKey) {
        return move(defaultBucket(), sourceKey, defaultBucket(), destKey);
    }

    @Override
    public Map<String, String> getMetadata(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public Map<String, String> getMetadata(String key) {
        return getMetadata(defaultBucket(), key);
    }

    @Override
    public long getSize(String bucket, String key) {
        throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                "S3ObjectStorageOperations requires AWS S3 SDK runtime");
    }

    @Override
    public long getSize(String key) {
        return getSize(defaultBucket(), key);
    }

    @Override
    public String getImplementationName() {
        return "s3";
    }
}
