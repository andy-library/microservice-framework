package com.microservice.framework.json.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonCodecException;
import com.microservice.framework.json.api.JsonTypeReference;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fastjson2 JSON 编解码器实现
 * <p>
 * 仅当用户显式配置 {@code framework.json.provider=fastjson2} 且
 * Fastjson2 依赖存在于类路径时才会激活。
 * <p>
 * 安全配置：
 * <ul>
 *   <li>阻断多态反序列化（禁用 AutoType）</li>
 *   <li>限制最大 JSON 数据长度</li>
 *   <li>限制最大嵌套深度</li>
 * </ul>
 *
 * @author Andy Yang
 */
public class Fastjson2JsonCodec implements JsonCodec {

    private static final String IMPLEMENTATION_NAME = "fastjson2";

    private final JsonProperties properties;

    /**
     * 创建 Fastjson2JsonCodec 实例
     *
     * @param properties JSON 配置属性
     */
    public Fastjson2JsonCodec(JsonProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建带有最大嵌套深度限制的 JSONReader.Context
     *
     * @return 配置好的 Context
     */
    private JSONReader.Context createReaderContext() {
        JSONReader.Context context = new JSONReader.Context();
        context.setMaxLevel(properties.getMaxDepth());
        return context;
    }

    @Override
    public String serialize(Object value) throws JsonCodecException {
        if (value == null) {
            throw new JsonCodecException(JsonCodecException.JSON_PARAM_NULL,
                    "serialize value must not be null");
        }
        try {
            return JSON.toJSONString(value, JSONWriter.Feature.WriteBigDecimalAsPlain);
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
        validateNestingDepth(json);
        try {
            return JSON.parseObject(json, type, createReaderContext());
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
        validateNestingDepth(json);
        try {
            Type targetType = typeRef.getType();
            return JSON.parseObject(json, targetType, createReaderContext());
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
            JSON.writeTo(out, value, JSONWriter.Feature.WriteBigDecimalAsPlain);
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
            return JSON.parseObject(in, StandardCharsets.UTF_8, type, createReaderContext());
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
        validateNestingDepth(json);
        try {
            try (JSONReader reader = JSONReader.of(json, createReaderContext())) {
                return reader.readArray(type);
            }
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
        validateNestingDepth(json);
        try {
            Map<String, Object> rawValues;
            try (JSONReader reader = JSONReader.of(json, createReaderContext())) {
                rawValues = reader.readObject();
            }
            Map<String, V> typedValues = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : rawValues.entrySet()) {
                String valueJson = JSON.toJSONString(entry.getValue());
                typedValues.put(entry.getKey(), JSON.parseObject(valueJson, valueType, createReaderContext()));
            }
            return typedValues;
        } catch (Exception e) {
            throw wrapException(e, "deserialize map with value type " + valueType.getName());
        }
    }

    @Override
    public String getImplementationName() {
        return IMPLEMENTATION_NAME;
    }

    /**
     * 校验 JSON 数据长度不超过安全限制
     */
    private void validatePayloadSize(String json) {
        int maxPayloadSize = properties.getMaxPayloadSize();
        if (maxPayloadSize > 0 && json.length() > maxPayloadSize) {
            throw new JsonCodecException(JsonCodecException.JSON_PAYLOAD_EXCEEDED,
                    "JSON payload size " + json.length()
                    + " exceeds maximum allowed size " + maxPayloadSize);
        }
    }

    private void validateNestingDepth(String json) {
        int maxDepth = properties.getMaxDepth();
        if (maxDepth <= 0) {
            return;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{' || current == '[') {
                depth++;
                if (depth > maxDepth) {
                    throw new JsonCodecException(JsonCodecException.JSON_DEPTH_EXCEEDED,
                            "JSON nesting depth exceeded limit: " + maxDepth);
                }
            } else if (current == '}' || current == ']') {
                depth--;
            }
        }
    }

    /**
     * 将底层异常包装为 JsonCodecException
     */
    private JsonCodecException wrapException(Exception e, String operation) {
        if (e instanceof JsonCodecException) {
            return (JsonCodecException) e;
        }

        String message = e.getMessage();

        // 深度超限
        if (message != null && (message.contains("level")
                || message.contains("depth"))) {
            return new JsonCodecException(JsonCodecException.JSON_DEPTH_EXCEEDED,
                    "JSON nesting depth exceeded limit during " + operation, e);
        }

        // 通用内部错误
        return new JsonCodecException(JsonCodecException.JSON_INTERNAL_ERROR,
                "JSON operation failed during " + operation, e);
    }
}
