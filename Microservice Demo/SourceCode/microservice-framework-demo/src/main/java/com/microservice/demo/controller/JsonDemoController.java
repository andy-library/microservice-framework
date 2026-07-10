package com.microservice.demo.controller;

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

    @GetMapping("/provider")
    public ApiResponse<Map<String, Object>> provider() {
        return ApiResponse.success(Map.of("provider", "jackson"));
    }

    @PostMapping("/roundtrip")
    public ApiResponse<Map<String, Object>> roundtrip(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(Map.of("roundtrip", body, "match", true));
    }
}
