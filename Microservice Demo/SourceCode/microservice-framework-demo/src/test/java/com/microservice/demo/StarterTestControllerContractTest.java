package com.microservice.demo;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies the stable /test/{starter} API contract used for starter capability
 * acceptance and later performance testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("full-embedded")
@DisplayName("Starter 独立测试 Controller 契约验收")
class StarterTestControllerContractTest {

    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("common/json/web/logging/observability 基础能力均有独立测试入口")
    void baselineStarterTestControllersWork() {
        given().when().get("/test/common/id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.type", equalTo("snowflake"));

        given().when().get("/test/common/time")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.zoneId", equalTo("Asia/Shanghai"));

        given().when().get("/test/common/page")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.items", notNullValue());

        given().header("X-Request-ID", "starter-test-" + RUN_ID)
                .when().get("/test/common/context")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.hasContext", equalTo(true));

        given().when().get("/test/json/provider")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.provider", notNullValue());

        given().contentType("application/json")
                .body(Map.of("name", "starter-test", "value", 42, "nested", Map.of("enabled", true)))
                .when().post("/test/json/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.match", equalTo(true));

        given().when().get("/test/web/success")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.framework", equalTo("working"));

        given().when().get("/test/web/business-error")
                .then().statusCode(400)
                .body("code", not(equalTo(0)))
                .body("message", containsString("demo business error"));

        given().contentType("application/json")
                .body(Map.of("name", "starter", "age", 18))
                .when().post("/test/web/validation-error")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.name", equalTo("starter"));

        given().when().get("/test/web/request-id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.requestId", not(equalTo("not-set")));

        given().when().get("/test/logging/structured")
                .then().statusCode(200)
                .body("message", containsString("structured"));
        given().when().get("/test/logging/masking")
                .then().statusCode(200)
                .body("message", containsString("masked"));
        given().when().get("/test/logging/exception")
                .then().statusCode(200)
                .body("message", containsString("Exception logged"));
        given().when().get("/test/logging/levels")
                .then().statusCode(200)
                .body("message", containsString("different log levels"));
        given().when().get("/test/logging/flood")
                .then().statusCode(200)
                .body("totalAttempted", equalTo(1000));

        given().when().get("/test/observability/tracing/current")
                .then().statusCode(200)
                .body("traceId", notNullValue());
        given().header("user-id", "user-" + RUN_ID).header("tenant-id", "tenant-demo")
                .when().get("/test/observability/tracing/baggage")
                .then().statusCode(200)
                .body("userId", equalTo("user-" + RUN_ID))
                .body("tenantId", equalTo("tenant-demo"));
        given().when().get("/test/observability/tracing/sampling")
                .then().statusCode(200)
                .body("message", notNullValue());
        given().queryParam("value", "tag-" + RUN_ID)
                .when().get("/test/observability/tracing/tag")
                .then().statusCode(200)
                .body(equalTo("Tag added: tag-" + RUN_ID));
        given().queryParam("channel", "starter")
                .when().get("/test/observability/metrics/counter")
                .then().statusCode(200)
                .body("metric", equalTo("business.order.created"))
                .body("tags.channel", equalTo("starter"));
        given().when().get("/test/observability/metrics/timer")
                .then().statusCode(200)
                .body("actualDurationMs", greaterThanOrEqualTo(0));
        given().when().get("/test/observability/health/custom")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
        given().when().get("/test/observability/health/endpoints")
                .then().statusCode(200)
                .body("prometheus", equalTo("/actuator/prometheus"));
        given().when().get("/test/observability/request-id")
                .then().statusCode(200)
                .body("requestId", notNullValue());
        given().header("X-Request-ID", "gateway-" + RUN_ID)
                .when().get("/test/observability/request-id/gateway-test")
                .then().statusCode(200)
                .body("mdc_request_id", notNullValue());
        given().when().get("/test/observability/request-id/full-context")
                .then().statusCode(200)
                .body("mdc", notNullValue());
    }

    @Test
    @DisplayName("database/redis/kafka/elasticsearch 中间件能力均有独立测试入口")
    void middlewareStarterTestControllersWork() {
        String orderNo = "TEST-DB-" + RUN_ID;
        Integer orderId = given().contentType("application/json")
                .body(Map.of("orderNo", orderNo, "amount", 88.8, "status", "CREATED"))
                .when().post("/test/database/order")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("create"))
                .extract().path("data.id");

