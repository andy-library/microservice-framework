package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * common-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/common")
public class CommonDemoController {

    @GetMapping("/id")
    public ApiResponse<Map<String, Object>> id() {
        return ApiResponse.success(Map.of("id", Math.abs(UUID.randomUUID().getMostSignificantBits()), "type", "snowflake"));
    }

    @GetMapping("/time")
    public ApiResponse<Map<String, Object>> time() {
        return ApiResponse.success(Map.of("time", ZonedDateTime.now().toString(), "zoneId", ZoneId.systemDefault().getId()));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> page() {
        return ApiResponse.success(Map.of("items", List.of("demo"), "page", 1, "size", 10));
    }

    @GetMapping("/context")
    public ApiResponse<Map<String, Object>> context() {
        return ApiResponse.success(Map.of("hasContext", true));
    }
}
