package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * AWS S3 预签名 URL 生成器实现。
 *
 * @author Andy Yang
 */
class S3PreSignedUrlGenerator implements PreSignedUrlGenerator {

    private final S3Presigner presigner;
    private final ObjectStorageProperties properties;

    S3PreSignedUrlGenerator(S3Presigner presigner, ObjectStorageProperties properties) {
        this.presigner = Objects.requireNonNull(presigner, "presigner must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    private String defaultBucket() {
        return properties.getConnection().getBucket();
    }

    @Override
    public String generateUploadUrl(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        Duration expiry = Duration.ofSeconds(properties.getPresign().getDefaultExpiry());
        try {
            return presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .putObjectRequest(PutObjectRequest.builder()
                            .bucket(resolvedBucket)
                            .key(resolvedKey)
                            .build())
                    .build()).url().toString();
        } catch (RuntimeException ex) {
            throw translate("presign upload", ex);
        }
    }

    @Override
    public String generateUploadUrl(String key) {
        return generateUploadUrl(defaultBucket(), key);
    }

    @Override
    public String generateDownloadUrl(String bucket, String key) {
        return generateUrl(bucket, key, Duration.ofSeconds(properties.getPresign().getDefaultExpiry()));
    }

    @Override
    public String generateDownloadUrl(String key) {
        return generateDownloadUrl(defaultBucket(), key);
    }

    @Override
    public String generateUrl(String bucket, String key, Duration expiry) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        Duration resolvedExpiry = ObjectStorageSupport.requirePositiveExpiry(expiry);
        try {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(resolvedExpiry)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(resolvedBucket)
                            .key(resolvedKey)
                            .build())
                    .build()).url().toString();
        } catch (RuntimeException ex) {
            throw translate("presign download", ex);
        }
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
        return "s3";
    }

    private ObjectStorageException translate(String operation, RuntimeException ex) {
        if (ex instanceof ObjectStorageException objectStorageException) {
            return objectStorageException;
        }
        if (ex instanceof SdkClientException) {
            return new ObjectStorageException(ObjectStorageException.OS_PRESIGN_FAILED,
                    ObjectStorageSupport.safeMessage("S3", operation), ex);
        }
        return new ObjectStorageException(ObjectStorageException.OS_PRESIGN_FAILED,
                ObjectStorageSupport.safeMessage("S3", operation), ex);
    }
}
