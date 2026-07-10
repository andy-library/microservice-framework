package com.microservice.framework.json.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecCustomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON 自动配置测试
 * <p>
 * 使用 ApplicationContextRunner 验证 Jackson/Fastjson2 实现的激活条件、
 * 互斥关系、属性绑定和用户自定义 Bean 覆盖默认 Bean 的行为。
 *
 * @author Andy Yang
 */
class JsonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    JsonAutoConfiguration.class,
                    JacksonJsonAutoConfiguration.class));

    // ======================================================================
     // 默认激活 Jackson
     // ======================================================================

    @Nested
    @DisplayName("默认配置激活 Jackson")
    class DefaultJacksonActivation {

        @Test
        @DisplayName("默认配置应激活 JacksonJsonCodec")
        void defaultShouldActivateJackson() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(JsonCodec.class);
                JsonCodec codec = context.getBean(JsonCodec.class);
                assertThat(codec.getImplementationName()).isEqualTo("jackson");
            });
        }

        @Test
        @DisplayName("默认配置 JsonCodec bean 名称应为 jacksonJsonCodec")
        void defaultBeanNameShouldBeJacksonJsonCodec() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("jacksonJsonCodec");
            });
        }
    }

    // ======================================================================
     // Fastjson2 激活条件
    // ======================================================================

    @Nested
    @DisplayName("Fastjson2 激活条件")
    class Fastjson2Activation {

        @Test
        @DisplayName("配置 provider=fastjson2 但缺少 fastjson2 依赖时不应激活任何 JsonCodec")
        void fastjson2WithoutDependencyShouldNotActivate() {
            // Fastjson2JsonAutoConfiguration requires @ConditionalOnClass("com.alibaba.fastjson2.JSON")
            // Without fastjson2 on classpath, this configuration is skipped.
            // JacksonAutoConfiguration has matchIfMissing=true, so provider=fastjson2
            // makes its havingValue mismatch, so Jackson won't activate either.
            contextRunner.withPropertyValues("framework.json.provider=fastjson2")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(JsonCodec.class);
                    });
        }
    }

    // ======================================================================
     // 用户自定义 Bean 覆盖
    // ======================================================================

    @Nested
    @DisplayName("用户自定义 Bean 覆盖")
    class UserProvidedBeanOverride {

        @Test
        @DisplayName("用户提供的 JsonCodec 应覆盖默认实现")
        void userProvidedJsonCodecShouldOverrideDefault() {
            JsonCodec customCodec = new JsonCodec() {
                @Override
                public String serialize(Object value) { return "custom"; }
                @Override
                public <T> T deserialize(String json, Class<T> type) { return null; }
                @Override
                public <T> T deserialize(String json, com.microservice.framework.json.api.JsonTypeReference<T> typeRef) { return null; }
                @Override
                public void serializeToStream(Object value, java.io.OutputStream out) {}
                @Override
                public <T> T deserializeFromStream(java.io.InputStream in, Class<T> type) { return null; }
                @Override
                public <T> java.util.List<T> deserializeList(String json, Class<T> type) { return java.util.Collections.emptyList(); }
                @Override
                public <V> java.util.Map<String, V> deserializeMap(String json, Class<V> valueType) { return java.util.Collections.emptyMap(); }
                @Override
                public String getImplementationName() { return "custom"; }
            };

            contextRunner.withBean("customJsonCodec", JsonCodec.class, () -> customCodec)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("customJsonCodec");
                        assertThat(context).doesNotHaveBean("jacksonJsonCodec");
                        assertThat(context.getBean(JsonCodec.class).getImplementationName())
                                .isEqualTo("custom");
                    });
        }
    }

    // ======================================================================
     // fail-on-unknown-properties 配置
    // ======================================================================

    @Nested
    @DisplayName("fail-on-unknown-properties 配置")
    class FailOnUnknownPropertiesConfig {

        @Test
        @DisplayName("默认 fail-on-unknown-properties 为 false，未知属性应被忽略")
        void defaultUnknownPropertiesShouldBeIgnored() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                JsonCodec codec = context.getBean(JsonCodec.class);
                String json = "{\"name\":\"test\",\"unknownProp\":\"val\"}";
                // Should not throw since fail-on-unknown-properties defaults to false
                assertThat(codec.deserialize(json, TestPojo.class)).isNotNull();
            });
        }

        @Test
        @DisplayName("配置 fail-on-unknown-properties=true 后未知属性应导致异常")
        void enabledUnknownPropertiesShouldFail() {
            contextRunner.withPropertyValues("framework.json.fail-on-unknown-properties=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        JsonCodec codec = context.getBean(JsonCodec.class);
                        String json = "{\"name\":\"test\",\"unknownProp\":\"val\"}";
                        org.assertj.core.api.Assertions.assertThatThrownBy(() -> codec.deserialize(json, TestPojo.class))
                                .isInstanceOf(com.microservice.framework.json.api.JsonCodecException.class);
                    });
        }
    }

    // ======================================================================
     // provider=jackson 显式配置
    // ======================================================================

    @Nested
    @DisplayName("显式配置 provider=jackson")
    class ExplicitJacksonProvider {

        @Test
        @DisplayName("显式配置 provider=jackson 应激活 JacksonJsonCodec")
        void explicitJacksonProviderShouldActivate() {
            contextRunner.withPropertyValues("framework.json.provider=jackson")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(JsonCodec.class);
                        assertThat(context.getBean(JsonCodec.class).getImplementationName())
                                .isEqualTo("jackson");
                    });
        }
    }

    // 测试数据对象
    public static class TestPojo {
        private String name;

        public TestPojo() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
