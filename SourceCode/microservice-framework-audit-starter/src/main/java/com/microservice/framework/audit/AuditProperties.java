package com.microservice.framework.audit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Audit Starter 配置属性
 * <p>
 * 聚合存储、留存、安全和 B 端配置组，
 * 所有属性前缀为 {@code framework.audit}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.audit")
public class AuditProperties {

    /**
     * 存储配置
     */
    @NestedConfigurationProperty
    private StorageProperties storage = new StorageProperties();

    /**
     * 留存配置
     */
    @NestedConfigurationProperty
    private RetentionProperties retention = new RetentionProperties();

    /**
     * 安全配置
     */
    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    /**
     * B 端配置
     */
    @NestedConfigurationProperty
    private BSideProperties bside = new BSideProperties();

    // Getters and Setters

    public StorageProperties getStorage() {
        return storage;
    }

    public void setStorage(StorageProperties storage) {
        this.storage = storage;
    }

    public RetentionProperties getRetention() {
        return retention;
    }

    public void setRetention(RetentionProperties retention) {
        this.retention = retention;
    }

    public SecurityProperties getSecurity() {
        return security;
    }

    public void setSecurity(SecurityProperties security) {
        this.security = security;
    }

    public BSideProperties getBside() {
        return bside;
    }

    public void setBside(BSideProperties bside) {
        this.bside = bside;
    }

    /**
     * 存储配置
     */
    public static class StorageProperties {

        /**
         * 存储类型，默认 MEMORY
         * <p>
         * 支持的存储类型：
         * - MEMORY：内存存储（仅用于开发和测试）
         * - JDBC：数据库存储（生产环境推荐）
         * - DATABASE：JDBC 的兼容别名
         */
        @NotBlank
        private String type = "MEMORY";

        /**
         * 是否异步写入，默认 true
         * <p>
         * 启用后审计记录通过异步方式写入存储，
         * 不阻塞业务线程，但可能存在短暂延迟。
         */
        private Boolean async = true;

        /**
         * 是否自动创建 JDBC 审计表。生产环境禁止启用，应交由迁移工具管理。
         */
        private Boolean autoCreateTable = false;

        /**
         * JDBC 审计表名，仅允许字母、数字和下划线。
         */
        @NotBlank
        private String tableName = "framework_audit_event";

        // Getters and Setters

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getAsync() {
            return async;
        }

        public void setAsync(Boolean async) {
            this.async = async;
        }

        public Boolean getAutoCreateTable() {
            return autoCreateTable;
        }

        public void setAutoCreateTable(Boolean autoCreateTable) {
            this.autoCreateTable = autoCreateTable;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }

    /**
     * 留存配置
     */
    public static class RetentionProperties {

        /**
         * 数据留存天数，默认 365
         * <p>
         * 超过此天数的审计记录将被自动清理，
         * 满足合规要求的同时避免存储膨胀。
         */
        @Min(1)
        private Integer days = 365;

        // Getters and Setters

        public Integer getDays() {
            return days;
        }

        public void setDays(Integer days) {
            this.days = days;
        }
    }

    /**
     * 安全配置
     */
    public static class SecurityProperties {

        /**
         * 是否启用防篡改 checksum，默认 true
         * <p>
         * 启用后每条审计记录将计算 checksum，
         * 读取时自动验证完整性，确保审计数据不被篡改。
         */
        private Boolean checksumEnabled = true;

        /**
         * checksum 计算算法，默认 SHA-256
         * <p>
         * 支持的算法取决于 JDK 提供的 MessageDigest 实现。
         */
        @NotBlank
        private String checksumAlgorithm = "SHA-256";

        // Getters and Setters

        public Boolean getChecksumEnabled() {
            return checksumEnabled;
        }

        public void setChecksumEnabled(Boolean checksumEnabled) {
            this.checksumEnabled = checksumEnabled;
        }

        public String getChecksumAlgorithm() {
            return checksumAlgorithm;
        }

        public void setChecksumAlgorithm(String checksumAlgorithm) {
            this.checksumAlgorithm = checksumAlgorithm;
        }
    }

    /**
     * B 端配置
     * <p>
     * B 端（企业内部管理侧）强制审计场景的配置。
     */
    public static class BSideProperties {

        /**
         * B 端是否强制审计，默认 true
         * <p>
         * 启用后所有 B 端操作将被强制记录审计事件，
         * 不可跳过或禁用，确保合规要求。
         */
        private Boolean mandatory = true;

        // Getters and Setters

        public Boolean getMandatory() {
            return mandatory;
        }

        public void setMandatory(Boolean mandatory) {
            this.mandatory = mandatory;
        }
    }
}
