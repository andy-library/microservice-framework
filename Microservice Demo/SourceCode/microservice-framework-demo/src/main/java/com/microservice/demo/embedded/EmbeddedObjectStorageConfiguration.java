package com.microservice.demo.embedded;

import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.ObjectStorageException;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import com.microservice.framework.objectstorage.api.StorageObject;
import com.microservice.framework.common.error.FrameworkErrorCode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedded Object Storage Configuration
 *
 * Provides in-memory ObjectStorageOperations and PreSignedUrlGenerator beans
 * that override the starter's default implementations via {@code @ConditionalOnMissingBean}.
 * Activates only when {@code framework.object-storage.provider=embedded} is set.
 *
 * <p>All implementations are fully functional (not stubs) and suitable for
 * integration testing and demo purposes without real S3/MinIO infrastructure.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.object-storage", name = "provider", havingValue = "embedded")
public class EmbeddedObjectStorageConfiguration {

    private static final FrameworkErrorCode OS_PARAM_NULL = ObjectStorageException.OS_PARAM_NULL;
    private static final FrameworkErrorCode OS_BUCKET_NOT_FOUND = ObjectStorageException.OS_BUCKET_NOT_FOUND;
    private static final FrameworkErrorCode OS_OBJECT_NOT_FOUND = ObjectStorageException.OS_OBJECT_NOT_FOUND;

    // ======================================================================
    // ObjectStorageOperations
    // ======================================================================

    @Bean
    public ObjectStorageOperations inMemoryObjectStorageOperations() {
        return new InMemoryObjectStorageOperations();
    }

    /**
     * ConcurrentHashMap-backed in-memory ObjectStorageOperations.
     * Stores objects as byte arrays with metadata tracking. Fully implements
     * all interface methods including upload, download, delete, exists, list,
     * copy, move, getMetadata, getSize, and getImplementationName.
     */
    static class InMemoryObjectStorageOperations implements ObjectStorageOperations {

        private final ConcurrentHashMap<String, StoredObject> store = new ConcurrentHashMap<>();
        private final String defaultBucket = "demo-bucket";

        static class StoredObject {
            final byte[] data;
            final String contentType;
            final Instant lastModified;
            final Map<String, String> metadata;
            final String eTag;

            StoredObject(byte[] data, String contentType, Map<String, String> metadata) {
                this.data = data;
                this.contentType = contentType;
                this.lastModified = Instant.now();
                this.metadata = metadata != null
                        ? Collections.unmodifiableMap(new HashMap<>(metadata))
                        : Collections.emptyMap();
                this.eTag = UUID.randomUUID().toString().substring(0, 8);
            }
        }

        private String storageKey(String bucket, String key) {
            return bucket + ":" + key;
        }

        private String storageKey(String key) {
            return defaultBucket + ":" + key;
        }

        private void validateParams(String bucket, String key) {
            if (key == null || key.isEmpty()) {
                throw new ObjectStorageException(OS_PARAM_NULL, "key must not be null or empty");
            }
        }

        @Override
        public StorageObject upload(String bucket, String key, InputStream inputStream,
                                    String contentType) {
            validateParams(bucket, key);
            try {
                byte[] data = inputStream.readAllBytes();
                Map<String, String> meta = new HashMap<>();
                meta.put("source", "embedded-upload");
                meta.put("originalContentType", contentType != null ? contentType : "application/octet-stream");
                StoredObject stored = new StoredObject(data, contentType, meta);
                String sk = storageKey(bucket != null ? bucket : defaultBucket, key);
                store.put(sk, stored);
                return new StorageObject(
                        key,
                        bucket != null ? bucket : defaultBucket,
                        data.length,
                        contentType,
                        stored.lastModified,
                        stored.metadata,
                        stored.eTag
                );
            } catch (Exception e) {
                throw new ObjectStorageException(ObjectStorageException.OS_INTERNAL_ERROR,
                        "Failed to read upload stream: " + e.getMessage(), e);
            }
        }

        @Override
        public StorageObject upload(String key, InputStream inputStream, String contentType) {
            return upload(defaultBucket, key, inputStream, contentType);
        }

        @Override
        public InputStream download(String bucket, String key) {
            validateParams(bucket, key);
            String sk = storageKey(bucket, key);
            StoredObject stored = store.get(sk);
            if (stored == null) {
                throw new ObjectStorageException(OS_OBJECT_NOT_FOUND,
                        "Object not found: " + key + " in bucket: " + bucket);
            }
            return new ByteArrayInputStream(stored.data);
        }

        @Override
        public InputStream download(String key) {
            return download(defaultBucket, key);
        }

        @Override
        public void delete(String bucket, String key) {
            validateParams(bucket, key);
            String sk = storageKey(bucket, key);
            if (store.remove(sk) == null) {
                throw new ObjectStorageException(OS_OBJECT_NOT_FOUND,
                        "Object not found: " + key + " in bucket: " + bucket);
            }
        }

        @Override
        public void delete(String key) {
            delete(defaultBucket, key);
        }

        @Override
        public boolean exists(String bucket, String key) {
            if (key == null || key.isEmpty()) {
                return false;
            }
            String sk = storageKey(bucket, key);
            return store.containsKey(sk);
        }

        @Override
        public boolean exists(String key) {
            return exists(defaultBucket, key);
        }