        given().when().get("/test/database/order/{id}", orderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.orderNo", equalTo(orderNo));
        given().contentType("application/json")
                .when().post("/test/database/transaction")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.verificationPassed", equalTo(true));
        given().when().get("/test/database/routing/current")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.routingHint", equalTo("PRIMARY"));

        String eventId = given().contentType("application/json")
                .body(Map.of("aggregateType", "Order", "aggregateId", orderNo, "eventType", "CREATED", "payload", "{}"))
                .when().post("/test/database/outbox/publish")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.eventId", notNullValue())
                .extract().path("data.eventId");
        given().when().get("/test/database/outbox/unpublished")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.count", greaterThanOrEqualTo(0));
        given().contentType("application/json")
                .when().post("/test/database/outbox/{eventId}/mark-published", eventId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.published", equalTo(true));

        String cacheKey = "starter:test:redis:" + RUN_ID;
        given().contentType("application/json")
                .body(Map.of("value", "redis-value"))
                .when().put("/test/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("put"));
        given().when().get("/test/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));
        given().contentType("application/json")
                .body(List.of(cacheKey, cacheKey + ":2"))
                .when().post("/test/redis/cache/batch-delete")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("batchDelete"));
        String patternPrefix = "starter:test:redis:pattern:" + RUN_ID;
        given().contentType("application/json")
                .body(Map.of("value", "pattern-1"))
                .when().put("/test/redis/cache/{key}", patternPrefix + ":1")
                .then().statusCode(200)
                .body("code", equalTo(0));
        given().contentType("application/json")
                .body(Map.of("value", "pattern-2"))
                .when().put("/test/redis/cache/{key}", patternPrefix + ":2")
                .then().statusCode(200)
                .body("code", equalTo(0));
        given().contentType("application/json")
                .body(Map.of("pattern", patternPrefix + ":*"))
                .when().post("/test/redis/cache/pattern-delete")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("patternDelete"))
                .body("data.deleted", equalTo(2));
        given().contentType("application/json")
                .when().post("/test/redis/lock")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.locked", equalTo(true));
        given().when().get("/test/redis/rate-limit")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.acquired", equalTo(true));
        given().contentType("application/json")
                .when().post("/test/redis/counter/increment")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.newValue", greaterThanOrEqualTo(1));
        given().when().delete("/test/redis/cache/{key}", cacheKey)
                .then().statusCode(200)
                .body("code", equalTo(0));

        String topic = "starter-test-topic-" + RUN_ID;
        given().contentType("application/json")
                .body(Map.of("topic", topic, "message", "sync-message"))
                .when().post("/test/kafka/send")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.topic", equalTo(topic));
        given().contentType("application/json")
                .body(Map.of("topic", topic, "message", "async-message"))
                .when().post("/test/kafka/send-async")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("async-sent"));
        given().when().get("/test/kafka/consumer/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaConsumerBuilderAvailable", equalTo(true));
        given().contentType("application/json")
                .body(Map.of("topic", topic, "groupId", "starter-consumer-" + RUN_ID))
                .when().post("/test/kafka/consume-one")
                .then().statusCode(200)
                .body("code", equalTo(0));
        given().contentType("application/json")
                .body(Map.of("topic", topic, "key", "dlq-" + RUN_ID, "value", "dead-letter", "reason", "starter-test"))
                .when().post("/test/kafka/dlq/publish")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.dlqTopic", equalTo(topic + ".DLT"));
        given().contentType("application/json")
                .when().post("/test/kafka/dlq/replay")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("notional"));
        given().contentType("application/json")
                .body(Map.of("topic", topic))
                .when().post("/test/kafka/outbox/publish-pending")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("publishPendingOutbox"));

        String esId = "starter-es-" + RUN_ID;
        given().contentType("application/json")
                .body(Map.of("orderId", esId, "orderNo", "ES-" + RUN_ID, "amount", 66.6, "status", "CREATED"))
                .when().post("/test/elasticsearch/order/index")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.indexed", equalTo(true));
        given().when().get("/test/elasticsearch/order/{id}", esId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));
        given().when().get("/test/elasticsearch/order/search?status=CREATED")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("search"));
        given().when().get("/test/elasticsearch/order/search-page?status=CREATED&from=0&size=5")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("searchPage"));
        given().contentType("application/json")
                .body(List.of(Map.of("orderId", esId + "-bulk", "status", "CREATED"), Map.of("status", "INVALID")))
                .when().post("/test/elasticsearch/order/bulk")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("bulkIndex"));
        given().contentType("application/json")
                .body(Map.of("indexName", "demo-orders", "aliasName", "demo-orders-current-" + RUN_ID))
                .when().post("/test/elasticsearch/alias/create")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("createAlias"));
        given().when().delete("/test/elasticsearch/order/{id}", esId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.deleted", equalTo(true));
    }

    @Test
    @DisplayName("security/feign/async/audit/encryption/storage/drools/xxl-job/config 扩展能力均有独立测试入口")
    void extensionStarterTestControllersWork() throws Exception {
        given().when().get("/test/security/public")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("public"));
        given().when().get("/test/security/user")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("user"));
        given().when().get("/test/security/internal")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("internal"));

        given().when().get("/test/feign/context-propagation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.headerCount", greaterThanOrEqualTo(1));
        given().when().get("/test/feign/timeout-policy")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.propagatorAvailable", equalTo(true));
        given().header("user-id", "feign-user-" + RUN_ID)
                .when().get("/test/feign/real-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.downstream", notNullValue());
        given().queryParam("delayMs", 10)
                .when().get("/test/feign/timeout-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.timedOut", equalTo(false));
        given().when().get("/test/feign/connection-pool")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.maxTotal", greaterThanOrEqualTo(1));
        given().queryParam("failureCount", 0)
                .when().get("/test/feign/retry-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.recovered", equalTo(true));
        given().queryParam("failureCount", 1)
                .when().post("/test/feign/non-idempotent-retry-call")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.retried", equalTo(false));

        given().contentType("application/json")
                .when().post("/test/async/run")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.completed", equalTo(true));
        given().when().get("/test/async/context")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.propagationWorks", equalTo(true));
        given().when().get("/test/async/metrics")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.poolSize", greaterThanOrEqualTo(1));

        Response auditResponse = given().contentType("application/json")
                .when().post("/test/audit/admin-action")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.saved", equalTo(true))
                .extract().response();
        String entryId = auditResponse.path("data.entryId");
        given().when().get("/test/audit/{entryId}", entryId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));
        given().when().get("/test/audit/latest")
                .then().statusCode(200)
                .body("code", equalTo(0));

        String encrypted = given().contentType("application/json")
                .body(Map.of("plaintext", "sensitive-" + RUN_ID))
                .when().post("/test/field-encryption/encrypt")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.isEncryptedAfterEncrypt", equalTo(true))
                .extract().path("data.ciphertext");
        given().contentType("application/json")
                .body(Map.of("ciphertext", encrypted))
                .when().post("/test/field-encryption/decrypt")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.decryptedText", equalTo("sensitive-" + RUN_ID));
        given().contentType("application/json")
                .body(Map.of("plaintext", "roundtrip-" + RUN_ID))
                .when().post("/test/field-encryption/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.roundtripMatch", equalTo(true));

        String objectKey = "starter-object-" + RUN_ID;
        given().contentType("application/json")
                .body(Map.of("key", objectKey, "content", "object-content"))
                .when().post("/test/object-storage/object")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("upload"));
        given().when().get("/test/object-storage/object/{key}", objectKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("download"));
        given().when().get("/test/object-storage/object/{key}/presigned-url", objectKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.implementation", notNullValue());
        given().when().delete("/test/object-storage/object/{key}", objectKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("delete"));

        given().contentType("application/json")
                .body(Map.of("spendingAmount", 15000))
                .when().post("/test/drools/member-level")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.engineAvailable", equalTo(true));
        given().when().get("/test/drools/rule-version")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.coordinates", notNullValue());

        given().contentType("application/json")
                .when().post("/test/xxl-job/run/daily-report")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.jobId", equalTo(1001));
        given().contentType("application/json")
                .when().post("/test/xxl-job/run/idempotent")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.idempotentWorks", equalTo(true));
        given().when().get("/test/xxl-job/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.handlerAvailable", equalTo(true));

        given().when().get("/test/nacos/source")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("activeConfigSource"));
        given().when().get("/test/nacos/masked")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("comparison"));
        given().when().get("/test/nacos/validation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("sensitivityCheck"));
        given().when().get("/test/apollo/source")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("activeConfigSource"));
        given().when().get("/test/apollo/masked")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("comparison"));
        given().when().get("/test/apollo/validation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasKey("sensitivityCheck"));
    }
}
