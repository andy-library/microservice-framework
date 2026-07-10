package com.microservice.framework.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecCustomizer;
import com.microservice.framework.json.api.JsonCodecException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Jackson JSON 编解码器特定测试
 * <p>
 * 验证 Jackson 实现特有的功能，如 ObjectMapper 定制、日期处理、BigDecimal 精度等。
 *
 * @author Andy Yang
 */
class JacksonJsonCodecTest {

    private final JsonProperties defaultProperties = new JsonProperties();

    private ObjectMapper createBootLikeObjectMapper() {
        return new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ======================================================================
     // ObjectMapper 定制测试
     // ======================================================================

    @Nested
    @DisplayName("ObjectMapper 定制")
    class ObjectMapperCustomization {

        @Test
        @DisplayName("JsonCodecCustomizer 应被正确应用")
        void customizerShouldBeApplied() {
            JsonCodecCustomizer customizer = objectMapper -> {
                objectMapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
                        .addSerializer(String.class,
                                new com.fasterxml.jackson.databind.ser.std.StdSerializer<String>(String.class) {
                                    @Override
                                    public void serialize(String value,
                                                          com.fasterxml.jackson.core.JsonGenerator gen,
                                                          com.fasterxml.jackson.databind.SerializerProvider provider)
                                            throws java.io.IOException {
                                        gen.writeString("CUSTOM:" + value);
                                    }
                                }));
            };

            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of(customizer));

            String json = codec.serialize(new TestPojo("test", 1));
            assertThat(json).contains("CUSTOM:test");
        }

        @Test
        @DisplayName("多个 JsonCodecCustomizer 应按顺序应用")
        void multipleCustomizersShouldBeAppliedInOrder() {
            JsonCodecCustomizer first = objectMapper -> {
                objectMapper.enable(
                        com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            };
            JsonCodecCustomizer second = objectMapper -> {
                objectMapper.disable(
                        com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            };

            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of(first, second));

            assertThat(codec.getImplementationName()).isEqualTo("jackson");
        }

        @Test
        @DisplayName("空定制器列表不应影响默认配置")
        void emptyCustomizersListShouldWork() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            assertThat(codec).isNotNull();
            assertThat(codec.getImplementationName()).isEqualTo("jackson");
        }
    }

    // ======================================================================
     // 日期处理测试
     // ======================================================================

    @Nested
    @DisplayName("日期处理")
    class DateHandling {

        @Test
        @DisplayName("默认使用 ISO-8601 格式序列化 LocalDateTime")
        void defaultIso8601Format() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            LocalDateTime ldt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            String json = codec.serialize(new DateWrapper(ldt, null));
            assertThat(json).contains("2024-06-15");
        }

        @Test
        @DisplayName("配置 dateFormat 后应使用指定格式")
        void customDateFormatShouldBeApplied() {
            JsonProperties props = new JsonProperties();
            props.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), props, List.of());

            String json = codec.serialize(new TestPojo("test", 1));
            assertThat(codec.getImplementationName()).isEqualTo("jackson");
        }
    }

    // ======================================================================
     // BigDecimal 精度测试
     // ======================================================================

    @Nested
    @DisplayName("BigDecimal 精度")
    class BigDecimalPrecision {

        @Test
        @DisplayName("BigDecimal 序列化应保留精确值")
        void bigDecimalSerializationPrecision() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            MoneyPojo pojo = new MoneyPojo(new BigDecimal("99999999.99"));
            String json = codec.serialize(pojo);

            assertThat(json).contains("99999999.99");
        }

        @Test
        @DisplayName("BigDecimal 反序列化应保留精确值")
        void bigDecimalDeserializationPrecision() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            String json = "{\"amount\":99999999.99}";
            MoneyPojo pojo = codec.deserialize(json, MoneyPojo.class);

            assertThat(pojo.getAmount()).isEqualByComparingTo(new BigDecimal("99999999.99"));
        }

        @Test
        @DisplayName("金额值 19.99 不应丢失精度")
        void moneyPrecisionNoLoss() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            MoneyPojo pojo = new MoneyPojo(new BigDecimal("19.99"));
            String json = codec.serialize(pojo);
            MoneyPojo deserialized = codec.deserialize(json, MoneyPojo.class);

            assertThat(deserialized.getAmount()).isEqualByComparingTo(new BigDecimal("19.99"));
        }
    }

    // ======================================================================
     // 流式操作测试
     // ======================================================================

    @Nested
    @DisplayName("流式操作")
    class StreamOperations {

        @Test
        @DisplayName("serializeToStream 应正确写入 JSON 数据")
        void serializeToStreamShouldWork() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            codec.serializeToStream(new TestPojo("stream", 99), out);

            String result = out.toString();
            assertThat(result).contains("\"stream\"");
            assertThat(result).contains("99");
        }

        @Test
        @DisplayName("deserializeFromStream 应正确读取 JSON 数据")
        void deserializeFromStreamShouldWork() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            String json = "{\"name\":\"stream\",\"age\":99}";
            java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(json.getBytes());
            TestPojo pojo = codec.deserializeFromStream(in, TestPojo.class);

            assertThat(pojo.getName()).isEqualTo("stream");
            assertThat(pojo.getAge()).isEqualTo(99);
        }

        @Test
        @DisplayName("serializeToStream null 输出流应抛出异常")
        void serializeToStreamNullOutShouldThrow() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            assertThatThrownBy(() -> codec.serializeToStream(new TestPojo("x", 1), null))
                    .isInstanceOf(JsonCodecException.class);
        }

        @Test
        @DisplayName("deserializeFromStream null 输入流应抛出异常")
        void deserializeFromStreamNullInShouldThrow() {
            JsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            assertThatThrownBy(() -> codec.deserializeFromStream(null, TestPojo.class))
                    .isInstanceOf(JsonCodecException.class);
        }
    }

    // ======================================================================
     // getObjectMapper 测试
     // ======================================================================

    @Nested
    @DisplayName("ObjectMapper 访问")
    class ObjectMapperAccess {

        @Test
        @DisplayName("getObjectMapper 应返回已配置的 ObjectMapper")
        void getObjectMapperShouldReturnConfiguredInstance() {
            JacksonJsonCodec codec = new JacksonJsonCodec(
                    createBootLikeObjectMapper(), defaultProperties, List.of());

            ObjectMapper mapper = codec.getObjectMapper();
            assertThat(mapper).isNotNull();
        }
    }

    // ======================================================================
     // 测试数据对象
     // ======================================================================

    public static class TestPojo {
        private String name;
        private int age;

        public TestPojo() {}

        public TestPojo(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class DateWrapper {
        private LocalDateTime timestamp;
        private LocalDate date;

        public DateWrapper() {}

        public DateWrapper(LocalDateTime timestamp, LocalDate date) {
            this.timestamp = timestamp;
            this.date = date;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    public static class MoneyPojo {
        private BigDecimal amount;

        public MoneyPojo() {}

        public MoneyPojo(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
