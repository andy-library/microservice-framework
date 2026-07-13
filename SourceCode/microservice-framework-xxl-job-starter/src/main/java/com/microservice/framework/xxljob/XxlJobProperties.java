package com.microservice.framework.xxljob;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * XXL-JOB Starter 配置属性
 * <p>
 * 聚合 Admin、Executor、幂等和超时配置组，
 * 所有属性前缀为 {@code framework.xxl-job}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.xxl-job")
@Validated
@GroupSequenceProvider(XxlJobProperties.ValidationGroupProvider.class)
public class XxlJobProperties {

    /**
     * 是否启用 XXL-JOB Starter，默认 true
     */
    private Boolean enabled = true;

    /**
     * Admin 配置
     */
    @NestedConfigurationProperty
    @Valid
    private AdminProperties admin = new AdminProperties();

    /**
     * Executor 配置
     */
    @NestedConfigurationProperty
    @Valid
    private ExecutorProperties executor = new ExecutorProperties();

    /**
     * 幂等配置
     */
    @NestedConfigurationProperty
    @Valid
    private IdempotencyProperties idempotency = new IdempotencyProperties();

    /**
     * 超时配置
     */
    @NestedConfigurationProperty
    @Valid
    private TimeoutProperties timeout = new TimeoutProperties();

    // Getters and Setters

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public AdminProperties getAdmin() {
        return admin;
    }

    public void setAdmin(AdminProperties admin) {
        this.admin = admin;
    }

    public ExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorProperties executor) {
        this.executor = executor;
    }

    public IdempotencyProperties getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(IdempotencyProperties idempotency) {
        this.idempotency = idempotency;
    }

    public TimeoutProperties getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutProperties timeout) {
        this.timeout = timeout;
    }

    /**
     * Admin 配置
     * <p>
     * XXL-JOB 调度中心相关配置。
     */
    public static class AdminProperties {

        /**
         * 调度中心地址列表（多个地址用逗号分隔）
         * <p>
         * 示例：http://127.0.0.1:8080/xxl-job-admin,http://127.0.0.1:8081/xxl-job-admin
         */
        @NotBlank(groups = EnabledConfiguration.class)
        private String addresses;

        /**
         * 执行器 AppName（调度中心注册时使用）
         */
        @NotBlank(groups = EnabledConfiguration.class)
        private String appName;

        /**
         * 访问令牌（调度中心安全认证）
         */
        private String accessToken;

        // Getters and Setters

        public String getAddresses() {
            return addresses;
        }

        public void setAddresses(String addresses) {
            this.addresses = addresses;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    /**
     * Executor 配置
     * <p>
     * XXL-JOB 执行器相关配置。
     */
    public static class ExecutorProperties {

        /**
         * 执行器 AppName（与 Admin 配置的 appName 对应）
         */
        @NotBlank(groups = EnabledConfiguration.class)
        private String appName;

        /**
         * 执行器注册地址（优先使用此地址，为空时自动获取）
         */
        private String address;

        /**
         * 执行器端口，默认 9999
         */
        @Min(value = 1, groups = EnabledConfiguration.class)
        private Integer port = 9999;

        /**
         * 执行器日志路径
         */
        @NotBlank(groups = EnabledConfiguration.class)
        private String logPath = "/data/applogs/xxl-job/jobhandler";

        /**
         * 执行器日志保留天数，默认 30
         */
        @Min(value = 1, groups = EnabledConfiguration.class)
        private Integer logRetentionDays = 30;

        // Getters and Setters

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getLogPath() {
            return logPath;
        }

        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }

        public Integer getLogRetentionDays() {
            return logRetentionDays;
        }

        public void setLogRetentionDays(Integer logRetentionDays) {
            this.logRetentionDays = logRetentionDays;
        }
    }

    /**
     * 幂等配置
     * <p>
     * 控制任务执行的幂等保护行为。
     */
    public static class IdempotencyProperties {

        /**
         * 是否启用幂等保护，默认 true
         * <p>
         * 启用后，同一任务重复触发时将自动跳过执行。
         */
        private Boolean enabled = true;

        /**
         * 幂等存储类型，默认 MEMORY
         * <p>
         * MEMORY：基于 ConcurrentHashMap 的内存存储，适用于单实例部署。
         * 生产环境建议覆盖为 Redis 或数据库实现。
         */
        private StoreType storeType = StoreType.MEMORY;

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public StoreType getStoreType() {
            return storeType;
        }

        public void setStoreType(StoreType storeType) {
            this.storeType = storeType;
        }
    }

    /**
     * 超时配置
     * <p>
     * 控制任务执行的超时行为和超时处理策略。
     */
    public static class TimeoutProperties {

        /**
         * 默认超时时间（秒），默认 300
         * <p>
         * 当任务执行超过此时间后，将触发超时处理。
         */
        @Min(value = 1, groups = EnabledConfiguration.class)
        private Integer defaultTimeout = 300;

        /**
         * 超时处理策略，默认 FAIL
         * <p>
         * FAIL：超时后返回失败结果
         * TIMEOUT：超时后返回超时结果
         */
        private TimeoutHandler timeoutHandler = TimeoutHandler.FAIL;

        // Getters and Setters

        public Integer getDefaultTimeout() {
            return defaultTimeout;
        }

        public void setDefaultTimeout(Integer defaultTimeout) {
            this.defaultTimeout = defaultTimeout;
        }

        public TimeoutHandler getTimeoutHandler() {
            return timeoutHandler;
        }

        public void setTimeoutHandler(TimeoutHandler timeoutHandler) {
            this.timeoutHandler = timeoutHandler;
        }
    }

    /**
     * 幂等存储类型枚举
     */
    public enum StoreType {
        /**
         * 内存存储（ConcurrentHashMap）
         */
        MEMORY
    }

    /**
     * 超时处理策略枚举
     */
    public enum TimeoutHandler {
        /**
         * 超时后标记为失败
         */
        FAIL,

        /**
         * 超时后标记为超时
         */
        TIMEOUT
    }

    interface EnabledConfiguration {
    }

    public static class ValidationGroupProvider implements DefaultGroupSequenceProvider<XxlJobProperties> {

        @Override
        public List<Class<?>> getValidationGroups(XxlJobProperties properties) {
            List<Class<?>> groups = new ArrayList<>();
            groups.add(XxlJobProperties.class);
            if (properties != null && Boolean.TRUE.equals(properties.getEnabled())) {
                groups.add(EnabledConfiguration.class);
            }
            return groups;
        }
    }
}
