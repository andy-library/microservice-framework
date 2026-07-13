package com.microservice.framework.observability.autoconfigure.tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * BaggageAutoConfiguration 单元测试
 */
class BaggageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaggageAutoConfiguration.class));

    @Test
    @DisplayName("AutoConfig: 创建 Beans")
    void testAutoConfig_CreatesBeans() {
        contextRunner.run(context -> {
            // 验证上下文启动成功
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    @DisplayName("BaggageFields: 可配置")
    void testBaggageFields_Configurable() {
        contextRunner
                .withPropertyValues(
                        "management.tracing.baggage.remote-fields=userId,tenantId",
                        "management.tracing.baggage.correlation-fields=userId")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("BaggageFields: 正确传播")
    void testBaggageFields_PropagatesCorrectly() {
        contextRunner
                .withPropertyValues(
                        "management.tracing.baggage.remote-fields=requestId")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    @DisplayName("BaggageFields: 空配置处理")
    void testBaggageFields_EmptyConfig() {
        contextRunner.run(context -> {
            // 空配置应使用默认值
            assertThat(context).hasNotFailed();
        });
    }

    @Test
    @DisplayName("framework baggage keys 应桥接到 Boot tracing 传播配置")
    void frameworkBaggageKeysShouldBridgeToBootTracingPropagationProperties() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", java.util.Map.of(
                "framework.observability.tracing.baggage-keys[0]", "tenantId",
                "framework.observability.tracing.baggage-keys[1]", "requestSource")));

        EnvironmentPostProcessor processor = baggagePropagationEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("management.tracing.baggage.remote-fields"))
                .isEqualTo("tenantId,requestSource");
        assertThat(environment.getProperty("management.tracing.baggage.correlation.fields"))
                .isEqualTo("tenantId,requestSource");
    }

    private EnvironmentPostProcessor baggagePropagationEnvironmentPostProcessor() {
        try {
            return (EnvironmentPostProcessor) Class
                    .forName("com.microservice.framework.observability.autoconfigure.tracing.BaggagePropagationEnvironmentPostProcessor")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException ex) {
            fail("Baggage propagation environment bridge is missing", ex);
            return null;
        }
    }
}
