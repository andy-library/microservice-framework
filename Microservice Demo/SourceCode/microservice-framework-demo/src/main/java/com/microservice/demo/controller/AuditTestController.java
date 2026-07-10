package com.microservice.demo.controller;

import com.microservice.framework.web.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * audit-starter 应用侧能力测试入口。
 */
@RestController
@RequestMapping("/test/audit")
public class AuditTestController {

    private final AuditDemoController auditDemoController;

    public AuditTestController(AuditDemoController auditDemoController) {
        this.auditDemoController = auditDemoController;
    }

    @PostMapping("/admin-action")
    public ApiResponse<Map<String, Object>> adminAction() {
        return auditDemoController.createAdminAction();
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> latest() {
        return auditDemoController.getLatest();
    }

    @GetMapping("/{entryId}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable String entryId) {
        return auditDemoController.getById(entryId);
    }
}
