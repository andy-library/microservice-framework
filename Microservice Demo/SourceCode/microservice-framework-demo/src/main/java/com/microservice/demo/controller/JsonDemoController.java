package com.microservice.demo.controller;

import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.json.api.JsonTypeReference;
import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * json-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/json")
public class JsonDemoController {

    private final JsonCodec jsonCodec;

    public JsonDemoController(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    @GetMapping("/provider")
    public ApiResponse<Map<String, Object>> provider() {
        return ApiResponse.success(Map.of("provider", jsonCodec.getImplementationName()));
    }

    @PostMapping("/roundtrip")
    public ApiResponse<Map<String, Object>> roundtrip(@RequestBody Map<String, Object> body) {
        String serialized = jsonCodec.serialize(body);
        Map<String, Object> roundtrip = jsonCodec.deserialize(serialized, new JsonTypeReference<Map<String, Object>>() {
        });
        return ApiResponse.success(Map.of("roundtrip", roundtrip, "match", roundtrip.equals(body),
                "codec", jsonCodec.getImplementationName(), "serialized", serialized));
    }
}
