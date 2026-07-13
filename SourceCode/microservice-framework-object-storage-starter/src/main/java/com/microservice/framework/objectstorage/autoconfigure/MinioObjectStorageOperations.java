package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.StorageObject;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MinIO Object Storage 操作实现。
 *
 * @author Andy Yang
 */
class MinioObjectStorageOperations implements ObjectStorageOperations {

    private final MinioClient minioClient;
    private final ObjectStorageProperties properties;

    MinioObjectStorageOperations(MinioClient minioClient, ObjectStorageProperties properties) {
        this.minioClient = Objects.requireNonNull(minioClient, "minioClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    private String defaultBucket() {
        return properties.getConnection().getBucket();
    }

    @Override
    public StorageObject upload(String bucket, String key, InputStream inputStream, String contentType) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        ObjectStorageSupport.validateContentType(properties, contentType);
        byte[] content = ObjectStorageSupport.readBounded(inputStream, properties.getUpload().getMaxFileSize());
        String resolvedContentType = ObjectStorageSupport.defaultContentType(contentType);
        try {
            var response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(resolvedKey)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .contentType(resolvedContentType)
                    .build());
            return new StorageObject(resolvedKey, resolvedBucket, content.length, resolvedContentType,
                    null, Collections.emptyMap(), response.etag());
        } catch (Exception ex) {
            throw translate("upload", ex);
        }
    }

    @Override
    public StorageObject upload(String key, InputStream inputStream, String contentType) {
        return upload(defaultBucket(), key, inputStream, contentType);
    }

    @Override
    public InputStream download(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(resolvedKey)
                    .build());
        } catch (Exception ex) {
            throw translate("download", ex);
        }
    }

    @Override
    public InputStream download(String key) {
        return download(defaultBucket(), key);
    }

    @Override
    public void delete(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(resolvedBucket)
                    .object(resolvedKey)
                    .build());
        } catch (Exception ex) {
            throw translate("delete", ex);
        }
    }

    @Override
    public void delete(String key) {
        delete(defaultBucket(), key);
    }

    @Override
    public boolean exists(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        try {
            statObject(resolvedBucket, resolvedKey);
            return true;
        } catch (Exception ex) {
            if (isObjectMissing(ex)) {
                return false;
            }
            throw translate("exists", ex);
        }
    }

    @Override
    public boolean exists(String key) {
        return exists(defaultBucket(), key);
    }

    @Override
    public List<StorageObject> list(String bucket, String prefix) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(resolvedBucket)
                    .prefix(prefix)
                    .recursive(true)
                    .includeUserMetadata(false)
                    .build());
            List<StorageObject> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    objects.add(new StorageObject(item.objectName(), resolvedBucket, item.size(),
                            null, item.lastModified() == null ? null : item.lastModified().toInstant(),
                            item.userMetadata() == null ? Collections.emptyMap() : item.userMetadata(),
                            item.etag()));
                }
            }
            return objects;
        } catch (Exception ex) {
            throw translate("list", ex);
        }
    }

    @Override
    public List<StorageObject> list(String prefix) {
        return list(defaultBucket(), prefix);
    }

    @Override
    public StorageObject copy(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        String resolvedSourceBucket = ObjectStorageSupport.requireText(sourceBucket, "sourceBucket");
        String resolvedSourceKey = ObjectStorageSupport.requireText(sourceKey, "sourceKey");
        String resolvedDestBucket = ObjectStorageSupport.requireText(destBucket, "destBucket");
        String resolvedDestKey = ObjectStorageSupport.requireText(destKey, "destKey");
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(resolvedDestBucket)
                    .object(resolvedDestKey)
                    .source(CopySource.builder()
                            .bucket(resolvedSourceBucket)
                            .object(resolvedSourceKey)
                            .build())
                    .build());
            return toStorageObject(resolvedDestBucket, resolvedDestKey, statObject(resolvedDestBucket, resolvedDestKey));
        } catch (Exception ex) {
            throw translate("copy", ex);
        }
    }

    @Override
    public StorageObject copy(String sourceKey, String destKey) {
        return copy(defaultBucket(), sourceKey, defaultBucket(), destKey);
    }

    @Override
    public StorageObject move(String sourceBucket, String sourceKey, String destBucket, String destKey) {
        StorageObject copied = copy(sourceBucket, sourceKey, destBucket, destKey);
        delete(sourceBucket, sourceKey);
        return copied;
    }

    @Override
    public StorageObject move(String sourceKey, String destKey) {
        return move(defaultBucket(), sourceKey, defaultBucket(), destKey);
    }

    @Override
    public Map<String, String> getMetadata(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        try {
            Map<String, String> metadata = statObject(resolvedBucket, resolvedKey).userMetadata();
            return metadata == null ? Collections.emptyMap() : metadata;
        } catch (Exception ex) {
            throw translate("metadata", ex);
        }
    }

    @Override
    public Map<String, String> getMetadata(String key) {
        return getMetadata(defaultBucket(), key);
    }

    @Override
    public long getSize(String bucket, String key) {
        String resolvedBucket = ObjectStorageSupport.requireText(bucket, "bucket");
        String resolvedKey = ObjectStorageSupport.requireText(key, "key");
        try {
            return statObject(resolvedBucket, resolvedKey).size();
        } catch (Exception ex) {
            throw translate("size", ex);
        }
    }

    @Override
    public long getSize(String key) {
        return getSize(defaultBucket(), key);
    }

    @Override
    public String getImplementationName() {
        return "minio";
    }

    private StatObjectResponse statObject(String bucket, String key) throws Exception {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucket)
                .object(key)
                .build());
    }

    private StorageObject toStorageObject(String bucket, String key, StatObjectResponse response) {
        return new StorageObject(key, bucket, response.size(), response.contentType(),
                response.lastModified() == null ? null : response.lastModified().toInstant(),
                response.userMetadata() == null ? Collections.emptyMap() : response.userMetadata(),
                response.etag());
    }

    private boolean isObjectMissing(Exception ex) {
        if (ex instanceof ErrorResponseException errorResponseException
                && errorResponseException.errorResponse() != null) {
            String code = errorResponseException.errorResponse().code();
            return "NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NotFound".equals(code);
        }
        return false;
    }

    private ObjectStorageException translate(String operation, Exception ex) {
        if (ex instanceof ObjectStorageException objectStorageException) {
            return objectStorageException;
        }
        if (isObjectMissing(ex)) {
            return new ObjectStorageException(ObjectStorageException.OS_OBJECT_NOT_FOUND,
                    ObjectStorageSupport.safeMessage("MinIO", operation), ex);
        }
        if (ex instanceof ErrorResponseException errorResponseException
                && errorResponseException.errorResponse() != null
                && "NoSuchBucket".equals(errorResponseException.errorResponse().code())) {
            return new ObjectStorageException(ObjectStorageException.OS_BUCKET_NOT_FOUND,
                    ObjectStorageSupport.safeMessage("MinIO", operation), ex);
        }
        return new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                ObjectStorageSupport.safeMessage("MinIO", operation), ex);
    }
}
