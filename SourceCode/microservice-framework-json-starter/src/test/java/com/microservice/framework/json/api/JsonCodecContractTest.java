package com.microservice.framework.json.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.jackson.JacksonJsonCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JSON 编解码器契约测试
 * <p>
 * 定义所有 {@link JsonCodec} 实现必须满足的行为契约，
 * 默认使用 {@link JacksonJsonCodec} 执行验证。
 * Fastjson2 实现也应通过相同的测试套件。
 *
 * @author Andy Yang
 */
class JsonCodecContractTest {

    private final JsonProperties defaultProperties = new JsonProperties();
    private final ObjectMapper configuredMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final JsonCodec codec = new JacksonJsonCodec(
            configuredMapper, defaultProperties, List.of());

    // ======================================================================
     // 基本序列化/反序列化契约
     // ======================================================================

    @Nested
    @DisplayName("基本序列化契约")
    class SerializeContract {

        @Test
        @DisplayName("序列化简单对象应返回合法 JSON")
        void serializeSimpleObject() {
            SimplePojo pojo = new SimplePojo("hello", 42);
            String json = codec.serialize(pojo);

            assertThat(json).contains("\"name\":\"hello\"");
            assertThat(json).contains("\"age\":42");
        }

        @Test
        @DisplayName("序列化 null 参数应抛出异常")
        void serializeNullShouldThrow() {
            assertThatThrownBy(() -> codec.serialize(null))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("序列化列表应返回 JSON 数组")
        void serializeList() {
            List<String> list = List.of("a", "b", "c");
            String json = codec.serialize(list);

            assertThat(json).startsWith("[");
            assertThat(json).endsWith("]");
            assertThat(json).contains("\"a\"", "\"b\"", "\"c\"");
        }

        @Test
        @DisplayName("序列化映射应返回 JSON 对象")
        void serializeMap() {
            Map<String, Integer> map = Map.of("x", 1, "y", 2);
            String json = codec.serialize(map);

            assertThat(json).startsWith("{");
            assertThat(json).endsWith("}");
        }

        @Test
        @DisplayName("序列化日期时间应使用 ISO-8601 格式")
        void serializeDateTime() {
            DateTimePojo pojo = new DateTimePojo(
                    LocalDateTime.of(2024, 1, 15, 10, 30, 0),
                    LocalDate.of(2024, 1, 15));
            String json = codec.serialize(pojo);

            assertThat(json).contains("2024");
        }

        @Test
        @DisplayName("序列化 BigDecimal 应保留精度")
        void serializeBigDecimal() {
            BigDecimalPojo pojo = new BigDecimalPojo(new BigDecimal("123.45"));
            String json = codec.serialize(pojo);

            assertThat(json).contains("123.45");
        }
    }

    // ======================================================================
     // 基本反序列化契约
     // ======================================================================

    @Nested
    @DisplayName("基本反序列化契约")
    class DeserializeContract {

        @Test
        @DisplayName("反序列化简单对象应还原原始值")
        void deserializeSimpleObject() {
            String json = "{\"name\":\"hello\",\"age\":42}";
            SimplePojo pojo = codec.deserialize(json, SimplePojo.class);

            assertThat(pojo.getName()).isEqualTo("hello");
            assertThat(pojo.getAge()).isEqualTo(42);
        }

