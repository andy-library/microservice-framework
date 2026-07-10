package com.microservice.framework.objectstorage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Object Storage Starter 配置属性
 * <p>
 * 所有属性前缀为 {@code framework.object-storage}，控制 S3-compatible
 * 对象存储的连接、上传限制、预签名和文件治理行为。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.object-storage")
public class ObjectStorageProperties {

    /**
     * 是否启用对象存储 Starter，默认 true
     */
    private boolean enabled = true;

    /**
     * 连接配置
     */
    @NestedConfigurationProperty
    private ConnectionProperties connection = new ConnectionProperties();

    /**
     * 上传配置
     */
    @NestedConfigurationProperty
    private UploadProperties upload = new UploadProperties();

    /**
     * 预签名 URL 配置
     */
    @NestedConfigurationProperty
    private PresignProperties presign = new PresignProperties();

    /**
     * 文件治理配置
     */
    @NestedConfigurationProperty
    private GovernanceProperties governance = new GovernanceProperties();

    // Getters and Setters

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ConnectionProperties getConnection() {
        return connection;
    }

    public void setConnection(ConnectionProperties connection) {
        this.connection = connection;
    }

    public UploadProperties getUpload() {
        return upload;
    }

    public void setUpload(UploadProperties upload) {
        this.upload = upload;
    }

    public PresignProperties getPresign() {
        return presign;
    }

    public void setPresign(PresignProperties presign) {
        this.presign = presign;
    }

    public GovernanceProperties getGovernance() {
        return governance;
    }

    public void setGovernance(GovernanceProperties governance) {
        this.governance = governance;
    }

    // ======================================================================
    // 连接配置
    // ======================================================================

    /**
     * 连接配置
     * <p>
     * 定义 S3-compatible 对象存储服务的连接参数。
     */
    public static class ConnectionProperties {

        /**
         * 对象存储服务端点（如 "https://s3.amazonaws.com" 或 "http://localhost:9000"）
         */
        @NotBlank
        private String endpoint = "https://s3.amazonaws.com";

        /**
         * 访问密钥（Access Key）
         */
        private String accessKey;

        /**
         * 密钥（Secret Key）
         */
        private String secretKey;

        /**
         * 区域（Region），默认 "us-east-1"
         */
        private String region = "us-east-1";

        /**
         * 默认存储桶名称
         */
        private String bucket;

        // Getters and Setters

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    // ======================================================================
    // 上传配置
    // ======================================================================

    /**
     * 上传配置
     * <p>
     * 控制文件上传的大小限制和类型限制。
     */
    public static class UploadProperties {

        /**
         * 单文件最大大小（字节），默认 50MB（52,428,800 字节）
         */
        @Min(1)
        private long maxFileSize = 52_428_800;

        /**
         * 允许的内容类型列表，为空表示不限制
         * <p>
         * 例如：["application/pdf", "image/png", "text/plain"]
         */
        private List<String> allowedTypes = new ArrayList<>();

        /**
         * 分块上传阈值（字节），默认 5MB（5,242,880 字节）
         * <p>
         * 文件大小超过此阈值时将自动使用分块上传。
         */
        @Min(1)
        private long multipartThreshold = 5_242_880;

        // Getters and Setters

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public List<String> getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(List<String> allowedTypes) {
            this.allowedTypes = allowedTypes;
        }

        public long getMultipartThreshold() {
            return multipartThreshold;
        }

        public void setMultipartThreshold(long multipartThreshold) {
            this.multipartThreshold = multipartThreshold;
        }
    }

    // ======================================================================
    // 预签名配置
    // ======================================================================

    /**
     * 预签名 URL 配置
     * <p>
     * 控制预签名 URL 的启用状态和默认有效期。
     */
    public static class PresignProperties {

        /**
         * 是否启用预签名 URL 功能，默认 true
         */
        private boolean enabled = true;

        /**
         * 预签名 URL 默认有效期（秒），默认 3600（1小时）
         */
        @Min(1)
        @Max(604800)
        private int defaultExpiry = 3600;

        // Getters and Setters

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultExpiry() {
            return defaultExpiry;
        }

        public void setDefaultExpiry(int defaultExpiry) {
            this.defaultExpiry = defaultExpiry;
        }
    }

    // ======================================================================
    // 治理配置
    // ======================================================================

    /**
     * 文件治理配置
     * <p>
     * 控制文件治理规则的启用状态，包括自动隔离等。
     */
    public static class GovernanceProperties {

        /**
         * 是否启用文件治理，默认 true
         */
        private boolean enabled = true;

        /**
         * 是否启用隔离功能，默认 false
         * <p>
         * 启用后，可疑文件将被自动隔离到指定存储桶，
         * 禁止正常访问直到审核完成。
         */
        private boolean quarantineEnabled = false;

        // Getters and Setters

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean getQuarantineEnabled() {
            return quarantineEnabled;
        }

        public void setQuarantineEnabled(boolean quarantineEnabled) {
            this.quarantineEnabled = quarantineEnabled;
        }
    }
}
