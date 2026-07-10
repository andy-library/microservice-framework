package com.microservice.framework.json.jackson;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecCustomizer;
import com.microservice.framework.json.api.JsonCodecException;
import com.microservice.framework.json.api.JsonTypeReference;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Jackson JSON 编解码器实现
 * <p>
 * 使用 Spring Boot 管理的 {@link ObjectMapper}，并在此基础上叠加框架安全配置：
 * <ul>
 *   <li>阻断多态反序列化（禁用 default typing）</li>
 *   <li>限制嵌套深度（通过 {@link StreamReadConstraints}）</li>
 *   <li>限制最大数据长度（通过 {@link StreamReadConstraints}）</li>
 *   <li>可选：遇到未知属性时是否失败（由 {@code framework.json.fail-on-unknown-properties} 控制）</li>
 * </ul>
 * <p>
 * 用户可通过注册 {@link JsonCodecCustomizer} Bean 进一步微调 ObjectMapper 配置。
 *
 * @author Andy Yang
 */
public class JacksonJsonCodec implements JsonCodec {

    private static final String IMPLEMENTATION_NAME = "jackson";

    private final ObjectMapper objectMapper;
    private final JsonProperties properties;

    /**
     * 创建 JacksonJsonCodec 实例
     *
     * @param baseMapper     Spring Boot 管理的基础 ObjectMapper
     * @param properties     JSON 配置属性
     * @param customizers    用户自定义定制器列表（可为空）
     */
    public JacksonJsonCodec(ObjectMapper baseMapper, JsonProperties properties,
                            List<JsonCodecCustomizer> customizers) {
        // 基于 Spring Boot 的 ObjectMapper 创建副本，避免修改共享实例
        ObjectMapper customizedMapper = baseMapper.copy();

        // 安全配置：根据配置决定是否在遇到未知属性时失败
        customizedMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                properties.getFailOnUnknownProperties());

        // 安全配置：限制嵌套深度和最大数据长度
        int maxDepth = properties.getMaxDepth();
        long maxPayloadSize = (long) properties.getMaxPayloadSize();

        StreamReadConstraints readConstraints = StreamReadConstraints.builder()
                .maxNestingDepth(maxDepth)
                .maxDocumentLength(maxPayloadSize)
                .build();
        customizedMapper.getFactory().setStreamReadConstraints(readConstraints);

        // 安全配置：限制写入时的嵌套深度
        StreamWriteConstraints writeConstraints = StreamWriteConstraints.builder()
                .maxNestingDepth(maxDepth)
                .build();
        customizedMapper.getFactory().setStreamWriteConstraints(writeConstraints);

        // 日期格式：默认使用 ISO-8601
        if (properties.getDateFormat() != null) {
            customizedMapper.setDateFormat(properties.getDateFormat());
        }

        // 禁止将空对象序列化为失败
        customizedMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 应用用户自定义定制器
        if (customizers != null) {
            for (JsonCodecCustomizer customizer : customizers) {
                customizer.customize(customizedMapper);
            }
        }

