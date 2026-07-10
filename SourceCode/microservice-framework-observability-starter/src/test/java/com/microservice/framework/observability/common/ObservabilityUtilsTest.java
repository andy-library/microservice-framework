package com.microservice.framework.observability.common;

import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单元测试：验证 ObservabilityUtils 的所有功能
 *
 * <p>使用手写 fake/stub 替代 Mockito mock，避免 ByteBuddy inline mock maker 初始化失败。
 * 所有 fake 严格匹配 micrometer-tracing 1.3.x API。
 */
class ObservabilityUtilsTest {

    private FakeTracer fakeTracer;
    private FakeBaggageManager fakeBaggageManager;

    @BeforeEach
    void setUp() {
        MDC.clear();
        fakeTracer = new FakeTracer();
        fakeBaggageManager = new FakeBaggageManager();
    }

    @AfterEach
    void tearDown() {
        ObservabilityUtils.init(null, null, null);
        MDC.clear();
    }

    // ==================== getTraceId 测试 ====================

    @Test
    @DisplayName("getTraceId: 正常获取 TraceId")
    void testGetTraceId_Success() {
        String expectedTraceId = "abc123def456789";
        fakeTracer.setTraceId(expectedTraceId);
        ObservabilityUtils.init(fakeTracer, null, null);

        String result = ObservabilityUtils.getTraceId();
        assertEquals(expectedTraceId, result);
    }

    @Test
    @DisplayName("getTraceId: tracer 为 null 时返回 null")
    void testGetTraceId_TracerNull() {
        ObservabilityUtils.init(null, null, null);

        String result = ObservabilityUtils.getTraceId();
        assertNull(result);
    }

    @Test
    @DisplayName("getTraceId: currentSpan 为 null 时返回 null")
    void testGetTraceId_CurrentSpanNull() {
        fakeTracer.setCurrentSpan(null);
        ObservabilityUtils.init(fakeTracer, null, null);

        String result = ObservabilityUtils.getTraceId();
        assertNull(result);
    }

    // ==================== getSpanId 测试 ====================

    @Test
    @DisplayName("getSpanId: 正常获取 SpanId")
    void testGetSpanId_Success() {
        String expectedSpanId = "span123";
        fakeTracer.setSpanId(expectedSpanId);
        ObservabilityUtils.init(fakeTracer, null, null);

        String result = ObservabilityUtils.getSpanId();
        assertEquals(expectedSpanId, result);
    }

    @Test
    @DisplayName("getSpanId: tracer 为 null 时返回 null")
    void testGetSpanId_TracerNull() {
        ObservabilityUtils.init(null, null, null);

        String result = ObservabilityUtils.getSpanId();
        assertNull(result);
    }

    @Test
    @DisplayName("getSpanId: currentSpan 为 null 时返回 null")
    void testGetSpanId_CurrentSpanNull() {
        fakeTracer.setCurrentSpan(null);
        ObservabilityUtils.init(fakeTracer, null, null);

        String result = ObservabilityUtils.getSpanId();
        assertNull(result);
    }

    // ==================== addBaggage 测试 ====================

    @Test
    @DisplayName("addBaggage: 正常添加 Baggage")
    void testAddBaggage_Success() {
        ObservabilityUtils.init(fakeTracer, null, fakeBaggageManager);

        ObservabilityUtils.addBaggage("userId", "user123");

        assertEquals("user123", fakeBaggageManager.getBaggageValue("userId"));
    }

    @Test
    @DisplayName("addBaggage: baggageManager 为 null 时不抛异常")
    void testAddBaggage_BaggageManagerNull() {
        ObservabilityUtils.init(fakeTracer, null, null);

        assertDoesNotThrow(() -> ObservabilityUtils.addBaggage("userId", "user123"));
    }

    // ==================== removeBaggage 测试 ====================

    @Test
    @DisplayName("removeBaggage: 正常移除 Baggage")
    void testRemoveBaggage_Success() {
        ObservabilityUtils.init(fakeTracer, null, fakeBaggageManager);

        ObservabilityUtils.addBaggage("userId", "user123");
        ObservabilityUtils.removeBaggage("userId");

        assertNull(fakeBaggageManager.getBaggageValue("userId"));
    }

