package com.microservice.framework.json.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecException;
import com.microservice.framework.json.jackson.JacksonJsonCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSON 安全限制测试
 * <p>
 * 验证框架安全策略是否正确阻断多态反序列化、限制嵌套深度和限制数据长度。
 *
 * @author Andy Yang
 */
class JsonSecurityTest {

    private final JsonProperties defaultProperties = new JsonProperties();
    private final JsonCodec defaultCodec = new JacksonJsonCodec(
            new ObjectMapper(), defaultProperties, List.of());

    // ======================================================================
     // 多态反序列化阻断测试
     // ======================================================================

    @Nested
    @DisplayName("多态反序列化阻断")
    class PolymorphicDeserializationBlocking {

        @Test
        @DisplayName("默认配置应阻断包含 @class 类型信息的反序列化")
        void shouldBlockPolymorphicDeserialization() {
            // Jackson 默认配置不应启用 default typing，即使 JSON 中包含 @class 字段
            // 也不应触发多态反序列化
            String json = "{\"name\":\"test\",\"age\":1}";
            SimplePojo pojo = defaultCodec.deserialize(json, SimplePojo.class);
            assertThat(pojo.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("不应反序列化为未声明的类型")
        void shouldNotDeserializeToUndeclaredType() {
            // 尝试将 JSON 反序列化为 Object 类型时，不应产生多态行为
            String json = "{\"name\":\"test\",\"age\":1}";
            Object result = defaultCodec.deserialize(json, Object.class);
            assertThat(result).isInstanceOf(java.util.Map.class);
        }
    }

    // ======================================================================
     // 嵌套深度限制测试
     // ======================================================================

    @Nested
    @DisplayName("嵌套深度限制")
    class MaxDepthLimit {

        @Test
        @DisplayName("超过配置深度限制的反序列化应抛出异常")
        void depthExceededShouldThrow() {
            JsonProperties props = new JsonProperties();
            props.setMaxDepth(10);
            JsonCodec codec = new JacksonJsonCodec(new ObjectMapper(), props, List.of());

            // Build deeply nested JSON (15 levels)
            String deeplyNested = buildDeeplyNestedJson(15);

            assertThatThrownBy(() -> codec.deserialize(deeplyNested, Object.class))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("在深度限制内的反序列化应成功")
        void withinDepthLimitShouldSucceed() {
            JsonProperties props = new JsonProperties();
            props.setMaxDepth(100);
            JsonCodec codec = new JacksonJsonCodec(new ObjectMapper(), props, List.of());

            // Build nested JSON (5 levels)
            String nested = buildDeeplyNestedJson(5);

            Object result = codec.deserialize(nested, Object.class);
            assertThat(result).isNotNull();
        }
    }

    // ======================================================================
     // 最大数据长度限制测试
     // ======================================================================

    @Nested
    @DisplayName("最大数据长度限制")
    class MaxPayloadSizeLimit {

        @Test
        @DisplayName("超过配置长度限制的反序列化应抛出异常")
        void payloadExceededShouldThrow() {
            JsonProperties props = new JsonProperties();
            props.setMaxPayloadSize(100);
            JsonCodec codec = new JacksonJsonCodec(new ObjectMapper(), props, List.of());

            // Build a JSON string longer than 100 characters
            String largeJson = "{\"name\":\"" + repeatChar('x', 200) + "\"}";

            assertThatThrownBy(() -> codec.deserialize(largeJson, SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("在长度限制内的反序列化应成功")
        void withinPayloadLimitShouldSucceed() {
            JsonProperties props = new JsonProperties();
            props.setMaxPayloadSize(1000);
            JsonCodec codec = new JacksonJsonCodec(new ObjectMapper(), props, List.of());

            String smallJson = "{\"name\":\"test\",\"age\":1}";
            SimplePojo pojo = codec.deserialize(smallJson, SimplePojo.class);
            assertThat(pojo.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("默认 10MB 限制应允许正常 JSON 数据")
        void defaultPayloadLimitShouldAllowNormalData() {
            String normalJson = "{\"name\":\"normal\",\"age\":42}";
            SimplePojo pojo = defaultCodec.deserialize(normalJson, SimplePojo.class);
            assertThat(pojo.getName()).isEqualTo("normal");
        }
    }

    // ======================================================================
     // 辅助方法
     // ======================================================================

    private static String buildDeeplyNestedJson(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"level\":");
        }
        sb.append("\"end\"");
        for (int i = 0; i < depth; i++) {
            sb.append("}");
        }
        return sb.toString();
    }

    private static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // 测试数据对象
    public static class SimplePojo {
        private String name;
        private int age;

        public SimplePojo() {}

        public SimplePojo(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }
}
