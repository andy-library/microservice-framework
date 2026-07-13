package com.microservice.demo;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.microservice.framework.logging.core.masking.MaskingJsonGeneratorDecorator;
import com.microservice.framework.logging.util.Log;
import com.microservice.framework.logging.util.LogBuilder;
import com.microservice.demo.job.ScheduledTaskDemo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Microservice Framework 综合能力验证测试
 * 涵盖：日志 (Structured/Masking/Flood/Exception), 追踪
 * (TraceId/Span/Baggage/Scheduled), RequestID 传递与降级
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "framework.logging.masking.enabled=true",
        "framework.logging.flood-protection.enabled=true",
        "app.demo.job.rate=60000"
})
public class FrameworkVerificationTest {

    @LocalServerPort
    private int port;

    private Logger rootLogger;
    private Level originalLogBuilderLevel;
    private OutputStreamAppender<ILoggingEvent> memoryAppender;
    private ByteArrayOutputStream outputStream;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private MaskingJsonGeneratorDecorator maskingDecorator;

    @Autowired
    private ScheduledTaskDemo scheduledTaskDemo;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        setupMemoryAppender();
    }

    @AfterEach
    public void tearDown() {
        if (rootLogger != null && memoryAppender != null) {
            rootLogger.detachAppender(memoryAppender);
            memoryAppender.stop();
            rootLogger.setLevel(originalLogBuilderLevel);
        }
    }

    private void setupMemoryAppender() {
        Logger logBuilderLogger = (Logger) LoggerFactory.getLogger(LogBuilder.class);
        LoggerContext context = logBuilderLogger.getLoggerContext();
        rootLogger = logBuilderLogger;
        originalLogBuilderLevel = logBuilderLogger.getLevel();
        logBuilderLogger.setLevel(Level.INFO);

        // 不再清理 TurboFilter，而是尝试重置它们
        context.getTurboFilterList().forEach(f -> {
            if (f instanceof com.microservice.framework.logging.core.filter.RateLimitingTurboFilter) {
                f.start(); // 调用 start 会重置令牌桶
            }
        });

        outputStream = new ByteArrayOutputStream();
        memoryAppender = new OutputStreamAppender<>();
        memoryAppender.setContext(context);
        memoryAppender.setOutputStream(outputStream);
        memoryAppender.setName("VERIFICATION_MEMORY_APPENDER");

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        encoder.setIncludeMdcKeyNames(List.of("traceId", "spanId", "requestId", "structured_data"));

        if (maskingDecorator != null) {
            encoder.setJsonGeneratorDecorator(maskingDecorator);
        } else {
            System.err.println("WARNING: MaskingJsonGeneratorDecorator is NULL in test context!");
        }

        encoder.start();
        memoryAppender.setEncoder(encoder);
        memoryAppender.start();
        rootLogger.addAppender(memoryAppender);
    }

    public List<JsonNode> getCapturedLogs() throws IOException {
        try {
            Thread.sleep(300); // 等待异步日志写回
        } catch (InterruptedException ignored) {
        }

        String logs = outputStream.toString(StandardCharsets.UTF_8);
        List<JsonNode> jsonLogs = new ArrayList<>();
        if (logs.isEmpty())
            return jsonLogs;

        String[] lines = logs.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                try {
                    jsonLogs.add(objectMapper.readTree(line));
                } catch (Exception ignored) {
                }
            }
        }
        return jsonLogs;
    }

    // ========================================================================
    // Logging Capabilities
    // ========================================================================

    @Test
    public void testDirectLog() throws IOException {
        Log.info("Direct log test").log();
        List<JsonNode> logs = getCapturedLogs();
        assertFalse(logs.isEmpty(), "Should capture direct log");
        assertTrue(logs.stream().anyMatch(l -> "Direct log test".equals(l.path("message").asText())));
    }

    @Test
    public void testStructuredLogging() throws IOException {
        given().when().get("/api/demo/logging/structured").then().statusCode(200);
        List<JsonNode> logs = getCapturedLogs();
        boolean found = logs.stream().anyMatch(log -> "结构化日志演示".equals(log.path("message").asText()) &&
                (log.has("structured_data") || log.path("mdc").has("structured_data")));
        if (!found) {
            System.out.println("Structured log not found. Captured logs:");
            logs.forEach(l -> System.out.println(" - " + l.toString()));
        }
        assertTrue(found, "未找到结构化日志");
    }

    @Test
    public void testDataMasking() throws IOException {
        given().when().get("/api/demo/logging/masking").then().statusCode(200);
        List<JsonNode> logs = getCapturedLogs();
        JsonNode targetLog = logs.stream()
                .filter(log -> log.path("message").asText().contains("数据脱敏演示"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到脱敏日志 in " + logs));

        String structuredData = targetLog.path("structured_data").asText();
        assertFalse(structuredData.isEmpty(), "structured_data should exist: " + targetLog);
        assertTrue(structuredData.contains("138****5678"),
                "Phone in structured_data should be masked: " + structuredData);
        assertTrue(structuredData.contains("********"),
                "ID card in structured_data should be masked: " + structuredData);
    }

    @Test
    public void testFloodProtection() throws IOException {
        given().when().get("/api/demo/logging/flood").then().statusCode(200);
        List<JsonNode> logs = getCapturedLogs();
        long count = logs.stream().filter(log -> "压力测试日志".equals(log.path("message").asText())).count();
        assertTrue(count > 0, "Should capture at least one flood log, count: " + count);
        assertTrue(count < 500, "Should be rate limited, count: " + count);
    }

    @Test
    public void testExceptionLogging() throws IOException, InterruptedException {
        given().when().get("/api/demo/logging/exception").then().statusCode(200);
        // 异常日志捕获可能稍慢
        Thread.sleep(200);
        List<JsonNode> logs = getCapturedLogs();
        JsonNode errorLog = logs.stream()
                .filter(log -> "异常日志演示".equals(log.path("message").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到异常日志"));
        assertEquals("ERROR", errorLog.get("level").asText());
        assertTrue(errorLog.has("stack_trace"));
    }

    // ========================================================================
    // Tracing Capabilities
    // ========================================================================

    @Test
    public void testTraceIdPropagation() {
        given().when().get("/api/demo/tracing/current").then()
                .statusCode(200)
                .body("data.traceId", notNullValue())
                .body("data.spanId", notNullValue());
    }

    @Test
    public void testBaggagePropagation() {
        given().header("user-id", "verify-user").header("tenant-id", "verify-tenant")
                .when().get("/api/demo/tracing/baggage").then()
                .statusCode(200)
                .body("data.userId", equalTo("verify-user"))
                .body("data.tenantId", equalTo("verify-tenant"));
    }

    @Test
    public void testScheduledTaskTracing() throws Exception {
        scheduledTaskDemo.executeDailyReport();
        long startTime = System.currentTimeMillis();
        boolean found = false;
        while (System.currentTimeMillis() - startTime < 10000) {
            List<JsonNode> logs = getCapturedLogs();
            found = logs.stream()
                    .anyMatch(log -> log.path("message").asText().contains("日报")
                            && log.has("traceId"));
            if (found)
                break;
            Thread.sleep(1000);
        }
        assertTrue(found, "定时任务应该被追踪并记录日志");
    }

    // ========================================================================
    // RequestID Capabilities
    // ========================================================================

    @Test
    public void testRequestIdPriority() {
        String customId = "verify-req-" + UUID.randomUUID();
        io.restassured.response.Response resp = given().header("X-Request-ID", customId)
                .when().get("/api/demo/tracing/current");

        resp.then().statusCode(200);
        String traceId = resp.path("data.traceId");
        String requestId = resp.path("data.requestId");

        // Priority 1: TraceId wins over Header Priority 2
        assertEquals(traceId, requestId, "RequestId should equal TraceId by Priority 1");
    }

    // ========================================================================
    // Actuator & Metrics
    // ========================================================================

    @Test
    public void testActuatorEndPoints() {
        given().when().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"));
        given().when().get("/actuator/metrics").then().statusCode(200).body("names", hasItems("jvm.memory.used"));
    }
}
