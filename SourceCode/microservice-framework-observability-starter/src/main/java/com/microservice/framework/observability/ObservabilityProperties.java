package com.microservice.framework.observability;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可观测性配置属性
 * 
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.observability")
@Validated
public class ObservabilityProperties {

    /**
     * 追踪配置
     */
    private TracingProperties tracing = new TracingProperties();

    /**
     * 指标配置
     */
    private MetricsProperties metrics = new MetricsProperties();

    public TracingProperties getTracing() {
        return tracing;
    }

    public void setTracing(TracingProperties tracing) {
        this.tracing = tracing;
    }

    public MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * 追踪配置
     */
    public static class TracingProperties {
        /**
         * 是否启用追踪
         */
        private boolean enabled = true;

        /**
         * 需要自动传播的 Baggage 键列表
         */
        private List<@NotBlank String> baggageKeys = new ArrayList<>();

        /**
         * Span 标签治理配置
         */
        private SpanTagProperties spanTags = new SpanTagProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getBaggageKeys() {
            return baggageKeys;
        }

        public void setBaggageKeys(List<String> baggageKeys) {
            this.baggageKeys = baggageKeys;
        }

        public SpanTagProperties getSpanTags() {
            return spanTags;
        }

        public void setSpanTags(SpanTagProperties spanTags) {
            this.spanTags = spanTags;
        }
    }

    /**
     * Span 标签治理配置
     */
    public static class SpanTagProperties {

        /**
         * 敏感标签键，命中后写入脱敏值
         */
        private Set<String> sensitiveKeys = new HashSet<>(Arrays.asList(
                "authorization",
                "cookie",
                "set-cookie",
                "password",
                "passwd",
                "secret",
                "token",
                "access-token",
                "refresh-token",
                "api-key",
                "apikey",
                "credential"));

        /**
         * 高基数字段，命中后写入脱敏值
         */
        private Set<String> highCardinalityKeys = new HashSet<>();

        /**
         * 敏感值正则表达式
         */
        private List<String> sensitiveValuePatterns = new ArrayList<>(List.of(
                "(?i).*(bearer\\s+|basic\\s+|password=|token=|secret=|api[_-]?key=).*"));

        /**
         * 标签值最大长度，0 表示不截断
         */
        private int maxValueLength = 0;

        /**
         * 脱敏占位值
         */
        private String redactedValue = "[REDACTED]";

        public Set<String> getSensitiveKeys() {
            return sensitiveKeys;
        }

        public void setSensitiveKeys(Set<String> sensitiveKeys) {
            this.sensitiveKeys = sensitiveKeys;
        }

        public Set<String> getHighCardinalityKeys() {
            return highCardinalityKeys;
        }

        public void setHighCardinalityKeys(Set<String> highCardinalityKeys) {
            this.highCardinalityKeys = highCardinalityKeys;
        }

        public List<String> getSensitiveValuePatterns() {
            return sensitiveValuePatterns;
        }

        public void setSensitiveValuePatterns(List<String> sensitiveValuePatterns) {
            this.sensitiveValuePatterns = sensitiveValuePatterns;
        }

        public int getMaxValueLength() {
            return maxValueLength;
        }

        public void setMaxValueLength(int maxValueLength) {
            this.maxValueLength = maxValueLength;
        }

        public String getRedactedValue() {
            return redactedValue;
        }

        public void setRedactedValue(String redactedValue) {
            this.redactedValue = redactedValue;
        }
    }

    /**
     * 指标配置
     */
    public static class MetricsProperties {
        /**
         * 是否启用指标
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}