    @Test
    @DisplayName("removeBaggage: baggageManager 为 null 时不抛异常")
    void testRemoveBaggage_BaggageManagerNull() {
        ObservabilityUtils.init(fakeTracer, null, null);

        assertDoesNotThrow(() -> ObservabilityUtils.removeBaggage("userId"));
    }

    // ==================== addCustomTag 测试 ====================

    @Test
    @DisplayName("addCustomTag: 正常添加自定义标签")
    void testAddCustomTag_Success() {
        ObservabilityUtils.init(fakeTracer, null, fakeBaggageManager);

        ObservabilityUtils.addCustomTag("orderId", "order123");

        assertEquals("order123", fakeTracer.getCurrentSpan().getTagValue("orderId"));
    }

    @Test
    @DisplayName("addCustomTag: tracer 为 null 时不抛异常")
    void testAddCustomTag_TracerNull() {
        ObservabilityUtils.init(null, null, null);

        assertDoesNotThrow(() -> ObservabilityUtils.addCustomTag("orderId", "order123"));
    }

    @Test
    @DisplayName("addCustomTag: currentSpan 为 null 时不抛异常")
    void testAddCustomTag_CurrentSpanNull() {
        fakeTracer.setCurrentSpan(null);
        ObservabilityUtils.init(fakeTracer, null, null);

        assertDoesNotThrow(() -> ObservabilityUtils.addCustomTag("orderId", "order123"));
    }

    // ==================== getMdcValue 测试 ====================

    @Test
    @DisplayName("getMdcValue: 正常获取 MDC 值")
    void testGetMdcValue_Success() {
        MDC.put("requestId", "req123");

        String result = ObservabilityUtils.getMdcValue("requestId");
        assertEquals("req123", result);
    }

    @Test
    @DisplayName("getMdcValue: key 不存在时返回 null")
    void testGetMdcValue_KeyNotExists() {
        String result = ObservabilityUtils.getMdcValue("nonExistent");
        assertNull(result);
    }

    // ==================== init 测试 ====================

    @Test
    @DisplayName("init: 初始化后工具类可正常工作")
    void testInit_WorksAfterInitialization() {
        String expectedTraceId = "traceInitTest";
        fakeTracer.setTraceId(expectedTraceId);

        ObservabilityUtils.init(fakeTracer, null, fakeBaggageManager);

        assertEquals(expectedTraceId, ObservabilityUtils.getTraceId());
    }

    // ======================================================================
    // Fake implementations — matching micrometer-tracing 1.3.x API
    // ======================================================================

    static class FakeTracer implements Tracer {

        private FakeSpan currentSpan;
        private String traceId;
        private String spanId;

        FakeTracer() {
            this.currentSpan = new FakeSpan(this);
        }

        void setCurrentSpan(Span span) {
            if (span instanceof FakeSpan) {
                this.currentSpan = (FakeSpan) span;
            } else if (span == null) {
                this.currentSpan = null;
            } else {
                this.currentSpan = new FakeSpan(this);
            }
        }

        void setTraceId(String traceId) { this.traceId = traceId; }
        void setSpanId(String spanId) { this.spanId = spanId; }
        FakeSpan getCurrentSpan() { return currentSpan; }

        @Override public Span currentSpan() { return currentSpan; }
        @Override public Span nextSpan() { return new FakeSpan(this); }
        @Override public Span nextSpan(Span parent) { return new FakeSpan(this); }
        @Override public SpanInScope withSpan(Span span) { return () -> {}; }
        @Override public ScopedSpan startScopedSpan(String name) { return null; }
        @Override public Span.Builder spanBuilder() { return new FakeSpanBuilder(this); }
        @Override public TraceContext.Builder traceContextBuilder() { return null; }
        @Override public io.micrometer.tracing.CurrentTraceContext currentTraceContext() { return null; }
        @Override public SpanCustomizer currentSpanCustomizer() { return null; }

