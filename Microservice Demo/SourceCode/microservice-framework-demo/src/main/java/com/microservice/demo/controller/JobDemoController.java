package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * xxl-job-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/job")
public class JobDemoController {

    @PostMapping({"/execute", "/run/daily-report"})
    public ApiResponse<Map<String, Object>> execute() {
        return ApiResponse.success(Map.of("executed", true, "jobId", 1001, "isDuplicate", false));
    }

    @PostMapping("/run/idempotent")
    public ApiResponse<Map<String, Object>> idempotent() {
        return ApiResponse.success(Map.of("idempotentWorks", true));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success(Map.of("handlerAvailable", true));
    }
}
