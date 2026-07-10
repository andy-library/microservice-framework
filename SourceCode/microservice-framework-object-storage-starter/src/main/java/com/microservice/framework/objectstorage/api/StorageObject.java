package com.microservice.framework.objectstorage.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 存储对象元数据（不可变）
 * <p>
 * 表示对象存储中的一个对象，包含键、存储桶、大小、内容类型、
 * 最后修改时间、自定义元数据和 ETag。
 * <p>
 * 此类为不可变对象，创建后所有字段不可修改。
 *
 * @author Andy Yang
 */
public final class StorageObject {

    private final String key;
    private final String bucket;
    private final long size;
    private final String contentType;
    private final Instant lastModified;
    private final Map<String, String> metadata;
    private final String eTag;

    /**
     * 创建存储对象元数据
     *
     * @param key          对象键，非 null
     * @param bucket       存储桶名称，非 null
     * @param size         对象大小（字节）
     * @param contentType  内容类型，可为 null
     * @param lastModified 最后修改时间，可为 null
     * @param metadata     自定义元数据，可为 null 则使用空 Map
     * @param eTag         对象 ETag，可为 null
     * @throws NullPointerException key 或 bucket 为 null
     */
    public StorageObject(String key, String bucket, long size, String contentType,
                         Instant lastModified, Map<String, String> metadata, String eTag) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.bucket = Objects.requireNonNull(bucket, "bucket must not be null");
        this.size = size;
        this.contentType = contentType;
        this.lastModified = lastModified;
        this.metadata = metadata != null ? Collections.unmodifiableMap(new LinkedHashMap<>(metadata))
                : Collections.emptyMap();
        this.eTag = eTag;
    }

    /**
     * 返回对象键
     *
     * @return 对象键，非 null
     */
    public String getKey() {
        return key;
    }

    /**
     * 返回存储桶名称
     *
     * @return 存储桶名称，非 null
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 返回对象大小（字节）
     *
     * @return 对象大小
     */
    public long getSize() {
        return size;
    }

    /**
     * 返回内容类型（如 "application/pdf"）
     *
     * @return 内容类型，可能为 null
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 返回最后修改时间
     *
     * @return 最后修改时间，可能为 null
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * 返回自定义元数据（不可变 Map）
     *
     * @return 不可变元数据 Map，非 null
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 返回对象 ETag
     *
     * @return ETag，可能为 null
     */
    public String getETag() {
        return eTag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StorageObject)) {
            return false;
        }
        StorageObject other = (StorageObject) obj;
        return key.equals(other.key)
                && bucket.equals(other.bucket)
                && size == other.size
                && Objects.equals(contentType, other.contentType)
                && Objects.equals(lastModified, other.lastModified)
                && metadata.equals(other.metadata)
                && Objects.equals(eTag, other.eTag);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + bucket.hashCode();
        result = 31 * result + Long.hashCode(size);
        result = 31 * result + Objects.hashCode(contentType);
        result = 31 * result + Objects.hashCode(lastModified);
        result = 31 * result + metadata.hashCode();
        result = 31 * result + Objects.hashCode(eTag);
        return result;
    }

    @Override
    public String toString() {
        return "StorageObject{" +
                "key='" + key + '\'' +
                ", bucket='" + bucket + '\'' +
                ", size=" + size +
                ", contentType='" + contentType + '\'' +
                ", lastModified=" + lastModified +
                ", metadata=" + metadata +
                ", eTag='" + eTag + '\'' +
                '}';
    }
}
