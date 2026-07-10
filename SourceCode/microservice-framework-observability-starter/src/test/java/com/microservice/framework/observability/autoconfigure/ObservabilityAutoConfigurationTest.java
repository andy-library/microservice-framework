package com.microservice.framework.observability.autoconfigure;

import com.microservice.framework.observability.autoconfigure.common.MdcAutoConfiguration;
import com.microservice.framework.observability.autoconfigure.metrics.PrometheusMetricsAutoConfiguration;
import com.microservice.framework.observability.autoconfigure.tracing.BaggageAutoConfiguration;
import com.microservice.framework.observability.autoconfigure.tracing.SpanTagAspectAutoConfiguration;
import com.microservice.framework.observability.autoconfigure.tracing.TraceableAspectAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;


import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityProductionSafetyAutoConfiguration.class,
                    MdcAutoConfiguration.class,
                    BaggageAutoConfiguration.class,
                    TraceableAspectAutoConfiguration.class,
                    SpanTagAspectAutoConfiguration.class,
                    PrometheusMetricsAutoConfiguration.class));

    @Test
    @DisplayName("非 Web 应用没有 Tracer Bean 时仍可启动")
    void nonWebApplicationWithoutTracerStillStarts() {
        contextRunner
                .withPropertyValues("spring.main.web-application-type=" + WebApplicationType.NONE.name())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("observabilityUtilsInitializer");
                    assertThat(context).doesNotHaveBean("traceableAspect");
                    assertThat(context).doesNotHaveBean("spanTagAspect");
                });
    }

    @Test
    @DisplayName("空 baggage key 应在配置绑定阶段失败")
    void blankBaggageKeyShouldFailFast() {
        contextRunner
                .withPropertyValues("framework.observability.tracing.baggage-keys[0]=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("生产环境不允许关闭 tracing")
    void prodShouldRejectDisabledTracing() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.observability.tracing.enabled=false",
                        "framework.observability.tracing.baggage-keys[0]=userId")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.observability.tracing.enabled cannot be disabled in prod profile");
                });
    }

    @Test
    @DisplayName("生产环境不允许关闭 metrics")
    void prodShouldRejectDisabledMetrics() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.observability.metrics.enabled=false",
                        "framework.observability.tracing.baggage-keys[0]=userId")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.observability.metrics.enabled cannot be disabled in prod profile");
                });
    }

    @Test
    @DisplayName("生产环境必须声明 baggage 传播键")
    void prodShouldRejectEmptyBaggageKeys() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("framework.observability.tracing.baggage-keys must not be empty in prod profile");
                });
    }

    @Test
    @DisplayName("生产推荐配置应通过可观测准入校验")
    void prodShouldAcceptRecommendedObservabilityBoundaries() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "framework.observability.tracing.enabled=true",
                        "framework.observability.metrics.enabled=true",
                        "framework.observability.tracing.baggage-keys[0]=userId",
                        "framework.observability.tracing.baggage-keys[1]=requestSource")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