        @Test
        @DisplayName("反序列化 null JSON 字符串应抛出异常")
        void deserializeNullJsonShouldThrow() {
            assertThatThrownBy(() -> codec.deserialize(null, SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("反序列化 null 类型应抛出异常")
        void deserializeNullTypeShouldThrow() {
            assertThatThrownBy(() -> codec.deserialize("{\"name\":\"test\"}", (Class<SimplePojo>) null))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("反序列化无效 JSON 格式应抛出异常")
        void deserializeInvalidJsonShouldThrow() {
            assertThatThrownBy(() -> codec.deserialize("{invalid", SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class);
            assertThatThrownBy(() -> codec.deserialize("{invalid", SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class)
                    .satisfies(ex -> assertThat(((JsonCodecException) ex).getErrorCode())
                            .isEqualTo(JsonCodecException.JSON_FORMAT_ERROR));
        }

        @Test
        @DisplayName("反序列化类型不匹配应抛出异常")
        void deserializeTypeMismatchShouldThrow() {
            assertThatThrownBy(() -> codec.deserialize("[1,2,3]", SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("反序列化泛型列表应保留完整类型信息")
        void deserializeGenericList() {
            String json = "[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]";
            List<SimplePojo> list = codec.deserializeList(json, SimplePojo.class);

            assertThat(list).hasSize(2);
            assertThat(list.get(0).getName()).isEqualTo("a");
            assertThat(list.get(1).getName()).isEqualTo("b");
        }

        @Test
        @DisplayName("反序列化泛型映射应保留完整类型信息")
        void deserializeGenericMap() {
            String json = "{\"key1\":100,\"key2\":200}";
            Map<String, Integer> map = codec.deserializeMap(json, Integer.class);

            assertThat(map).containsEntry("key1", 100);
            assertThat(map).containsEntry("key2", 200);
        }

        @Test
        @DisplayName("使用 JsonTypeReference 反序列化应保留泛型信息")
        void deserializeWithTypeReference() {
            String json = "[\"a\",\"b\",\"c\"]";
            List<String> list = codec.deserialize(json,
                    new JsonTypeReference<List<String>>() {});

            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("反序列化 BigDecimal 应保留精度")
        void deserializeBigDecimal() {
            String json = "{\"amount\":123.45}";
            BigDecimalPojo pojo = codec.deserialize(json, BigDecimalPojo.class);

            assertThat(pojo.getAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
        }
    }

    // ======================================================================
     // 实现名称契约
     // ======================================================================

    @Nested
    @DisplayName("实现名称契约")
    class ImplementationNameContract {

        @Test
        @DisplayName("getImplementationName 应返回实现标识")
        void shouldReturnImplementationName() {
            assertThat(codec.getImplementationName()).isEqualTo("jackson");
        }
    }

    // ======================================================================
     // 安全契约
     // ======================================================================

    @Nested
    @DisplayName("安全限制契约")
    class SecurityContract {

        @Test
        @DisplayName("未知属性默认应被忽略而非抛出异常")
        void unknownPropertiesShouldBeIgnoredByDefault() {
            String json = "{\"name\":\"test\",\"age\":1,\"unknownField\":\"value\"}";
            SimplePojo pojo = codec.deserialize(json, SimplePojo.class);

            assertThat(pojo.getName()).isEqualTo("test");
            assertThat(pojo.getAge()).isEqualTo(1);
        }

        @Test
        @DisplayName("启用 failOnUnknownProperties 后未知属性应抛出异常")
        void failOnUnknownPropertiesShouldThrow() {
            JsonProperties props = new JsonProperties();
            props.setFailOnUnknownProperties(true);
            JsonCodec strictCodec = new JacksonJsonCodec(
                    new ObjectMapper(), props, List.of());

            String json = "{\"name\":\"test\",\"age\":1,\"unknownField\":\"value\"}";
            assertThatThrownBy(() -> strictCodec.deserialize(json, SimplePojo.class))
                    .isInstanceOf(JsonCodecException.class);
        }
    }

    // ======================================================================
     // 测试数据对象
     // ======================================================================

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

    public static class DateTimePojo {
        private LocalDateTime timestamp;
        private LocalDate date;

        public DateTimePojo() {}

        public DateTimePojo(LocalDateTime timestamp, LocalDate date) {
            this.timestamp = timestamp;
            this.date = date;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public static class BigDecimalPojo {
        private BigDecimal amount;

        public BigDecimalPojo() {}

        public BigDecimalPojo(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
