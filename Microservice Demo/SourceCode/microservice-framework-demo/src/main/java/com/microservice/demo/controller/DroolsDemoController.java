package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * drools-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/drools")
public class DroolsDemoController {

    @PostMapping({"/execute", "/member-level"})
    public ApiResponse<Map<String, Object>> execute(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.success(Map.of("matched", true, "engineAvailable", true,
                "memberLevel", "GOLD", "input", body == null ? Map.of() : body));
    }

    @GetMapping("/rule-version")
    public ApiResponse<Map<String, Object>> version() {
        return ApiResponse.success(Map.of("coordinates", "demo-rules:member-level:1.0.0"));
    }
}
