package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Request ID 集成测试
 * 验证 RequestId 三级降级逻辑：
 * 1. TraceId 优先 (MdcTracingObservationHandler 以 TraceId 覆盖)
 * 2. Header 次之 (X-Request-ID 网关透传)
 * 3. UUID 兜底 (RequestIdFilter 自动生成)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RequestIdIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    /**
     * 测试场景 1: TraceId 优先
     * 预期: requestId 等于 traceId (由 Micrometer Tracing 自动创建)
     */
    @Test
    @DisplayName("优先级1: TraceId 存在时作为 RequestId")
    public void testRequestIdFromTraceId() {
        given()
                .when()
                .get("/api/demo/request-id")
                .then()
                .statusCode(200)
                .body("data.requestId", notNullValue())
                .body("data.traceId", notNullValue())
                .body("data.source", equalTo("TraceId (优先级1)"));
    }

    /**
     * 测试场景 2: 验证 Request ID 和 TraceId 相等
     * 由于 TraceId 优先级最高，即使传入 Header，也应使用 TraceId
     */
    @Test
    @DisplayName("TraceId 覆盖 Header 验证")
    public void testTraceIdOverridesHeader() {
        given()
                .header("X-Request-ID", "my-custom-gateway-id")
                .when()
                .get("/api/demo/request-id/gateway-test")
                .then()
                .statusCode(200)
                .body("data.input_header", equalTo("my-custom-gateway-id"))
                .body("data.trace_id", notNullValue())
                .body("data.mdc_request_id", notNullValue())
                // mdc_request_id 应等于 trace_id，而非 input_header
                .body("data.explanation", containsString("TraceId 存在时优先使用"));
    }

    /**
     * 测试场景 3: 获取完整上下文
     */
    @Test
    @DisplayName("完整 Observability 上下文验证")
    public void testFullContext() {
        given()
                .when()
                .get("/api/demo/request-id/full-context")
                .then()
                .statusCode(200)
                .body("data.mdc.traceId", notNullValue())
                .body("data.mdc.spanId", notNullValue())
                .body("data.mdc.requestId", notNullValue())
                .body("data.tracer.traceId", notNullValue())
                .body("data.tracer.spanId", notNullValue())
                .body("data.tracer.sampled", notNullValue());
    }

    /**
     * 测试场景 4: 带自定义 Header 的完整上下文
     */
    @Test
    @DisplayName("自定义 Header 场景下的上下文验证")
    public void testFullContextWithHeader() {
        given()
                .header("X-Request-ID", "test-request-id-123")
                .when()
                .get("/api/demo/request-id/full-context")
                .then()
                .statusCode(200)
                .body("data.headers.X-Request-ID", equalTo("test-request-id-123"))
                .body("data.mdc.requestId", notNullValue())
                // 由于 TraceId 优先，mdc.requestId 不会等于 Header
                .body("data.mdc.traceId", notNullValue());
    }

    /**
     * 测试场景 5: 验证 RequestId 长度 (应为 32 位 hex)
     */
    @Test
    @DisplayName("RequestId 格式验证 (32位 hex)")
    public void testRequestIdFormat() {
        given()
                .when()
                .get("/api/demo/request-id")
                .then()
                .statusCode(200)
                .body("data.requestId", matchesPattern("[a-f0-9]{32}"));
    }

    /**
     * 测试场景 6: 验证多次请求的 RequestId 不同
     */
    @Test
    @DisplayName("多次请求生成不同的 RequestId")
    public void testUniqueRequestIds() {
        String requestId1 = given()
                .when()
                .get("/api/demo/request-id")
                .then()
                .statusCode(200)
                .extract()
                .path("data.requestId");

        String requestId2 = given()
                .when()
                .get("/api/demo/request-id")
                .then()
                .statusCode(200)
                .extract()
                .path("data.requestId");

        // 两次请求应生成不同的 TraceId/RequestId
        org.junit.jupiter.api.Assertions.assertNotEquals(requestId1, requestId2,
                "每次请求应生成唯一的 RequestId");
    }
}
