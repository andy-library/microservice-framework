package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Real Feign starter API acceptance test.
 *
 * <p>This suite verifies that feign-starter capabilities can be used through
 * real demo HTTP APIs, not only through bean presence or isolated propagator
 * unit tests.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"full-embedded", "feign-acceptance"})
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@DisplayName("Feign Starter API 验收")
class RealFeignAcceptanceTest {

    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("真实 Feign 调用应传播上下文和服务身份且调用后恢复线程上下文")
    void realFeignCallPropagatesContextAndRestoresCallerContext() {
        String requestId = "feign-acceptance-" + RUN_ID;
        String userId = "user-" + RUN_ID;

        given().header("X-Request-ID", requestId)
                .header("user-id", userId)
                .when().get("/demo/feign/real-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.downstream.requestId", notNullValue())
                .body("data.downstream.userId", equalTo(userId))
                .body("data.clientClass", containsString("MeteredFeignClient"))
                .body("data.downstream.serviceIdentity", equalTo("microservice-framework-demo"))
                .body("data.callerContextAfter.requestId", notNullValue())
                .body("data.callerContextAfter.userId", equalTo(userId));
    }

    @Test
    @DisplayName("真实 Feign 调用应按配置触发读取超时")
    void realFeignCallHonorsReadTimeout() {
        given().queryParam("delayMs", 300)
                .when().get("/demo/feign/timeout-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.timedOut", equalTo(true))
                .body("data.elapsedMs", greaterThanOrEqualTo(100))
                .body("data.exceptionType", notNullValue());
    }

    @Test
    @DisplayName("真实 Feign 调用应按配置执行 503 重试并恢复")
    void realFeignCallHonorsRetryPolicy() {
        given().queryParam("failureCount", 1)
                .when().get("/demo/feign/retry-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.recovered", equalTo(true))
                .body("data.attempts", equalTo(2));
    }

    @Test
    @DisplayName("真实 Feign 非幂等 POST 调用遇到 503 不应重试")
    void realFeignPostCallShouldNotRetryNonIdempotentRequest() {
        given().queryParam("failureCount", 1)
                .when().post("/demo/feign/non-idempotent-retry-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.retried", equalTo(false))
                .body("data.attempts", equalTo(1))
                .body("data.exceptionType", notNullValue());
    }

    @Test
    @DisplayName("真实 Feign 连接池参数应按配置生效")
    void realFeignConnectionPoolShouldHonorConfiguredLimits() {
        given()
                .when().get("/demo/feign/connection-pool")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.maxTotal", equalTo(123))
                .body("data.defaultMaxPerRoute", equalTo(45))
                .body("data.clientClass", containsString("MeteredFeignClient"));
    }

    @Test
    @DisplayName("真实 Feign 调用应记录 Micrometer 指标")
    void realFeignCallShouldRecordMicrometerMetrics() {
        given().header("X-Request-ID", "feign-metrics-" + RUN_ID)
                .header("user-id", "metrics-user-" + RUN_ID)
                .when().get("/demo/feign/real-call")
                .then().statusCode(200)
                .body("code", equalTo(0));

        given()
                .when().get("/actuator/metrics/framework.feign.client.requests")
                .then().statusCode(200)
                .body("name", equalTo("framework.feign.client.requests"))
                .body("measurements[0].value", greaterThanOrEqualTo(1.0f));
    }

    @Test
    @DisplayName("真实 Feign 并发调用不应串上下文")
    void realFeignConcurrentCallsShouldNotLeakContext() {
        IntStream.range(0, 12).parallel().forEach(index -> {
            String requestId = "feign-concurrent-" + RUN_ID + "-" + index;
            String userId = "user-concurrent-" + RUN_ID + "-" + index;

            io.restassured.path.json.JsonPath json = given()
                    .header("X-Request-ID", requestId)
                    .header("user-id", userId)
                    .when().get("/demo/feign/real-call")
                    .then().statusCode(200)
                    .body("code", equalTo(0))
                    .extract().jsonPath();

            String propagatedRequestId = json.getString("data.downstream.requestId");
            assertNotNull(propagatedRequestId);
            assertEquals(userId, json.getString("data.downstream.userId"));
            assertEquals(propagatedRequestId, json.getString("data.callerContextAfter.requestId"));
            assertEquals(userId, json.getString("data.callerContextAfter.userId"));
        });
    }
}