        this.objectMapper = customizedMapper;
        this.properties = properties;
    }

    @Override
    public String serialize(Object value) throws JsonCodecException {
        if (value == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "serialize value must not be null");
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw wrapException(e, "serialize");
        }
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) throws JsonCodecException {
        if (json == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "json string must not be null");
        }
        if (type == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "target type must not be null");
        }
        validatePayloadSize(json);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw wrapException(e, "deserialize to " + type.getName());
        }
    }

    @Override
    public <T> T deserialize(String json, JsonTypeReference<T> typeRef) throws JsonCodecException {
        if (json == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "json string must not be null");
        }
        if (typeRef == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "type reference must not be null");
        }
        validatePayloadSize(json);
        try {
            return objectMapper.readValue(json,
                    objectMapper.constructType(typeRef.getType()));
        } catch (Exception e) {
            throw wrapException(e, "deserialize to " + typeRef.getType());
        }
    }

    @Override
    public void serializeToStream(Object value, OutputStream out) throws JsonCodecException {
        if (value == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "serialize value must not be null");
        }
        if (out == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "output stream must not be null");
        }
        try {
            objectMapper.writeValue(out, value);
        } catch (Exception e) {
            throw wrapException(e, "serialize to stream");
        }
    }

    @Override
    public <T> T deserializeFromStream(InputStream in, Class<T> type) throws JsonCodecException {
        if (in == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "input stream must not be null");
        }
        if (type == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "target type must not be null");
        }
        try {
            return objectMapper.readValue(in, type);
        } catch (Exception e) {
            throw wrapException(e, "deserialize from stream to " + type.getName());
        }
    }

    @Override
    public <T> List<T> deserializeList(String json, Class<T> type) throws JsonCodecException {
        if (json == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "json string must not be null");
        }
        if (type == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "element type must not be null");
        }
        validatePayloadSize(json);
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, type));
        } catch (Exception e) {
            throw wrapException(e, "deserialize list of " + type.getName());
        }
    }

    @Override
    public <V> Map<String, V> deserializeMap(String json, Class<V> valueType) throws JsonCodecException {
        if (json == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "json string must not be null");
        }
        if (valueType == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "value type must not be null");
        }
        validatePayloadSize(json);
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory()
                            .constructMapType(Map.class, String.class, valueType));
        } catch (Exception e) {
            throw wrapException(e, "deserialize map with value type " + valueType.getName());
        }
    }

    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    /**
     * 返回底层 ObjectMapper 实例（仅供高级场景使用）
     *
     * @return 已配置的 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * 校验 JSON 数据长度不超过安全限制
     * <p>
     * Jackson 的 StreamReadConstraints 仅在流式读取时生效，
     * 因此需要手动校验字符串输入的长度。
     */
    private void validatePayloadSize(String json) {
        int maxPayloadSize = properties.getMaxPayloadSize();
        if (maxPayloadSize > 0 && json.length() > maxPayloadSize) {
            throw new JsonCodecException(JsonCodecException.JSON_PAYLOAD_EXCEEDED,
                    "JSON payload size " + json.length()
                    + " exceeds maximum allowed size " + maxPayloadSize);
        }
    }

    /**
     * 将底层异常包装为 JsonCodecException，保留错误类别信息
     */
    private JsonCodecException wrapException(Exception e, String operation) {
        if (e instanceof JsonCodecException) {
            return (JsonCodecException) e;
        }

        String message = e.getMessage();

        // 深度超限
        if (message != null && (message.contains("Nesting depth")
                || message.contains("maxNestingDepth"))) {
            return new JsonCodecException(JsonCodecException.JSON_DEPTH_EXCEEDED,
                    "JSON nesting depth exceeded limit during " + operation, e);
        }

        // 长度超限
        if (message != null && (message.contains("Document length")
                || message.contains("maxDocumentLength"))) {
            return new JsonCodecException(JsonCodecException.JSON_PAYLOAD_EXCEEDED,
                    "JSON payload size exceeded limit during " + operation, e);
        }

        // 未知属性
        if (e instanceof com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException) {
            return new JsonCodecException(JsonCodecException.JSON_UNKNOWN_PROPERTY,
                    "Unrecognized JSON property during " + operation + ": " + message, e);
        }

        // 类型不匹配
        if (e instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
            return new JsonCodecException(JsonCodecException.JSON_TYPE_MISMATCH,
                    "Type mismatch during " + operation + ": " + message, e);
        }

        // JSON 格式错误
        if (e instanceof com.fasterxml.jackson.core.JsonParseException) {
            return new JsonCodecException(JsonCodecException.JSON_FORMAT_ERROR,
                    "Invalid JSON format during " + operation, e);
        }

        // 通用内部错误
        return new JsonCodecException(JsonCodecException.JSON_INTERNAL_ERROR,
                "JSON operation failed during " + operation, e);
    }
}
