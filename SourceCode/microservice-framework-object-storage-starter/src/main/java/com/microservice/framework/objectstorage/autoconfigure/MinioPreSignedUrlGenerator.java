package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * MinIO 预签名 URL 生成器实现。
 *
 * @author Andy Yang
 */
class MinioPreSignedUrlGenerator implements PreSignedUrlGenerator {

    private final MinioClient minioClient;
    private final ObjectStorageProperties properties;

    MinioPreSignedUrlGenerator(MinioClient minioClient, ObjectStorageProperties properties) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    private String defaultBucket() {
        return properties.getConnection().getBucket();
    }

    @Override
    public String generateUploadUrl(String bucket, String key) {
        return generateUrl(bucket, key, Duration.ofSeconds(properties.getPresign().getDefaultExpiry()), Method.PUT);
    }

    @Override
    public String generateUploadUrl(String key) {
        return generateUploadUrl(defaultBucket(), key);
    }

    @Override
    public String generateDownloadUrl(String bucket, String key) {
        return generateUrl(bucket, key, Duration.ofSeconds(properties.getPresign().getDefaultExpiry()), Method.GET);
    }

    @Override
    public String generateDownloadUrl(String key) {
        return generateDownloadUrl(defaultBucket(), key);
    }

    @Override
    public String generateUrl(String bucket, String key, Duration expiry) {
        return generateUrl(bucket, key, expiry, Method.GET);
    }

    @Override
    public String generateUrl(String key, Duration expiry) {
        return generateUrl(defaultBucket(), key, expiry);
    }

    @Override
    public String generateUrl(String bucket, String key, Instant expiration) {
        return generateUrl(bucket, key, ObjectStorageSupport.expiryUntil(expiration));
    }

    @Override
    public String getImplementationName() {
        return "minio";
    }

    private String generateUrl(String bucket, String key, Duration expiry, Method method) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        Duration resolvedExpiry = ObjectStorageSupport.requirePositiveExpiry(expiry);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(resolvedBucket)
                    .object(resolvedKey)
                    .method(method)
                    .expiry(Math.toIntExact(resolvedExpiry.toSeconds()))
                    .build());
        } catch (Exception ex) {
            throw translate("presign", ex);
        }
    }

    private ObjectStorageException translate(String operation, Exception ex) {
        if (ex instanceof ObjectStorageException objectStorageException) {
            return objectStorageException;
        }
        return new ObjectStorageException(ObjectStorageException.OS_PRESIGN_FAILED,
                ObjectStorageSupport.safeMessage("MinIO", operation), ex);
    }
}
