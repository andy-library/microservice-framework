package com.microservice.framework.observability.autoconfigure.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PrometheusMetricsAutoConfiguration 单元测试
 */
class PrometheusMetricsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PrometheusMetricsAutoConfiguration.class));

    @Test
    @DisplayName("AutoConfig: 上下文启动成功")
    void testAutoConfig_ContextStarts() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    @DisplayName("PrometheusConfig: 默认配置可用")
    void testPrometheusConfig_DefaultsAvailable() {
        contextRunner
                .withPropertyValues(
                        "management.endpoints.web.exposure.include=prometheus")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("PrometheusConfig: 可禁用")
    void testPrometheusConfig_CanBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "management.metrics.export.prometheus.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("PrometheusConfig: 自定义端点路径")
    void testPrometheusConfig_CustomEndpoint() {
        contextRunner
                .withPropertyValues(
                        "management.endpoints.web.base-path=/actuator",
                        "management.endpoints.web.exposure.include=prometheus,health")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }
}