        @Override
        public List<StorageObject> list(String bucket, String prefix) {
            List<StorageObject> result = new ArrayList<>();
            String bucketPrefix = bucket + ":";
            for (Map.Entry<String, StoredObject> entry : store.entrySet()) {
                if (entry.getKey().startsWith(bucketPrefix)) {
                    String objKey = entry.getKey().substring(bucketPrefix.length());
                    if (prefix == null || objKey.startsWith(prefix)) {
                        StoredObject stored = entry.getValue();
                        result.add(new StorageObject(
                                objKey, bucket, stored.data.length, stored.contentType,
                                stored.lastModified, stored.metadata, stored.eTag
                        ));
                    }
                }
            }
            return result;
        }

        @Override
        public List<StorageObject> list(String prefix) {
            return list(defaultBucket, prefix);
        }

        @Override
        public StorageObject copy(String sourceBucket, String sourceKey,
                                  String destBucket, String destKey) {
            validateParams(sourceBucket, sourceKey);
            validateParams(destBucket, destKey);
            String srcKey = storageKey(sourceBucket, sourceKey);
            StoredObject source = store.get(srcKey);
            if (source == null) {
                throw new ObjectStorageException(OS_OBJECT_NOT_FOUND,
                        "Source object not found: " + sourceKey);
            }
            StoredObject copy = new StoredObject(
                    source.data.clone(), source.contentType, new HashMap<>(source.metadata));
            String dstKey = storageKey(destBucket, destKey);
            store.put(dstKey, copy);
            return new StorageObject(
                    destKey, destBucket, copy.data.length, copy.contentType,
                    copy.lastModified, copy.metadata, copy.eTag
            );
        }

        @Override
        public StorageObject copy(String sourceKey, String destKey) {
            return copy(defaultBucket, sourceKey, defaultBucket, destKey);
        }

        @Override
        public StorageObject move(String sourceBucket, String sourceKey,
                                  String destBucket, String destKey) {
            StorageObject copied = copy(sourceBucket, sourceKey, destBucket, destKey);
            delete(sourceBucket, sourceKey);
            return copied;
        }

        @Override
        public StorageObject move(String sourceKey, String destKey) {
            return move(defaultBucket, sourceKey, defaultBucket, destKey);
        }

        @Override
        public Map<String, String> getMetadata(String bucket, String key) {
            validateParams(bucket, key);
            String sk = storageKey(bucket, key);
            StoredObject stored = store.get(sk);
            if (stored == null) {
                throw new ObjectStorageException(OS_OBJECT_NOT_FOUND,
                        "Object not found: " + key);
            }
            return stored.metadata;
        }

        @Override
        public Map<String, String> getMetadata(String key) {
            return getMetadata(defaultBucket, key);
        }

        @Override
        public long getSize(String bucket, String key) {
            validateParams(bucket, key);
            String sk = storageKey(bucket, key);
            StoredObject stored = store.get(sk);
            if (stored == null) {
                throw new ObjectStorageException(OS_OBJECT_NOT_FOUND,
                        "Object not found: " + key);
            }
            return stored.data.length;
        }

        @Override
        public long getSize(String key) {
            return getSize(defaultBucket, key);
        }

        @Override
        public String getImplementationName() {
            return "InMemoryObjectStorage";
        }
    }

    // ======================================================================
    // PreSignedUrlGenerator
    // ======================================================================

    @Bean
    public PreSignedUrlGenerator inMemoryPreSignedUrlGenerator() {
        return new InMemoryPreSignedUrlGenerator();
    }

    /**
     * In-memory PreSignedUrlGenerator that returns fake but valid-looking presigned URLs.
     * URLs contain the bucket, key, and expiry information in the path for test verification.
     */
    static class InMemoryPreSignedUrlGenerator implements PreSignedUrlGenerator {

        private final String defaultBucket = "demo-bucket";
        private final String baseUrl = "https://embedded-storage.local";

        private String buildUrl(String bucket, String key, String operation, String expiryInfo) {
            String signature = UUID.randomUUID().toString().substring(0, 16);
            return String.format("%s/%s/%s?operation=%s&expiry=%s&signature=%s",
                    baseUrl, bucket, key, operation, expiryInfo, signature);
        }

        @Override
        public String generateUploadUrl(String bucket, String key) {
            return buildUrl(bucket, key, "upload", "1h");
        }

        @Override
        public String generateUploadUrl(String key) {
            return generateUploadUrl(defaultBucket, key);
        }

        @Override
        public String generateDownloadUrl(String bucket, String key) {
            return buildUrl(bucket, key, "download", "1h");
        }

        @Override
        public String generateDownloadUrl(String key) {
            return generateDownloadUrl(defaultBucket, key);
        }

        @Override
        public String generateUrl(String bucket, String key, Duration expiry) {
            String expiryInfo = expiry != null ? expiry.getSeconds() + "s" : "default";
            return buildUrl(bucket, key, "presigned", expiryInfo);
        }

        @Override
        public String generateUrl(String key, Duration expiry) {
            return generateUrl(defaultBucket, key, expiry);
        }

        @Override
        public String generateUrl(String bucket, String key, Instant expiration) {
            String expiryInfo = expiration != null
                    ? expiration.getEpochSecond() + "epoch"
                    : "default";
            return buildUrl(bucket, key, "presigned", expiryInfo);
        }

        @Override
        public String getImplementationName() {
            return "InMemoryPreSignedUrlGenerator";
        }
    }
}
