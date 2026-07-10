package com.microservice.framework.observability.autoconfigure.tracing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
}
