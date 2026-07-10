package com.microservice.framework.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 日志配置属性
 * 
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.logging")
public class LoggingProperties {

    /**
     * 异步日志配置
     */
    @NestedConfigurationProperty
    private AsyncProperties async = new AsyncProperties();

    /**
     * 日志风暴防护配置
     */
    @NestedConfigurationProperty
    private FloodProtectionProperties floodProtection = new FloodProtectionProperties();

    /**
     * 数据脱敏配置
     */
    @NestedConfigurationProperty
    private MaskingProperties masking = new MaskingProperties();

    /**
     * Trace 关联采样配置
     */
    @NestedConfigurationProperty
    private TraceSamplingProperties traceSampling = new TraceSamplingProperties();

    // Getters and Setters

    public AsyncProperties getAsync() {
        return async;
    }

    public void setAsync(AsyncProperties async) {
        this.async = async;
    }

    public FloodProtectionProperties getFloodProtection() {
        return floodProtection;
    }

    public void setFloodProtection(FloodProtectionProperties floodProtection) {
        this.floodProtection = floodProtection;
    }

    public MaskingProperties getMasking() {
        return masking;
    }

    public void setMasking(MaskingProperties masking) {
        this.masking = masking;
    }

    public TraceSamplingProperties getTraceSampling() {
        return traceSampling;
    }

    public void setTraceSampling(TraceSamplingProperties traceSampling) {
        this.traceSampling = traceSampling;
    }

    /**
     * 异步日志配置
     */
    public static class AsyncProperties {
        /**
         * 是否启用异步日志
         */
        private boolean enabled = true;

        /**
         * 队列大小
         */
        private int queueSize = 256;

        /**
         * 丢弃阈值（0 表示不丢弃）
         */
        private int discardingThreshold = 0;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public int getDiscardingThreshold() {
            return discardingThreshold;
        }

        public void setDiscardingThreshold(int discardingThreshold) {
            this.discardingThreshold = discardingThreshold;
        }
    }

    /**
     * 日志风暴防护配置
     */
    public static class FloodProtectionProperties {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 每秒允许的日志数量
         */
        private int rate = 10;

        /**
         * 突发容量
         */
        private int burstCapacity = 100;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            this.rate = rate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    /**
     * 数据脱敏配置
     */
    public static class MaskingProperties {
        /**
         * 是否启用脱敏
         */
        private boolean enabled = false;

        /**
         * 启用的默认规则
         */
        private java.util.List<String> enabledDefaultRules = java.util.Arrays.asList("MOBILE_PHONE", "ID_CARD");

        /**
         * 自定义规则
         */
        private java.util.List<CustomMaskingRule> customRules = new java.util.ArrayList<>();

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public java.util.List<String> getEnabledDefaultRules() {
            return enabledDefaultRules;
        }

        public void setEnabledDefaultRules(java.util.List<String> enabledDefaultRules) {
            this.enabledDefaultRules = enabledDefaultRules;
        }

        public java.util.List<CustomMaskingRule> getCustomRules() {
            return customRules;
        }

        public void setCustomRules(java.util.List<CustomMaskingRule> customRules) {
            this.customRules = customRules;
        }
    }

    /**
     * 自定义脱敏规则
     */
    public static class CustomMaskingRule {
        /**
         * 规则名称
         */
        private String name;

        /**
         * 正则表达式
         */
        private String regex;

        /**
         * 脱敏方式
         */
        private String mask;

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRegex() {
            return regex;
        }

        public void setRegex(String regex) {
            this.regex = regex;
        }

        public String getMask() {
            return mask;
        }

        public void setMask(String mask) {
            this.mask = mask;
        }
    }

    /**
     * Trace 关联采样配置
     */
    public static class TraceSamplingProperties {
        /**
         * 是否启用 Trace 关联采样
         */
        private boolean enabled = true;

        /**
         * 未采样 Trace 的日志级别阈值
         */
        private String levelForUnsampled = "INFO";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLevelForUnsampled() {
            return levelForUnsampled;
        }

        public void setLevelForUnsampled(String levelForUnsampled) {
            this.levelForUnsampled = levelForUnsampled;
        }
    }
}
