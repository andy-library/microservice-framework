package com.microservice.framework.json.fastjson2;

import com.microservice.framework.json.JsonProperties;
import com.microservice.framework.json.api.JsonCodecException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Fastjson2JsonCodecTest {

    @Test
    @DisplayName("deserializeMap should convert values to requested value type")
    void deserializeMapShouldConvertValuesToRequestedValueType() {
        Fastjson2JsonCodec codec = new Fastjson2JsonCodec(new JsonProperties());

        Map<String, OrderValue> result = codec.deserializeMap(
                "{\"order\":{\"id\":\"O-1\",\"amount\":99}}",
                OrderValue.class);

        assertThat(result.get("order")).isInstanceOf(OrderValue.class);
        assertThat(result.get("order").getId()).isEqualTo("O-1");
        assertThat(result.get("order").getAmount()).isEqualTo(99);
    }

    @Test
    @DisplayName("deserializeList should enforce max depth")
    void deserializeListShouldEnforceMaxDepth() {
        JsonProperties properties = new JsonProperties();
        properties.setMaxDepth(1);
        Fastjson2JsonCodec codec = new Fastjson2JsonCodec(properties);

        assertThatThrownBy(() -> codec.deserializeList("[{\"nested\":{\"value\":\"x\"}}]", OrderValue.class))
                .isInstanceOf(JsonCodecException.class)
                .hasMessageContaining("nesting depth");
    }

    public static class OrderValue {
        private String id;
        private int amount;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }
}
