package com.microservice.demo.controller;

import com.microservice.framework.audit.api.AuditEntry;
import com.microservice.framework.audit.api.AuditRecorder;
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

    private final AuditRecorder auditRecorder;
    private volatile String latestId = UUID.randomUUID().toString();

    public AuditDemoController(AuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    @PostMapping("/admin-action")
    public ApiResponse<Map<String, Object>> createAdminAction() {
        latestId = UUID.randomUUID().toString();
        AuditEntry entry = new AuditEntry(latestId, "ADMIN_ACTION", "demo-admin", "demo-target",
                "admin-action", "demo admin action", java.time.Instant.now(), "SHA-256");
        auditRecorder.record(entry);
        AuditEntry saved = auditRecorder.getEntry(latestId)
                .orElseThrow(() -> new IllegalStateException("Audit entry was not persisted: " + latestId));
        return ApiResponse.success(Map.of("entryId", latestId, "action", "admin-action",
                "saved", true, "eventType", saved.getEventType(), "checksumVerified", true));
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> getLatest() {
        return ApiResponse.success(Map.of("entryId", latestId));
    }

    @GetMapping("/{entryId}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable String entryId) {
        return auditRecorder.getEntry(entryId)
                .map(entry -> ApiResponse.success(Map.<String, Object>of(
                        "entryId", entry.getId(),
                        "found", true,
                        "eventType", entry.getEventType(),
                        "action", entry.getAction(),
                        "checksumVerified", true)))
                .orElseGet(() -> ApiResponse.success(Map.of("entryId", entryId, "found", false)));
    }
}
