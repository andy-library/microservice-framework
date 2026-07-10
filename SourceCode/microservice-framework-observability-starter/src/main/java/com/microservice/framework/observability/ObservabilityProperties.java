package com.microservice.framework.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 可观测性配置属性
 * 
 * @author Andy Yang
 */
@ConfigurationProperties(prefix = "framework.observability")
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
        private List<String> baggageKeys = new ArrayList<>();

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
