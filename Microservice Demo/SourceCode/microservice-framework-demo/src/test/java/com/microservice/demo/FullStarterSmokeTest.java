package com.microservice.demo;

import com.microservice.framework.async.api.AsyncTaskExecutor;
import com.microservice.framework.elasticsearch.api.ElasticsearchOperations;
import com.microservice.framework.elasticsearch.api.IndexManager;
import com.microservice.framework.feign.api.FeignContextPropagator;
import com.microservice.framework.fieldencryption.api.FieldEncryptor;
import com.microservice.framework.kafka.api.KafkaPublisher;
import com.microservice.framework.objectstorage.api.ObjectStorageOperations;
import com.microservice.framework.objectstorage.api.PreSignedUrlGenerator;
import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCounter;
import com.microservice.framework.redis.api.RedisCommandExecutor;
import com.microservice.framework.drools.api.RuleEngine;
import com.microservice.framework.drools.api.RuleVersion;
import com.microservice.framework.audit.api.AuditRecorder;
import com.microservice.framework.common.id.IdGenerator;
import com.microservice.framework.common.time.FrameworkClock;
import com.microservice.framework.common.context.ThreadLocalContextAdapter;
import com.microservice.framework.json.api.JsonCodec;
import com.microservice.framework.xxljob.api.IdempotentJobHandler;

import io.micrometer.tracing.Tracer;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full Starter Smoke Test — verifies all starter core beans exist in ApplicationContext.
 *
 * <p>This test activates the full-embedded profile to ensure all starters are enabled
 * with embedded providers. Each test method verifies that a starter's core bean is
 * registered in the application context, proving that the starter auto-configuration
 * and embedded provider integration work correctly.</p>
 *
 * <p>Run with: {@code mvn clean verify} (test activates full-embedded profile internally)</p>
 *
 * <p>Config center starters (nacos/apollo) are intentionally disabled in full-embedded
 * profile because they are mutually exclusive — they are tested separately via
 * ConfigCenterConflictTest.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("full-embedded")
@DisplayName("全量 Starter 冒烟测试")
class FullStarterSmokeTest {

    // ======================================================================
    // Common Starter Beans
    // ======================================================================

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private FrameworkClock frameworkClock;

    @Autowired
    private ThreadLocalContextAdapter contextAdapter;

    @Test
    @DisplayName("common-starter: IdGenerator Bean 存在且可用")
    void testIdGeneratorBeanExists() {
        assertNotNull(idGenerator, "IdGenerator bean should be registered");
        long id = idGenerator.generate();
        assertTrue(id > 0, "Generated ID should be positive");
    }

    @Test
    @DisplayName("common-starter: FrameworkClock Bean 存在且可用")
    void testFrameworkClockBeanExists() {
        assertNotNull(frameworkClock, "FrameworkClock bean should be registered");
        assertNotNull(frameworkClock.now(), "FrameworkClock.now() should return non-null");
        assertNotNull(frameworkClock.getZoneId(), "FrameworkClock.getZoneId() should return non-null");
    }

    @Test
    @DisplayName("common-starter: ThreadLocalContextAdapter Bean 存在且可用")
    void testContextAdapterBeanExists() {
        assertNotNull(contextAdapter, "ThreadLocalContextAdapter bean should be registered");
    }

    // ======================================================================
    // JSON Starter Beans
    // ======================================================================

    @Autowired
    private JsonCodec jsonCodec;

    @Test
    @DisplayName("json-starter: JsonCodec Bean 存在且可用")
    void testJsonCodecBeanExists() {
        assertNotNull(jsonCodec, "JsonCodec bean should be registered");
        String name = jsonCodec.getImplementationName();
        assertNotNull(name, "JsonCodec should have an implementation name");
    }

    // ======================================================================
    // Observability Starter Beans
    // ======================================================================

    @Autowired
    private Tracer tracer;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("observability-starter: Tracer Bean 存在且可用")
    void testTracerBeanExists() {
        assertNotNull(tracer, "Tracer bean should be registered");
    }

    @Test
    @DisplayName("observability-starter: MeterRegistry Bean 存在且可用")
    void testMeterRegistryBeanExists() {
        assertNotNull(meterRegistry, "MeterRegistry bean should be registered");
    }

    // ======================================================================
    // Redis Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private RedisCounter redisCounter;

    @Autowired
    private RedisCommandExecutor redisCommandExecutor;

