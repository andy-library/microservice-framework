package com.microservice.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.demo.job.ScheduledTaskDemo;
import com.microservice.framework.logging.util.LogBuilder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
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

import static org.junit.jupiter.api.Assertions.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * 追踪功能集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "framework.logging.masking.enabled=true",
        "framework.logging.flood-protection.enabled=true",
        "app.demo.job.rate=1000"
})
public class TracingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ScheduledTaskDemo scheduledTaskDemo;

    private Logger rootLogger;
    private Level originalLogBuilderLevel;
    private OutputStreamAppender<ILoggingEvent> memoryAppender;
    private ByteArrayOutputStream outputStream;
    private ObjectMapper objectMapper = new ObjectMapper();

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

        outputStream = new ByteArrayOutputStream();
        memoryAppender = new OutputStreamAppender<>();
        memoryAppender.setContext(context);
        memoryAppender.setOutputStream(outputStream);
        memoryAppender.setName("TRACING_MEMORY_APPENDER");

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(context);
        encoder.setIncludeMdcKeyNames(List.of("traceId", "spanId", "requestId"));
        encoder.start();

        memoryAppender.setEncoder(encoder);
        memoryAppender.start();
        rootLogger.addAppender(memoryAppender);
    }

    private List<JsonNode> getCapturedLogs() throws IOException {
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

    @Test
    public void testCurrentTrace() {
        given()
                .when()
                .get("/api/demo/tracing/current")
                .then()
                .statusCode(200)
                .body("traceId", notNullValue())
                .body("spanId", notNullValue());
    }

    @Test
    public void testBaggagePropagation() {
        given()
                .header("user-id", "user123")
                .header("tenant-id", "tenant456")
                .when()
                .get("/api/demo/tracing/baggage")
                .then()
                .statusCode(200)
                .body("userId", equalTo("user123"))
                .body("tenantId", equalTo("tenant456"))
                .body("traceId", notNullValue());
    }

    @Test
    public void testTraceSampling() {
        given()
                .when()
                .get("/api/demo/tracing/sampling")
                .then()
                .statusCode(200)
                .body("sampled", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    public void testActuatorHealth() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    public void testActuatorMetrics() {
        given()
                .when()
                .get("/actuator/metrics")
                .then()
                .statusCode(200)
                .body("names", hasItems("jvm.memory.used", "system.cpu.usage"));
    }

    @Test
    public void testActuatorPrometheus() {
        given()
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(200)
                .body(containsString("jvm_memory_used_bytes"));
    }

    @Test
    public void testSpanTag() {
        given()
                .param("value", "test-value")
                .when()
                .get("/api/demo/tracing/tag")
                .then()
                .statusCode(200)
                .body(equalTo("Tag added: test-value"));
    }

    @Test
    public void testRequestIdFallback() {
        // 1. Design Note: TraceId Priority 1 will override Header Priority 2
        // So requestId should be equal to traceId even if header is provided
        String customRequestId = "header-req-" + UUID.randomUUID();
        io.restassured.response.Response respWithHeader = given()
                .header("X-Request-ID", customRequestId)
                .when()
                .get("/api/demo/tracing/current");

        respWithHeader.then()
                .statusCode(200)
                .body("requestId", notNullValue())
                .body("traceId", notNullValue());

        assertEquals((Object) respWithHeader.path("traceId"), (Object) respWithHeader.path("requestId"),
                "TraceId should override Header as per Priority 1");

        // 2. 测试降级到 TraceId (Priority 1 wins)
        io.restassured.response.Response response = given()
                .when()
                .get("/api/demo/tracing/current");

        response.then()
                .statusCode(200)
                .body("requestId", notNullValue())
                .body("traceId", notNullValue());

        String traceId = response.path("traceId");
        String requestId = response.path("requestId");
        assertEquals((Object) traceId, (Object) requestId, "RequestId should equal TraceId when tracing is enabled");
    }

    @Test
    public void testScheduledTaskTracing() throws Exception {
        scheduledTaskDemo.executeDailyReport();
        long startTime = System.currentTimeMillis();
        boolean found = false;
        while (System.currentTimeMillis() - startTime < 5000) {
            List<JsonNode> logs = getCapturedLogs();
            found = logs.stream()
                    .anyMatch(log -> log.path("message").asText().contains("日报")
                            && log.has("traceId"));
            if (found)
                break;
            Thread.sleep(500);
        }

        assertTrue(found, "定时任务应该被追踪并记录日志");
    }
}
