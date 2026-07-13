package com.microservice.framework.objectstorage.autoconfigure;

import com.microservice.framework.objectstorage.ObjectStorageProperties;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.StorageObject;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AWS S3 Object Storage 操作实现。
 *
 * @author Andy Yang
 */
class S3ObjectStorageOperations implements ObjectStorageOperations {

    private final S3Client s3Client;
    private final ObjectStorageProperties properties;

    S3ObjectStorageOperations(S3Client s3Client, ObjectStorageProperties properties) {
        this.s3Client = Objects.requireNonNull(s3Client, "s3Client must not be null");
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
            var response = s3Client.putObject(PutObjectRequest.builder()
                            .bucket(resolvedBucket)
                            .key(resolvedKey)
                            .contentType(resolvedContentType)
                            .build(),
                    RequestBody.fromBytes(content));
            return new StorageObject(resolvedKey, resolvedBucket, content.length, resolvedContentType,
                    null, Collections.emptyMap(), response.eTag());
        } catch (RuntimeException ex) {
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
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(resolvedKey)
                    .build());
            return response;
        } catch (RuntimeException ex) {
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
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(resolvedKey)
                    .build());
        } catch (RuntimeException ex) {
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
            headObject(resolvedBucket, resolvedKey);
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (RuntimeException ex) {
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
            List<StorageObject> objects = new ArrayList<>();
            String continuationToken = null;
            do {
                ListObjectsV2Response response = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(resolvedBucket)
                        .prefix(prefix)
                        .continuationToken(continuationToken)
                        .build());
                for (S3Object object : response.contents()) {
                    objects.add(new StorageObject(object.key(), resolvedBucket, object.size() == null ? 0 : object.size(),
                            null, object.lastModified(), Collections.emptyMap(), object.eTag()));
                }
                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);
            return objects;
        } catch (RuntimeException ex) {
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
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(resolvedSourceBucket)
                    .sourceKey(resolvedSourceKey)
                    .destinationBucket(resolvedDestBucket)
                    .destinationKey(resolvedDestKey)
                    .build());
            return toStorageObject(resolvedDestBucket, resolvedDestKey, headObject(resolvedDestBucket, resolvedDestKey));
        } catch (RuntimeException ex) {
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
            return headObject(resolvedBucket, resolvedKey).metadata();
        } catch (RuntimeException ex) {
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
            Long contentLength = headObject(resolvedBucket, resolvedKey).contentLength();
            return contentLength == null ? 0 : contentLength;
        } catch (RuntimeException ex) {
            throw translate("size", ex);
        }
    }

    @Override
    public long getSize(String key) {
        return getSize(defaultBucket(), key);
    }

    @Override
    public String getImplementationName() {
        return "s3";
    }

    private HeadObjectResponse headObject(String bucket, String key) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    private StorageObject toStorageObject(String bucket, String key, HeadObjectResponse response) {
        Long contentLength = response.contentLength();
        return new StorageObject(key, bucket, contentLength == null ? 0 : contentLength,
                response.contentType(), response.lastModified(), response.metadata(), response.eTag());
    }

    private ObjectStorageException translate(String operation, RuntimeException ex) {
        if (ex instanceof ObjectStorageException objectStorageException) {
            return objectStorageException;
        }
        if (ex instanceof NoSuchKeyException) {
            return new ObjectStorageException(ObjectStorageException.OS_OBJECT_NOT_FOUND,
                    ObjectStorageSupport.safeMessage("S3", operation), ex);
        }
        if (ex instanceof NoSuchBucketException) {
            return new ObjectStorageException(ObjectStorageException.OS_BUCKET_NOT_FOUND,
                    ObjectStorageSupport.safeMessage("S3", operation), ex);
        }
        if (ex instanceof S3Exception || ex instanceof AwsServiceException || ex instanceof SdkClientException) {
            return new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                    ObjectStorageSupport.safeMessage("S3", operation), ex);
        }
        return new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                ObjectStorageSupport.safeMessage("S3", operation), ex);
    }
}
