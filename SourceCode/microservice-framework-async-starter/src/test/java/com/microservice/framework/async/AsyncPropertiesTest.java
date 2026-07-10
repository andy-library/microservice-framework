package com.microservice.framework.async;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AsyncProperties 绑定和默认值测试
 * <p>
 * 验证各嵌套配置组的默认值符合设计规格，
 * 并验证属性绑定后值能正确覆盖默认值。
 *
 * @author Andy Yang
 */
class AsyncPropertiesTest {

    @Test
    @DisplayName("默认线程池配置应与设计规格一致")
    void defaultPoolPropertiesShouldMatchSpecification() {
        AsyncProperties properties = new AsyncProperties();
        AsyncProperties.PoolProperties pool = properties.getPool();

        assertThat(pool.getCoreSize()).isEqualTo(4);
        assertThat(pool.getMaxSize()).isEqualTo(8);
        assertThat(pool.getQueueCapacity()).isEqualTo(256);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("async-");
        assertThat(pool.getRejection()).isEqualTo(AsyncProperties.RejectionPolicy.CALLER_RUNS);
    }

    @Test
    @DisplayName("默认上下文传播配置应与设计规格一致")
    void defaultContextPropertiesShouldMatchSpecification() {
        AsyncProperties properties = new AsyncProperties();
        AsyncProperties.ContextProperties context = properties.getContext();

        assertThat(context.getPropagationEnabled()).isTrue();
    }

    @Test
    @DisplayName("默认优雅关停配置应与设计规格一致")
    void defaultShutdownPropertiesShouldMatchSpecification() {
        AsyncProperties properties = new AsyncProperties();
        AsyncProperties.ShutdownProperties shutdown = properties.getShutdown();

        assertThat(shutdown.getAwaitTermination()).isTrue();
        assertThat(shutdown.getAwaitTerminationSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("自定义属性值应能覆盖默认值")
    void customPropertyValuesShouldOverrideDefaults() {
        AsyncProperties properties = new AsyncProperties();

        properties.getPool().setCoreSize(8);
        properties.getPool().setMaxSize(16);
        properties.getPool().setQueueCapacity(512);
        properties.getPool().setThreadNamePrefix("worker-");
        properties.getPool().setRejection(AsyncProperties.RejectionPolicy.ABORT);

        assertThat(properties.getPool().getCoreSize()).isEqualTo(8);
        assertThat(properties.getPool().getMaxSize()).isEqualTo(16);
        assertThat(properties.getPool().getQueueCapacity()).isEqualTo(512);
        assertThat(properties.getPool().getThreadNamePrefix()).isEqualTo("worker-");
        assertThat(properties.getPool().getRejection()).isEqualTo(AsyncProperties.RejectionPolicy.ABORT);

        properties.getContext().setPropagationEnabled(false);
        assertThat(properties.getContext().getPropagationEnabled()).isFalse();

        properties.getShutdown().setAwaitTermination(false);
        properties.getShutdown().setAwaitTerminationSeconds(60);
        assertThat(properties.getShutdown().getAwaitTermination()).isFalse();
        assertThat(properties.getShutdown().getAwaitTerminationSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("RejectionPolicy 枚举应包含所有定义的策略")
    void rejectionPolicyEnumShouldContainAllDefinedStrategies() {
        AsyncProperties.RejectionPolicy[] policies = AsyncProperties.RejectionPolicy.values();

        assertThat(policies).containsExactlyInAnyOrder(
                AsyncProperties.RejectionPolicy.ABORT,
                AsyncProperties.RejectionPolicy.CALLER_RUNS,
                AsyncProperties.RejectionPolicy.DISCARD_OLDEST);
    }
}
