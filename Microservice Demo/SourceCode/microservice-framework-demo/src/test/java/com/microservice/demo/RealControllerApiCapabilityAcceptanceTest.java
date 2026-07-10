package com.microservice.demo;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Real controller API acceptance test for all starter capabilities.
 *
 * <p>This suite validates that starter capabilities can be used through normal
 * business-facing HTTP APIs in the demo application. It requires local MySQL,
 * Redis, Kafka and Elasticsearch and is intentionally opt-in.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("full-middleware")
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@DisplayName("真实中间件 Controller API 全能力验收")
class RealControllerApiCapabilityAcceptanceTest {

    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "");
    private static final String TOPIC = "microservice-framework-demo-topic";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("common/json/web：基础开发能力 API 可落地")
    void commonJsonAndWebApisWork() {
        given().when().get("/demo/common/id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.type", equalTo("snowflake"));

        given().when().get("/demo/common/time")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.time", notNullValue())
                .body("data.zoneId", equalTo("Asia/Shanghai"));

        given().when().get("/demo/common/page")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(3))
                .body("data.items", hasSize(3))
                .body("data.pageNumber", equalTo(1))
                .body("data.pageSize", equalTo(10));

        given().when().get("/demo/common/context")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.hasContext", equalTo(true));

        given().when().get("/demo/json/provider")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.provider", notNullValue());

        given().contentType("application/json")
                .body("""
                        {"name":"api-acceptance","value":42,"nested":{"enabled":true}}
                        """)
                .when().post("/demo/json/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.match", equalTo(true))
                .body("data.roundtrip.nested.enabled", equalTo(true));

        given().when().get("/demo/web/success")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.framework", equalTo("working"));

        given().when().get("/demo/web/business-error")
                .then().statusCode(400)
                .body("code", not(equalTo(0)))
                .body("message", containsString("demo business error"));

        given().contentType("application/json")
                .body("{\"name\":\"acceptance\",\"age\":30}")
                .when().post("/demo/web/validation-error")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.name", equalTo("acceptance"));

        given().contentType("application/json")
                .body("{\"name\":\"\",\"age\":200}")
                .when().post("/demo/web/validation-error")
                .then().statusCode(400)
                .body("code", not(equalTo(0)))
                .body("details", hasKey("name"))
                .body("details", hasKey("age"));

        given().header("X-Request-ID", "api-acceptance-" + RUN_ID)
                .when().get("/demo/web/request-id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.requestId", not(equalTo("not-set")));
    }

    @Test
    @DisplayName("logging/observability/security/async：治理链路 API 可落地")
    void governanceAndRuntimeApisWork() {
        given().when().get("/api/demo/logging/structured")
                .then().statusCode(200)
                .body("message", containsString("structured"));

        given().when().get("/api/demo/logging/masking")
                .then().statusCode(200)
                .body("message", containsString("masked"));

        given().when().get("/api/demo/logging/exception")
                .then().statusCode(200)
                .body("message", containsString("Exception logged"));

        given().when().get("/api/demo/logging/levels")
                .then().statusCode(200)
                .body("message", containsString("different log levels"));

        given().when().get("/api/demo/logging/flood")
                .then().statusCode(200)
                .body("totalAttempted", equalTo(1000));

        given().when().get("/api/demo/tracing/current")
                .then().statusCode(200)
                .body("traceId", notNullValue())
                .body("spanId", notNullValue());

        given().header("user-id", "user-" + RUN_ID)
                .header("tenant-id", "tenant-demo")
                .when().get("/api/demo/tracing/baggage")
                .then().statusCode(200)
                .body("userId", equalTo("user-" + RUN_ID))
                .body("tenantId", equalTo("tenant-demo"))
                .body("traceId", notNullValue());

        given().when().get("/api/demo/tracing/sampling")
                .then().statusCode(200)
                .body("message", notNullValue());

        given().queryParam("value", "api-tag-" + RUN_ID)
                .when().get("/api/demo/tracing/tag")
                .then().statusCode(200)
                .body(equalTo("Tag added: api-tag-" + RUN_ID));

        given().queryParam("channel", "api")
                .when().get("/api/demo/metrics/counter")
                .then().statusCode(200)
                .body("metric", equalTo("business.order.created"))
                .body("tags.channel", equalTo("api"));

        given().when().get("/api/demo/metrics/timer")
                .then().statusCode(200)
                .body("metric", equalTo("business.payment.process"))
                .body("actualDurationMs", greaterThan(0));

        given().when().get("/demo/security/public")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("public"));

        given().when().get("/demo/security/internal")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("internal"));

        given().contentType("application/json")
                .when().post("/demo/async/run")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.completed", equalTo(true))
                .body("data.taskResult", containsString("async-result"));

        given().when().get("/demo/async/context")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.propagationWorks", equalTo(true));

        given().when().get("/demo/async/metrics")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.poolSize", greaterThan(0));
    }

    @Test
    @DisplayName("database/redis/kafka/elasticsearch：真实中间件 API 可落地")
    void middlewareApisWork() {
        String orderNo = "CTRL-MYSQL-" + RUN_ID;
        Integer orderId = given().contentType("application/json")
                .body("""
                        {"orderNo":"%s","amount":199.99,"status":"CREATED"}
                        """.formatted(orderNo))
                .when().post("/demo/database/order")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", greaterThan(0))
                .body("data.orderNo", equalTo(orderNo))
                .body("data.operation", equalTo("create"))
                .extract().path("data.id");

        given().when().get("/demo/database/order/{id}", orderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.orderNo", equalTo(orderNo));

        given().contentType("application/json")
                .when().post("/demo/database/transaction")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.verificationPassed", equalTo(true));

        given().when().get("/demo/database/routing/current")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.routingHint", equalTo("PRIMARY"))
                .body("data.isPrimary", equalTo(true));

        String cacheKey = "controller:api:redis:" + RUN_ID;
        given().contentType("application/json")
                .body("{\"value\":\"real-controller-cache\"}")
                .when().put("/demo/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("put"));

        given().when().get("/demo/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));

        given().when().delete("/demo/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.deleted", equalTo(true));

        given().when().get("/demo/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(false));

        given().contentType("application/json")
                .body(List.of(cacheKey + ":1", cacheKey + ":2"))
                .when().post("/demo/redis/cache/batch-delete")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("batchDelete"))
                .body("data.count", equalTo(2));

        given().contentType("application/json")
                .when().post("/demo/redis/lock")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.locked", equalTo(true))
                .body("data.unlocked", equalTo(true));

        given().when().get("/demo/redis/rate-limit")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.acquired", equalTo(true));

        given().contentType("application/json")
                .when().post("/demo/redis/counter/increment")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.newValue", greaterThan(0));

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","message":"sync-controller-%s"}
                        """.formatted(TOPIC, RUN_ID))
                .when().post("/demo/kafka/send")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.topic", equalTo(TOPIC))
                .body("data.status", not(equalTo("send-failed")))
                .body("data.recordMetadataClass", equalTo("RecordMetadata"));

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","message":"async-controller-%s"}
                        """.formatted(TOPIC, RUN_ID))
                .when().post("/demo/kafka/send-async")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.topic", equalTo(TOPIC))
                .body("data.status", equalTo("async-sent"));

        given().when().get("/demo/kafka/consumer/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaPublisherAvailable", equalTo(true))
                .body("data.kafkaConsumerBuilderAvailable", equalTo(true));

        given().contentType("application/json")
                .when().post("/demo/kafka/dlq/replay")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaAvailable", equalTo(true))
                .body("data.status", equalTo("notional"));

        String esOrderId = "ctrl-es-" + RUN_ID;
        given().contentType("application/json")
                .body("""
                        {"orderId":"%s","orderNo":"CTRL-ES-%s","amount":299.99,"status":"CREATED"}
                        """.formatted(esOrderId, RUN_ID))
                .when().post("/demo/es/order/index")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.indexed", equalTo(true))
                .body("data.id", equalTo(esOrderId));

        given().when().get("/demo/es/order/{id}", esOrderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true))
                .body("data.document.orderId", equalTo(esOrderId));

        given().queryParam("status", "CREATED")
                .when().get("/demo/es/order/search")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", greaterThan(0));

        given().when().delete("/demo/es/order/{id}", esOrderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.deleted", equalTo(true));
    }

    @Test
    @DisplayName("feign/audit/encryption/storage/drools/xxl-job：扩展组件 API 可落地")
    void extensionApisWork() {
        given().when().get("/demo/feign/context-propagation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.headerCount", greaterThan(0));

        given().when().get("/demo/feign/timeout-policy")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.propagatorAvailable", equalTo(true))
                .body("data.restoredRequestId", equalTo("downstream-request-001"));

        given().contentType("application/json")
                .when().post("/demo/audit/admin-action")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.saved", equalTo(true))
                .body("data.checksumVerified", equalTo(true));

        given().when().get("/demo/audit/latest")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", notNullValue());

        String plaintext = "sensitive-controller-" + RUN_ID;
        Response encryptResponse = given().contentType("application/json")
                .body("""
                        {"plaintext":"%s"}
                        """.formatted(plaintext))
                .when().post("/demo/encryption/encrypt")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.isEncryptedAfterEncrypt", equalTo(true))
                .body("data.ciphertext", not(equalTo(plaintext)))
                .extract().response();

        String ciphertext = encryptResponse.path("data.ciphertext");
        String secondCiphertext = given().contentType("application/json")
                .body("""
                        {"plaintext":"%s"}
                        """.formatted(plaintext))
                .when().post("/demo/encryption/encrypt")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.isEncryptedAfterEncrypt", equalTo(true))
                .extract().path("data.ciphertext");
        assertNotEquals(ciphertext, secondCiphertext, "默认随机 AEAD 加密同一明文应产生不同密文");

        given().contentType("application/json")
                .body("""
                        {"ciphertext":"%s"}
                        """.formatted(ciphertext))
                .when().post("/demo/encryption/decrypt")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.decryptedText", equalTo(plaintext));

        given().contentType("application/json")
                .body("""
                        {"plaintext":"%s"}
                        """.formatted(plaintext))
                .when().post("/demo/encryption/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.roundtripMatch", equalTo(true));

        String storageKey = "controller-storage-" + RUN_ID;
        given().contentType("application/json")
                .body("""
                        {"key":"%s","content":"controller api storage content"}
                        """.formatted(storageKey))
                .when().post("/demo/storage/object")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("upload"));

        given().when().get("/demo/storage/object/{key}", storageKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("download"))
                .body("data.content", equalTo("controller api storage content"));

        given().when().get("/demo/storage/object/{key}/presigned-url", storageKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.downloadUrl", notNullValue())
                .body("data.uploadUrl", notNullValue());

        given().when().delete("/demo/storage/object/{key}", storageKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.existsBeforeDelete", equalTo(true))
                .body("data.existsAfterDelete", equalTo(false));

        given().contentType("application/json")
                .body("{\"spendingAmount\":12000}")
                .when().post("/demo/drools/member-level")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.engineAvailable", equalTo(true))
                .body("data.memberLevel", notNullValue());

        given().when().get("/demo/drools/rule-version")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.coordinates", notNullValue())
                .body("data.engineValidated", equalTo(true));

        given().contentType("application/json")
                .when().post("/demo/job/run/daily-report")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", notNullValue());

        given().contentType("application/json")
                .when().post("/demo/job/run/idempotent")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.idempotentWorks", equalTo(true));

        given().when().get("/demo/job/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.handlerAvailable", equalTo(true));
    }
}
