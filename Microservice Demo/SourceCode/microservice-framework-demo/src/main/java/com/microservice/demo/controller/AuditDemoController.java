package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * audit-starter demo endpoints.
 *
 * @author Andy Yang
 */
@RestController
@RequestMapping("/demo/audit")
public class AuditDemoController {

    private String latestId = UUID.randomUUID().toString();

    @PostMapping("/admin-action")
    public ApiResponse<Map<String, Object>> createAdminAction() {
        latestId = UUID.randomUUID().toString();
        return ApiResponse.success(Map.of("entryId", latestId, "action", "admin-action",
                "saved", true, "eventType", "ADMIN_ACTION"));
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> getLatest() {
        return ApiResponse.success(Map.of("entryId", latestId));
    }

    @GetMapping("/{entryId}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable String entryId) {
        return ApiResponse.success(Map.of("entryId", entryId, "found", true));
    }
}
