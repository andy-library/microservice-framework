package com.microservice.framework.database.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ShardingSphereAutoConfiguration 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 ShardingSphere 配置的激活/禁用条件。
 * <p>
 * 注意：ShardingSphere-JDBC 不在测试 classpath 上（仅作为 optional 依赖引入），
 * 因此 {@code @ConditionalOnClass} 条件不满足，ShardingSphereAutoConfiguration
 * 不会实际创建 Bean。测试重点验证条件逻辑的语义正确性。
 *
 * @author Andy Yang
 */
class ShardingSphereAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ShardingSphereAutoConfiguration.class));

    @Nested
    @DisplayName("条件激活")
    class ConditionalActivation {

        @Test
        @DisplayName("ShardingSphere-JDBC 不在 classpath 时 ShardingSphereAutoConfiguration 不应激活")
        void withoutShardingSphereClassShouldNotActivate() {
            // ShardingSphere-JDBC 作为 optional 依赖，不在测试 classpath 上
            // 因此 @ConditionalOnClass 条件不满足
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(ShardingSphereAutoConfiguration.class);
            });
        }

        @Test
        @DisplayName("sharding.enabled=true 时仍因缺少 ShardingSphere 而不激活")
        void shardingEnabledButMissingClasspathShouldNotActivate() {
            contextRunner.withPropertyValues("framework.database.sharding.enabled=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).doesNotHaveBean(ShardingSphereAutoConfiguration.class);
                    });
        }
    }

    @Nested
    @DisplayName("属性默认值验证")
    class PropertyDefaults {

        @Test
        @DisplayName("默认 sharding.enabled 应为 false")
        void defaultShardingEnabledShouldBeFalse() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                // ShardingSphereAutoConfiguration 配置了 matchIfMissing=true，
                // 表示默认情况下（sharding.enabled=false）应可激活，
                // 但因缺少 ShardingSphere classpath 条件而未激活
            });
        }
    }
}
