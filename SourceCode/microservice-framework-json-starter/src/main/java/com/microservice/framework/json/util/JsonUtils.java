package com.microservice.framework.json.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.framework.json.api.JsonCodecException;

/**
 * Lightweight JSON utility for framework internals that cannot depend on a
 * Spring-managed {@code JsonCodec} bean.
 *
 * @author Andy Yang
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new JsonCodecException(JsonCodecException.JSON_INTERNAL_ERROR,
                    "Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new JsonCodecException(JsonCodecException.JSON_INTERNAL_ERROR,
                    "Failed to deserialize JSON", e);
        }
    }
}
