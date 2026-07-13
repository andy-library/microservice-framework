package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Real local middleware acceptance test.
 *
 * <p>This suite is intentionally opt-in because it requires local MySQL, Redis,
 * Kafka and Elasticsearch. Run with:
 * {@code mvn verify -Preal-middleware-acceptance -Ddemo.real.middleware.acceptance=true}</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("full-middleware")
@EnabledIfSystemProperty(named = "demo.real.middleware.acceptance", matches = "true")
@DisplayName("真实中间件 Starter 实战验收")
class RealMiddlewareStarterAcceptanceTest {

    private static final String RUN_ID = UUID.randomUUID().toString().replace("-", "");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("基础强制链路：common/json/web/logging/observability/security/async 可用")
    void baselineStartersAreAvailable() {
        given().when().get("/demo/common/id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.type", equalTo("snowflake"));

        given().when().get("/demo/common/time")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.zoneId", equalTo("Asia/Shanghai"));

        given().when().get("/demo/json/provider")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.provider", notNullValue());

        given().when().get("/demo/web/success")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.framework", equalTo("working"));

        given().when().get("/api/demo/logging/structured")
                .then().statusCode(200)
                .body("message", notNullValue());

        given().when().get("/api/demo/tracing/current")
                .then().statusCode(200)
                .body("data.traceId", notNullValue())
                .body("data.spanId", notNullValue());

        given().when().get("/demo/security/public")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("public"));

