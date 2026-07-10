package com.microservice.demo.controller;

import com.microservice.framework.logging.util.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志演示 Controller
 * 专门用于演示各种日志功能
 */
@RestController
@RequestMapping("/api/demo/logging")
public class LogDemoController {

    /**
     * 演示结构化日志
     */
    @GetMapping("/structured")
    public ResponseEntity<Map<String, String>> structuredLog() {
        Log.info("结构化日志演示")
                .with("key1", "value1")
                .with("key2", "value2")
                .with("timestamp", System.currentTimeMillis())
                .log();

        return ResponseEntity.ok(Map.of("message", "Check console for structured log"));
    }

    /**
     * 演示数据脱敏
     */
    @GetMapping("/masking")
    public ResponseEntity<Map<String, String>> maskingDemo() {
        // 这些敏感信息会在日志中被自动脱敏
        Log.info("数据脱敏演示")
                .with("phone", "13812345678")
                .with("idCard", "110101199001011234")
                .with("email", "test@example.com")
                .with("bankCard", "6222021234567890123")
                .log();

        return ResponseEntity.ok(Map.of("message", "Check console for masked data"));
    }

    /**
     * 演示日志限流（日志风暴防护）
     */
    @GetMapping("/flood")
    public ResponseEntity<Map<String, Object>> floodProtection() {
        int totalLogs = 1000;
        int allowedLogs = 0;

        for (int i = 0; i < totalLogs; i++) {
            Log.info("压力测试日志")
                    .with("index", i)
                    .with("timestamp", System.currentTimeMillis())
                    .log();
            allowedLogs++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAttempted", totalLogs);
        result.put("message", "Check console - only first ~100 logs should appear due to flood protection");

        return ResponseEntity.ok(result);
    }

    /**
     * 演示异常日志
     */
    @GetMapping("/exception")
    public ResponseEntity<Map<String, String>> exceptionLog() {
        try {
            throw new RuntimeException("This is a test exception");
        } catch (Exception e) {
            Log.error("异常日志演示")
                    .withException(e)
                    .with("errorType", "RuntimeException")
                    .with("testCase", "exception-logging")
                    .log();
        }

        return ResponseEntity.ok(Map.of("message", "Exception logged with auto-extracted fields"));
    }

    /**
     * 演示不同日志级别
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, String>> logLevels() {
        Log.debug("Testing debug log level").with("level", "DEBUG").log();
        Log.info("INFO 级别日志").with("level", "INFO").log();
        Log.warn("WARN 级别日志").with("level", "WARN").log();

        try {
            throw new IllegalStateException("Test error");
        } catch (Exception e) {
            Log.error("ERROR 级别日志").withException(e).with("level", "ERROR").log();
        }

        return ResponseEntity.ok(Map.of("message", "Check console for different log levels"));
    }
}