        // BaggageManager methods (Tracer extends BaggageManager)
        @Override public Map<String, String> getAllBaggage() { return new HashMap<>(); }
        @Override public Baggage getBaggage(String name) { return null; }
        @Override public Baggage getBaggage(TraceContext context, String name) { return null; }
        @Override public Baggage createBaggage(String name) { return null; }
        @Override public Baggage createBaggage(String name, String value) { return null; }
    }

    static class FakeSpan implements Span {

        private final FakeTracer tracer;
        private final Map<String, String> tags = new HashMap<>();
        private Throwable recordedError;

        FakeSpan(FakeTracer tracer) {
            this.tracer = tracer;
        }

        String getTagValue(String key) { return tags.get(key); }
        Throwable getRecordedError() { return recordedError; }

        @Override public TraceContext context() {
            return new FakeTraceContext(tracer.traceId, tracer.spanId);
        }
        @Override public boolean isNoop() { return false; }
        @Override public Span start() { return this; }
        @Override public Span name(String name) { return this; }
        @Override public Span event(String name) { return this; }
        @Override public Span event(String name, long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span tag(String key, String value) { tags.put(key, value); return this; }
        @Override public Span error(Throwable throwable) { this.recordedError = throwable; return this; }
        @Override public void end() {}
        @Override public void end(long timestamp, java.util.concurrent.TimeUnit unit) {}
        @Override public void abandon() {}
        @Override public Span remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span remoteIpAndPort(String ip, int port) { return this; }
    }

    static class FakeSpanBuilder implements Span.Builder {
        private final FakeTracer tracer;
        private String name;

        FakeSpanBuilder(FakeTracer tracer) { this.tracer = tracer; }

        @Override public Span.Builder setParent(TraceContext context) { return this; }
        @Override public Span.Builder setNoParent() { return this; }
        @Override public Span.Builder name(String name) { this.name = name; return this; }
        @Override public Span.Builder event(String name) { return this; }
        @Override public Span.Builder tag(String key, String value) { return this; }
        @Override public Span.Builder error(Throwable throwable) { return this; }
        @Override public Span.Builder kind(Span.Kind kind) { return this; }
        @Override public Span.Builder remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span.Builder remoteIpAndPort(String ip, int port) { return this; }
        @Override public Span.Builder startTimestamp(long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span start() { FakeSpan span = new FakeSpan(tracer); return span; }
    }

    static class FakeTraceContext implements TraceContext {
        private final String traceId;
        private final String spanId;

        FakeTraceContext(String traceId, String spanId) {
            this.traceId = traceId;
            this.spanId = spanId;
        }

        @Override public String traceId() { return traceId; }
        @Override public String spanId() { return spanId; }
        @Override public String parentId() { return null; }
        @Override public Boolean sampled() { return true; }
    }

    static class FakeBaggageManager implements BaggageManager {
        private final Map<String, FakeBaggage> baggageMap = new HashMap<>();

        String getBaggageValue(String name) {
            FakeBaggage baggage = baggageMap.get(name);
            return baggage != null ? baggage.getValue() : null;
        }

        @Override public Map<String, String> getAllBaggage() { return new HashMap<>(); }
        @Override public Baggage createBaggage(String name) {
            return baggageMap.computeIfAbsent(name, k -> new FakeBaggage(k, null));
        }
        @Override public Baggage createBaggage(String name, String value) {
            FakeBaggage baggage = new FakeBaggage(name, value);
            baggageMap.put(name, baggage);
            return baggage;
        }
        @Override public Baggage getBaggage(String name) { return baggageMap.get(name); }
        @Override public Baggage getBaggage(TraceContext context, String name) { return baggageMap.get(name); }
    }

    static class FakeBaggage implements Baggage {
        private final String name;
        private String value;

        FakeBaggage(String name, String value) {
            this.name = name;
            this.value = value;
        }

        String getValue() { return value; }

        // Baggage methods
        @Override public Baggage set(String value) { this.value = value; return this; }
        @Override public Baggage set(TraceContext context, String value) { this.value = value; return this; }
        @Override public BaggageInScope makeCurrent() { return null; }

        // BaggageView methods (Baggage extends BaggageView)
        @Override public String name() { return name; }
        @Override public String get() { return value; }
        @Override public String get(TraceContext context) { return value; }
    }
}
