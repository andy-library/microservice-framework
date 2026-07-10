package com.microservice.framework.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 ObservabilityProperties 的默认配置和属性设置
 *
 * 覆盖场景：
 * 1. TracingProperties 默认值和设置
 * 2. MetricsProperties 默认值和设置
 */
class ObservabilityPropertiesTest {

    // ==================== TracingProperties 测试 ====================

    @Test
    @DisplayName("TracingProperties: 默认配置 enabled=true, baggageKeys 为空列表")
    void testTracingDefaultValues() {
        ObservabilityProperties.TracingProperties tracing = new ObservabilityProperties.TracingProperties();

        assertTrue(tracing.isEnabled(), "追踪默认应启用");
        assertNotNull(tracing.getBaggageKeys(), "baggageKeys 不应为 null");
        assertTrue(tracing.getBaggageKeys().isEmpty(), "baggageKeys 默认应为空列表");
    }

    @Test
    @DisplayName("TracingProperties: 设置属性值")
    void testTracingSetValues() {
        ObservabilityProperties.TracingProperties tracing = new ObservabilityProperties.TracingProperties();

        tracing.setEnabled(false);
        tracing.getBaggageKeys().add("userId");
        tracing.getBaggageKeys().add("tenantId");

        assertFalse(tracing.isEnabled());
        assertEquals(2, tracing.getBaggageKeys().size());
        assertTrue(tracing.getBaggageKeys().contains("userId"));
        assertTrue(tracing.getBaggageKeys().contains("tenantId"));
    }

    // ==================== MetricsProperties 测试 ====================

    @Test
    @DisplayName("MetricsProperties: 默认配置 enabled=true")
    void testMetricsDefaultValues() {
        ObservabilityProperties.MetricsProperties metrics = new ObservabilityProperties.MetricsProperties();

        assertTrue(metrics.isEnabled(), "指标默认应启用");
    }

    @Test
    @DisplayName("MetricsProperties: 设置属性值")
    void testMetricsSetValues() {
        ObservabilityProperties.MetricsProperties metrics = new ObservabilityProperties.MetricsProperties();

        metrics.setEnabled(false);

        assertFalse(metrics.isEnabled());
    }

    // ==================== ObservabilityProperties 整体测试 ====================

    @Test
    @DisplayName("ObservabilityProperties: 整体结构初始化验证")
    void testObservabilityPropertiesStructure() {
        ObservabilityProperties props = new ObservabilityProperties();

        assertNotNull(props.getTracing(), "Tracing 配置不应为 null");
        assertNotNull(props.getMetrics(), "Metrics 配置不应为 null");
    }

    @Test
    @DisplayName("ObservabilityProperties: 设置子配置对象")
    void testObservabilityPropertiesSetSubProperties() {
        ObservabilityProperties props = new ObservabilityProperties();
        ObservabilityProperties.TracingProperties newTracing = new ObservabilityProperties.TracingProperties();
        ObservabilityProperties.MetricsProperties newMetrics = new ObservabilityProperties.MetricsProperties();

        newTracing.setEnabled(false);
        newMetrics.setEnabled(false);

        props.setTracing(newTracing);
        props.setMetrics(newMetrics);

        assertFalse(props.getTracing().isEnabled());
        assertFalse(props.getMetrics().isEnabled());
    }
}
