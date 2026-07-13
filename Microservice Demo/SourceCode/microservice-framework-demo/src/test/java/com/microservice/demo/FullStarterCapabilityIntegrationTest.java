package com.microservice.demo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Full Starter Capability Integration Test — verifies all demo endpoints
 * work correctly with real starter beans (not simulated/503 fallbacks).
 *
 * <p>This test activates the full-embedded profile where all starters use
 * embedded in-memory providers. The key rule: NO endpoint should return
 * code=503 (service unavailable). All endpoints must return code=0 (success)
 * and must have meaningful business field assertions.</p>
 *
 * <p>Run with: {@code mvn clean verify} (test activates full-embedded profile internally)</p>
 *
 * <p>Failure path tests verify that error responses still use real starter
 * infrastructure (not simulated fallbacks) and return appropriate error codes.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("full-embedded")
@DisplayName("全量 Starter 能力集成测试")
@Execution(ExecutionMode.SAME_THREAD)
class FullStarterCapabilityIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    // ======================================================================
    // Common Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("common-starter 能力验证")
    class CommonStarterTests {

        @Test
        @DisplayName("GET /demo/common/id — ID生成器可用，返回 snowflake 类型")
        void testCommonId() {
            given()
                .when().get("/demo/common/id")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.type", equalTo("snowflake"))
                .body("data.source", equalTo("IdGenerator"));
        }

        @Test
        @DisplayName("GET /demo/common/time — FrameworkClock 可用，返回时区信息")
        void testCommonTime() {
            given()
                .when().get("/demo/common/time")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.time", notNullValue())
                .body("data.zoneId", equalTo("Asia/Shanghai"))
                .body("data.source", equalTo("FrameworkClock"));
        }
    }

    // ======================================================================
    // JSON Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("json-starter 能力验证")
    class JsonStarterTests {

        @Test
        @DisplayName("POST /demo/json/roundtrip — JSON 序列化/反序列化 roundtrip 匹配")
        void testJsonRoundtrip() {
            given()
                .contentType("application/json")
                .body("{\"name\":\"test\",\"value\":42}")
                .when().post("/demo/json/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.roundtrip", notNullValue())
                .body("data.match", equalTo(true))
                .body("data.codec", notNullValue())
                .body("data.serialized", containsString("\"name\""));
        }
    }

    // ======================================================================
    // Web Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("web-starter 能力验证")
    class WebStarterTests {

        @Test
        @DisplayName("GET /demo/web/success — ApiResponse 包装正常响应")
        void testWebSuccess() {
            given()
                .when().get("/demo/web/success")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.message", notNullValue())
                .body("data.framework", equalTo("working"));
        }

        @Test
        @DisplayName("GET /demo/web/business-error — BusinessException 返回非零 code")
        void testWebBusinessError() {
            given()
                .when().get("/demo/web/business-error")
                .then().statusCode(400)
                .body("code", not(equalTo(0)))
                .body("message", containsString("demo business error"));
        }
    }

    // ======================================================================
    // Security Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("security-starter 能力验证")
    class SecurityStarterTests {

        @Test
        @DisplayName("GET /demo/security/public — 公开端点无需认证")
        void testSecurityPublic() {
            given()
                .when().get("/demo/security/public")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("public"));
        }

        @Test
        @DisplayName("GET /demo/security/user — permit-all profile 下端点应稳定返回用户上下文")
        void testSecurityUnauthorized() {
            given()
                .when().get("/demo/security/user")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.access", equalTo("user"));
        }
    }

    // ======================================================================
    // Logging Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("logging-starter 能力验证")
    class LoggingStarterTests {

        @Test
        @DisplayName("GET /api/demo/logging/structured — 结构化日志端点响应正常")
        void testLogStructured() {
            given()
                .when().get("/api/demo/logging/structured")
                .then().statusCode(200)
                .body("data.message", containsString("structured"));
        }
    }

    // ======================================================================
    // Observability Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("observability-starter 能力验证")
    class ObservabilityStarterTests {

        @Test
        @DisplayName("GET /api/demo/tracing/current — Tracer Bean 可用，返回 traceId")
        void testTracingCurrent() {
            given()
                .when().get("/api/demo/tracing/current")
                .then().statusCode(200)
                .body("data.traceId", notNullValue());
        }
    }

    // ======================================================================
    // Redis Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("redis-starter 能力验证 (embedded provider)")
    class RedisStarterTests {

        @Test
        @DisplayName("PUT + GET /demo/redis/cache/{key} — 缓存读写可用")
        void testRedisCachePutAndGet() {
            given()
                .contentType("application/json")
                .body("{\"value\":\"test-value\"}")
                .when().put("/demo/redis/cache/test-cap-key")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("put"));

            given()
                .when().get("/demo/redis/cache/test-cap-key")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(true))
                .body("data.value.value", equalTo("test-value"));
        }

        @Test
        @DisplayName("GET /demo/redis/cache/nonexistent — 缓存未命中，仍返回 code=0")
        void testRedisCacheMiss() {
            given()
                .when().get("/demo/redis/cache/nonexistent-key-" + System.currentTimeMillis())
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(false));
        }

        @Test
        @DisplayName("POST /demo/redis/lock — 分布式锁可用，加锁解锁成功")
        void testRedisLock() {
            given()
                .contentType("application/json")
                .when().post("/demo/redis/lock")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.locked", equalTo(true))
                .body("data.unlocked", equalTo(true));
        }
    }

    // ======================================================================
    // Database Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("database-starter 能力验证")
    class DatabaseStarterTests {

        @Test
        @DisplayName("POST /demo/database/order — JPA 持久化可用，订单创建成功")
        void testDatabaseOrderCreate() {
            given()
                .contentType("application/json")
                .body("{\"orderNo\":\"TEST-001\",\"amount\":99.9,\"status\":\"CREATED\"}")
                .when().post("/demo/database/order")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", greaterThan(0))
                .body("data.orderNo", equalTo("TEST-001"))
                .body("data.operation", equalTo("create"));
        }

        @Test
        @DisplayName("POST /demo/database/transaction — 事务回滚验证")
        void testDatabaseTransactionRollback() {
            given()
                .contentType("application/json")
                .body("{\"rollbackOrderNo\":\"ROLLBACK-001\"}")
                .when().post("/demo/database/transaction")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.rolledBack", equalTo(true));
        }
    }

    // ======================================================================
    // Kafka Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("kafka-starter 能力验证 (embedded provider)")
    class KafkaStarterTests {

        @Test
        @DisplayName("POST /demo/kafka/send-async — embedded Kafka 发布能力成功")
        void testKafkaSendAsync() {
            given()
                .contentType("application/json")
                .body("{\"topic\":\"embedded-capability-topic\",\"message\":\"embedded-message\"}")
                .when().post("/demo/kafka/send-async")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.topic", equalTo("embedded-capability-topic"))
                .body("data.status", equalTo("async-sent"));
        }

        @Test
        @DisplayName("GET /demo/kafka/consumer/status — embedded Kafka 消费治理组件可用")
        void testKafkaConsumerStatus() {
            given()
                .when().get("/demo/kafka/consumer/status")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.kafkaPublisherAvailable", equalTo(true))
                .body("data.kafkaConsumerBuilderAvailable", equalTo(true));
        }
    }

    // ======================================================================
    // Feign Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("feign-starter 能力验证")
    class FeignStarterTests {

        @Test
        @DisplayName("GET /demo/feign/context-propagation — FeignContextPropagator 必须可用")
        void testFeignContextPropagation() {
            given()
                .when().get("/demo/feign/context-propagation")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.headerCount", greaterThan(0))
                .body("data.propagateKeys", notNullValue());
        }
    }

    // ======================================================================
    // XXL-Job Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("xxl-job-starter 能力验证 (embedded provider)")
    class XxlJobStarterTests {

        @Test
        @DisplayName("POST /demo/job/run/daily-report — IdempotentJobHandler 可用，非 503")
        void testJobRunDailyReport() {
            given()
                .contentType("application/json")
                .when().post("/demo/job/run/daily-report")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.jobId", equalTo(1001))
                .body("data.isDuplicate", notNullValue());
        }
    }

    // ======================================================================
    // Elasticsearch Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("elasticsearch-starter 能力验证 (embedded provider)")
    class ElasticsearchStarterTests {

        @Test
        @DisplayName("POST /demo/es/order/index — 文档索引可用，非 503")
        void testEsIndexOrder() {
            given()
                .contentType("application/json")
                .body("{\"orderId\":\"es-test-1\",\"orderNo\":\"ES-001\",\"amount\":100.0,\"status\":\"CREATED\"}")
                .when().post("/demo/es/order/index")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.indexed", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.operation", equalTo("index"));
        }

        @Test
        @DisplayName("GET /demo/es/order/search — 搜索端点可用，非 503")
        void testEsSearchOrders() {
            // First index a document to ensure search returns results
            given()
                .contentType("application/json")
                .body("{\"orderId\":\"es-search-test\",\"orderNo\":\"ES-SEARCH\",\"amount\":50.0,\"status\":\"CREATED\"}")
                .when().post("/demo/es/order/index")
                .then().statusCode(200)
                .body("code", equalTo(0));

            // Then search
            given()
                .when().get("/demo/es/order/search")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.results", notNullValue())
                .body("data.operation", equalTo("search"));
        }
    }

    // ======================================================================
    // Audit Starter Endpoints (NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("audit-starter 能力验证")
    class AuditStarterTests {

        @Test
        @DisplayName("POST /demo/audit/admin-action — AuditRecorder 可用，非 503")
        void testAuditAdminAction() {
            given()
                .contentType("application/json")
                .when().post("/demo/audit/admin-action")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.saved", equalTo(true))
                .body("data.eventType", equalTo("ADMIN_ACTION"))
                .body("data.checksumVerified", equalTo(true));

            given()
                .when().get("/demo/audit/{entryId}", "missing-audit-entry")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.found", equalTo(false));
        }
    }

    // ======================================================================
    // Field Encryption Starter Endpoints (NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("field-encryption-starter 能力验证")
    class FieldEncryptionStarterTests {

        @Test
        @DisplayName("POST /demo/encryption/roundtrip — FieldEncryptor 可用，roundtrip 匹配")
        void testEncryptionRoundtrip() {
            given()
                .contentType("application/json")
                .body("{\"plaintext\":\"sensitive-data-test\"}")
                .when().post("/demo/encryption/roundtrip")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.roundtripMatch", equalTo(true))
                .body("data.verificationPassed", equalTo(true))
                .body("data.isEncryptedAfterEncrypt", equalTo(true))
                .body("data.ciphertext", not(equalTo("c2Vuc2l0aXZlLWRhdGEtdGVzdA==")));
        }
    }

    // ======================================================================
    // Object Storage Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("object-storage-starter 能力验证 (embedded provider)")
    class ObjectStorageStarterTests {

        @Test
        @DisplayName("POST + GET /demo/storage/object — 对象存储读写可用，非 503")
        void testStorageObjectUploadAndDownload() {
            // Upload
            given()
                .contentType("application/json")
                .body("{\"key\":\"test-obj-key\",\"content\":\"test-content-value\"}")
                .when().post("/demo/storage/object")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.key", notNullValue())
                .body("data.operation", equalTo("upload"));

            // Download
            given()
                .when().get("/demo/storage/object/test-obj-key")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.operation", equalTo("download"))
                .body("data.found", equalTo(true))
                .body("data.content", equalTo("test-content-value"))
                .body("data.implementation", notNullValue());
        }
    }

    // ======================================================================
    // Drools Starter Endpoints (embedded provider — NO 503!)
    // ======================================================================

    @Nested
    @DisplayName("drools-starter 能力验证 (embedded provider)")
    class DroolsStarterTests {

        @Test
        @DisplayName("POST /demo/drools/member-level — RuleEngine 可用，engineAvailable=true")
        void testDroolsMemberLevel() {
            given()
                .contentType("application/json")
                .body("{\"spendingAmount\":15000}")
                .when().post("/demo/drools/member-level")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.engineAvailable", equalTo(true))
                .body("data.memberLevel", equalTo("DIAMOND"))
                .body("data.firedRules", notNullValue());
        }
    }

    // ======================================================================
    // Async Starter Endpoints
    // ======================================================================

    @Nested
    @DisplayName("async-starter 能力验证")
    class AsyncStarterTests {

        @Test
        @DisplayName("POST /demo/async/run — 异步任务执行完成，context 传播可用")
        void testAsyncRun() {
            given()
                .contentType("application/json")
                .when().post("/demo/async/run")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.completed", equalTo(true))
                .body("data.taskResult", notNullValue());
        }
    }

    // ======================================================================
    // Performance Endpoints (real beans, no simulated)
    // ======================================================================

    @Nested
    @DisplayName("性能端点能力验证 (使用真实 Bean)")
    class PerformanceEndpointTests {

        @Test
        @DisplayName("GET /perf/cache-hit — 使用真实 RedisCache，source 非 simulated")
        void testPerfCacheHitUsesRealCache() {
            given()
                .when().get("/perf/cache-hit")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.source", not(equalTo("simulated")))
                .body("data.hit", notNullValue());
        }

        @Test
        @DisplayName("POST /perf/full-chain-async — 使用真实 Bean，traceId 来自 Tracer")
        void testPerfFullChainUsesRealBeans() {
            given()
                .contentType("application/json")
                .when().post("/perf/full-chain-async")
                .then().statusCode(200)
                .body("code", equalTo(0))
                .body("data.cacheLookup", not(equalTo("simulated-hit")))
                .body("data.asyncTaskSubmitted", equalTo(true))
                .body("data.traceId", notNullValue());
        }
    }
}
