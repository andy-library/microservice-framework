package com.microservice.framework.observability.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus 指标自动配置
 * 启用 Prometheus 端点
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "framework.observability.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PrometheusMetricsAutoConfiguration {

    /**
     * Prometheus 配置
     * 注意：Spring Boot Actuator 会自动暴露 /actuator/prometheus 端点
     * 这里只需要确保配置正确即可
     */
    @Configuration(proxyBeanMethods = false)
    static class PrometheusConfiguration {
        // Prometheus 端点由 Spring Boot Actuator 自动配置
        // 通过 management.endpoints.web.exposure.include=prometheus 启用
    }
}