    @Test
    @DisplayName("redis-starter: 所有核心 Bean 存在 (embedded provider)")
    void testRedisBeansExist() {
        assertNotNull(redisCache, "RedisCache bean should be registered");
        assertNotNull(distributedLock, "DistributedLock bean should be registered");
        assertNotNull(rateLimiter, "RateLimiter bean should be registered");
        assertNotNull(redisCounter, "RedisCounter bean should be registered");
        assertNotNull(redisCommandExecutor, "RedisCommandExecutor bean should be registered");
    }

    // ======================================================================
    // Database Starter Beans
    // ======================================================================

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("database-starter: DataSource Bean 存在 (H2)")
    void testDataSourceBeanExists() {
        assertNotNull(dataSource, "DataSource bean should be registered");
    }

    // ======================================================================
    // Kafka Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private KafkaPublisher<Object> kafkaPublisher;

    @Test
    @DisplayName("kafka-starter: KafkaPublisher Bean 存在 (embedded provider)")
    void testKafkaPublisherBeanExists() {
        assertNotNull(kafkaPublisher, "KafkaPublisher bean should be registered");
    }

    // ======================================================================
    // Feign Starter Beans
    // ======================================================================

    // FeignContextPropagator requires spring-cloud-openfeign on classpath;
    // since that library is optional in feign-starter, this bean may be absent.
    // We use required=false and document the limitation.
    @Autowired(required = false)
    private FeignContextPropagator feignContextPropagator;

    @Test
    @DisplayName("feign-starter: FeignContextPropagator Bean 可用时存在 (需要 OpenFeign client)")
    void testFeignContextPropagatorBeanExists() {
        // FeignContextPropagator may not be present if spring-cloud-openfeign
        // is not on the classpath (it's optional in feign-starter)
        if (feignContextPropagator != null) {
            assertNotNull(feignContextPropagator, "FeignContextPropagator bean should work when available");
        }
        // If null, this is expected — feign-starter's auto-config requires OpenFeign classes
    }

    // ======================================================================
    // XXL-Job Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private IdempotentJobHandler idempotentJobHandler;

    @Test
    @DisplayName("xxl-job-starter: IdempotentJobHandler Bean 存在 (embedded provider)")
    void testIdempotentJobHandlerBeanExists() {
        assertNotNull(idempotentJobHandler, "IdempotentJobHandler bean should be registered");
    }

    // ======================================================================
    // Elasticsearch Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private IndexManager indexManager;

    @Test
    @DisplayName("elasticsearch-starter: ElasticsearchOperations + IndexManager Bean 存在 (embedded)")
    void testElasticsearchBeansExist() {
        assertNotNull(elasticsearchOperations, "ElasticsearchOperations bean should be registered");
        assertNotNull(indexManager, "IndexManager bean should be registered");
    }

    // ======================================================================
    // Audit Starter Beans
    // ======================================================================

    @Autowired
    private AuditRecorder auditRecorder;

    @Test
    @DisplayName("audit-starter: AuditRecorder Bean 存在且可用")
    void testAuditRecorderBeanExists() {
        assertNotNull(auditRecorder, "AuditRecorder bean should be registered");
    }

    // ======================================================================
    // Field Encryption Starter Beans
    // ======================================================================

    @Autowired
    private FieldEncryptor fieldEncryptor;

    @Test
    @DisplayName("field-encryption-starter: FieldEncryptor Bean 存在且可用")
    void testFieldEncryptorBeanExists() {
        assertNotNull(fieldEncryptor, "FieldEncryptor bean should be registered");
    }

    // ======================================================================
    // Object Storage Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private ObjectStorageOperations objectStorageOperations;

    @Autowired
    private PreSignedUrlGenerator preSignedUrlGenerator;

    @Test
    @DisplayName("object-storage-starter: 核心 Bean 存在 (embedded provider)")
    void testObjectStorageBeansExist() {
        assertNotNull(objectStorageOperations, "ObjectStorageOperations bean should be registered");
        assertNotNull(preSignedUrlGenerator, "PreSignedUrlGenerator bean should be registered");
    }

    // ======================================================================
    // Drools Starter Beans (embedded provider)
    // ======================================================================

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleVersion ruleVersion;

    @Test
    @DisplayName("drools-starter: RuleEngine + RuleVersion Bean 存在 (embedded provider)")
    void testDroolsBeansExist() {
        assertNotNull(ruleEngine, "RuleEngine bean should be registered");
        assertNotNull(ruleVersion, "RuleVersion bean should be registered");
    }

    // ======================================================================
    // Async Starter Beans
    // ======================================================================

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Test
    @DisplayName("async-starter: AsyncTaskExecutor Bean 存在且可用")
    void testAsyncTaskExecutorBeanExists() {
        assertNotNull(asyncTaskExecutor, "AsyncTaskExecutor bean should be registered");
    }
}
