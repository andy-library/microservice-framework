package com.microservice.framework.common;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Common Starter 配置属性
 * <p>
 * 聚合日期时间、分布式 ID、轻量上下文等配置组，
 * 所有属性前缀为 {@code framework.common}。
 *
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.common")
public class CommonProperties {

    /**
     * 日期时间配置
     */
    @NestedConfigurationProperty
    private TimeProperties time = new TimeProperties();

    /**
     * 分布式 ID 配置
     */
    @NestedConfigurationProperty
    private IdProperties id = new IdProperties();

    /**
     * 轻量上下文配置
     */
    @NestedConfigurationProperty
    private ContextProperties context = new ContextProperties();

    // Getters and Setters

    public TimeProperties getTime() {
        return time;
    }

    public void setTime(TimeProperties time) {
        this.time = time;
    }

    public IdProperties getId() {
        return id;
    }

    public void setId(IdProperties id) {
        this.id = id;
    }

    public ContextProperties getContext() {
        return context;
    }

    public void setContext(ContextProperties context) {
        this.context = context;
    }

    /**
     * 日期时间配置
     */
    public static class TimeProperties {

        /**
         * 配置时区，默认 UTC
         * <p>
         * 所有日期时间操作必须使用此时区，禁止依赖操作系统默认时区。
         */
        private String timeZone = "UTC";

        // Getters and Setters

        public String getTimeZone() {
            return timeZone;
        }

        public void setTimeZone(String timeZone) {
            this.timeZone = timeZone;
        }
    }

    /**
     * 分布式 ID 配置
     */
    public static class IdProperties {

        /**
         * ID 生成算法类型，默认 snowflake
         */
        @NotNull
        private String type = "snowflake";

        /**
         * 工作节点 ID（0-1023）
         * <p>
         * 必须显式配置，以确保集群内各节点唯一。
         */
        private Long workerId;

        /**
         * 时钟回拨容忍阈值（毫秒），默认 5000
         * <p>
         * 当时钟回拨幅度不超过此阈值时，等待时间追平后继续生成；
         * 超过此阈值时直接抛出异常，防止生成重复 ID。
         */
        @Min(0)
        private Long clockBackwardTolerance = 5000L;

        // Getters and Setters

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getWorkerId() {
            return workerId;
        }

        public void setWorkerId(Long workerId) {
            this.workerId = workerId;
        }

        public Long getClockBackwardTolerance() {
            return clockBackwardTolerance;
        }

        public void setClockBackwardTolerance(Long clockBackwardTolerance) {
            this.clockBackwardTolerance = clockBackwardTolerance;
        }
    }

    /**
     * 轻量上下文配置
     */
    public static class ContextProperties {

        /**
         * 是否启用上下文传播，默认 true
         */
        private Boolean enabled = true;

        // Getters and Setters

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