        given().contentType("application/json")
                .when().post("/demo/async/run")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.completed", equalTo(true));
    }

    @Test
    @DisplayName("database-starter：真实 MySQL 持久化与事务回滚可用")
    void databaseStarterWorksWithMysql() {
        String orderNo = "MYSQL-" + RUN_ID;

        Integer id = given().contentType("application/json")
                .body("""
                        {"orderNo":"%s","amount":88.88,"status":"CREATED"}
                        """.formatted(orderNo))
                .when().post("/demo/database/order")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", greaterThan(0))
                .body("data.orderNo", equalTo(orderNo))
                .extract().path("data.id");

        given().when().get("/demo/database/order/{id}", id)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.orderNo", equalTo(orderNo));

        given().contentType("application/json")
                .when().post("/demo/database/transaction")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.method", containsString("TransactionTemplateFacade"))
                .body("data.verificationPassed", equalTo(true));

        String eventId = given().contentType("application/json")
                .body("""
                        {"aggregateType":"Order","aggregateId":"%s","eventType":"CREATED","payload":"{\\"orderNo\\":\\"%s\\"}"}
                        """.formatted(orderNo, orderNo))
                .when().post("/demo/database/outbox/publish")
                .then().log().ifValidationFails().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("publish"))
                .body("data.eventId", notNullValue())
                .extract().path("data.eventId");

        given().when().get("/demo/database/outbox/unpublished")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.count", greaterThan(0));

        given().contentType("application/json")
                .when().post("/demo/database/outbox/{eventId}/mark-published", eventId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("markPublished"))
                .body("data.published", equalTo(true));
    }

    @Test
    @DisplayName("redis-starter：真实 Redis 缓存、批量删除、锁、限流、计数器可用")
    void redisStarterWorksWithRealRedis() {
        String key = "acceptance:redis:" + RUN_ID;
        List<String> batchKeys = List.of(key, key + ":2");

        given().contentType("application/json")
                .body("{\"value\":\"redis-real-value\"}")
                .when().put("/demo/redis/cache/{key}", key)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("put"));

        given().when().get("/demo/redis/cache/{key}", key)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));

        given().contentType("application/json")
                .body(batchKeys)
                .when().post("/demo/redis/cache/batch-delete")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("batchDelete"));

        String patternPrefix = "acceptance:redis:pattern:" + RUN_ID;
        String patternKey1 = patternPrefix + ":1";
        String patternKey2 = patternPrefix + ":2";
        given().contentType("application/json")
                .body("{\"value\":\"pattern-value-1\"}")
                .when().put("/demo/redis/cache/{key}", patternKey1)
                .then().statusCode(200)
                .body("code", equalTo(0));
        given().contentType("application/json")
                .body("{\"value\":\"pattern-value-2\"}")
                .when().put("/demo/redis/cache/{key}", patternKey2)
                .then().statusCode(200)
                .body("code", equalTo(0));
        given().contentType("application/json")
                .body(Map.of("pattern", patternPrefix + ":*"))
                .when().post("/demo/redis/cache/pattern-delete")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("patternDelete"))
                .body("data.deleted", equalTo(2));
        given().when().get("/demo/redis/cache/{key}", patternKey1)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(false));
        given().when().get("/demo/redis/cache/{key}", patternKey2)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(false));

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
    }

    @Test
    @DisplayName("kafka-starter：真实 Kafka 生产者、消费者构建、DLQ/幂等基础能力可用")
    void kafkaStarterWorksWithRealKafka() {
        String topic = "microservice-framework-demo-topic-" + RUN_ID;
        String message = "real-kafka-" + RUN_ID;

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","message":"%s"}
                        """.formatted(topic, message))
                .when().post("/demo/kafka/send")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.topic", equalTo(topic))
                .body("data.status", not(equalTo("send-failed")))
                .body("data.recordMetadataClass", equalTo("RecordMetadata"));

        given().when().get("/demo/kafka/consumer/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaPublisherAvailable", equalTo(true))
                .body("data.kafkaConsumerBuilderAvailable", equalTo(true))
                .body("data.consumerBuilderInfo", hasKey("enable.auto.commit"));

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","groupId":"real-consumer-%s"}
                        """.formatted(topic, RUN_ID))
                .when().post("/demo/kafka/consume-one")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true))
                .body("data.value", equalTo(message));

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","key":"%s","value":"%s","reason":"acceptance failure"}
                        """.formatted(topic, "dlq-" + RUN_ID, message))
                .when().post("/demo/kafka/dlq/publish")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaAvailable", equalTo(true))
                .body("data.dlqTopic", equalTo(topic + ".DLT"));

        given().contentType("application/json")
                .body("""
                        {"topic":"%s","groupId":"real-dlq-consumer-%s"}
                        """.formatted(topic + ".DLT", RUN_ID))
                .when().post("/demo/kafka/consume-one")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true))
                .body("data.value.reason", equalTo("acceptance failure"));

        String aggregateId = "KAFKA-OUTBOX-" + RUN_ID;
        given().contentType("application/json")
                .body("""
                        {"aggregateType":"Order","aggregateId":"%s","eventType":"CREATED","payload":"{\\"orderNo\\":\\"%s\\"}"}
                        """.formatted(aggregateId, aggregateId))
                .when().post("/demo/database/outbox/publish")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.eventId", notNullValue());

        given().contentType("application/json")
                .body("""
                        {"topic":"%s"}
                        """.formatted(topic))
                .when().post("/demo/kafka/outbox/publish-pending")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("publishPendingOutbox"))
                .body("data.topic", equalTo(topic))
                .body("data.published", greaterThan(0));
    }

    @Test
    @DisplayName("elasticsearch-starter：真实 Elasticsearch 索引、查询、删除可用")
    void elasticsearchStarterWorksWithRealElasticsearch() {
        String orderId = "es-" + RUN_ID;

        given().contentType("application/json")
                .body("""
                        {"orderId":"%s","orderNo":"ES-%s","amount":66.66,"status":"CREATED"}
                        """.formatted(orderId, RUN_ID))
                .when().post("/demo/es/order/index")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.indexed", equalTo(true))
                .body("data.id", equalTo(orderId));

        given().when().get("/demo/es/order/{id}", orderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true));

        given().queryParam("field", "orderId").queryParam("value", orderId)
                .when().get("/demo/es/order/search")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", greaterThan(0));

        given().when().delete("/demo/es/order/{id}", orderId)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.deleted", equalTo(true));
    }

    @Test
    @DisplayName("扩展组件：feign/audit/encryption/storage/drools/xxl-job 功能路径可用")
    void extensionStartersAreAvailable() {
        given().when().get("/demo/feign/context-propagation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.headerCount", greaterThan(0));

        given().log().ifValidationFails().contentType("application/json")
                .when().post("/demo/audit/admin-action")
                .then().log().ifValidationFails().statusCode(200)
                .body("code", equalTo(0))
                .body("data.saved", equalTo(true));

        given().contentType("application/json")
                .body("{\"plaintext\":\"sensitive-real-acceptance\"}")
                .when().post("/demo/encryption/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.roundtripMatch", equalTo(true));

        String storageKey = "storage-" + RUN_ID;
        given().contentType("application/json")
                .body("""
                        {"key":"%s","content":"real acceptance storage content"}
                        """.formatted(storageKey))
                .when().post("/demo/storage/object")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("upload"));

        given().when().get("/demo/storage/object/{key}", storageKey)
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("download"));

        given().contentType("application/json")
                .body("{\"spendingAmount\":15000}")
                .when().post("/demo/drools/member-level")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.engineAvailable", equalTo(true));

        given().contentType("application/json")
                .when().post("/demo/job/run/daily-report")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.jobId", equalTo(1001));
    }
}
