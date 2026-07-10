package com.microservice.framework.observability.autoconfigure.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MdcAutoConfiguration 单元测试
 */
class MdcAutoConfigurationTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("MdcFilter: 注入 TraceId")
    void testMdcFilter_InjectsTraceId() {
        // 模拟注入 TraceId
        MDC.put("traceId", "test-trace-id-12345");

        String traceId = MDC.get("traceId");

        assertNotNull(traceId);
        assertEquals("test-trace-id-12345", traceId);
    }

    @Test
    @DisplayName("MdcFilter: 注入 SpanId")
    void testMdcFilter_InjectsSpanId() {
        // 模拟注入 SpanId
        MDC.put("spanId", "span-id-67890");

        String spanId = MDC.get("spanId");

        assertNotNull(spanId);
        assertEquals("span-id-67890", spanId);
    }

    @Test
    @DisplayName("MdcFilter: 注入 RequestId")
    void testMdcFilter_InjectsRequestId() {
        // 模拟注入 RequestId
        MDC.put("requestId", "req-uuid-abc123");

        String requestId = MDC.get("requestId");

        assertNotNull(requestId);
        assertEquals("req-uuid-abc123", requestId);
    }

    @Test
    @DisplayName("MdcFilter: 请求后清理")
    void testMdcFilter_CleansUpAfterRequest() {
        // 模拟请求期间的 MDC
        MDC.put("traceId", "trace-to-clean");
        MDC.put("spanId", "span-to-clean");
        MDC.put("requestId", "request-to-clean");

        // 模拟请求结束后清理
        MDC.clear();

        // 验证已清理
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("spanId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("MdcTracingObservationHandler: Observation 停止后清理 MDC")
    void observationHandlerShouldCleanMdcValues() {
        FakeTracer tracer = new FakeTracer(new FakeSpan("current-trace", "current-span"));
        MdcAutoConfiguration.MdcTracingObservationHandler handler =
                new MdcAutoConfiguration.MdcTracingObservationHandler(tracer);
        io.micrometer.observation.Observation.Context context =
                new io.micrometer.observation.Observation.Context();

        handler.onStart(context);
        assertEquals("current-trace", MDC.get("traceId"));
        assertEquals("current-span", MDC.get("spanId"));
        assertEquals("current-trace", MDC.get("requestId"));

        handler.onStop(context);
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("spanId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("只有 Tracer 而没有 Propagator 时应跳过 MDC 初始化")
    void tracerWithoutPropagatorShouldNotBreakApplicationStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MdcAutoConfiguration.class))
                .withBean(Tracer.class, () -> new FakeTracer(null))
                .run(context -> {
                    assertDoesNotThrow(() -> assertTrue(context.isRunning()));
                    assertFalse(context.containsBean("observabilityUtilsInitializer"));
                });
    }

    static class FakeTracer implements Tracer {

        private final Span currentSpan;

        FakeTracer(Span currentSpan) {
            this.currentSpan = currentSpan;
        }

        @Override public Span currentSpan() { return currentSpan; }
        @Override public Span nextSpan() { return null; }
        @Override public Span nextSpan(Span parent) { return null; }
        @Override public SpanInScope withSpan(Span span) { return () -> {}; }
        @Override public ScopedSpan startScopedSpan(String name) { return null; }
        @Override public Span.Builder spanBuilder() { return null; }
        @Override public TraceContext.Builder traceContextBuilder() { return null; }
        @Override public CurrentTraceContext currentTraceContext() { return null; }
        @Override public SpanCustomizer currentSpanCustomizer() { return null; }
        @Override public Map<String, String> getAllBaggage() { return new HashMap<>(); }
        @Override public Baggage getBaggage(String name) { return null; }
        @Override public Baggage getBaggage(TraceContext context, String name) { return null; }
        @Override public Baggage createBaggage(String name) { return null; }
        @Override public Baggage createBaggage(String name, String value) { return null; }
    }

    record FakeSpan(String traceId, String spanId) implements Span {
        @Override public TraceContext context() { return new FakeTraceContext(traceId, spanId); }
        @Override public boolean isNoop() { return false; }
        @Override public Span start() { return this; }
        @Override public Span name(String name) { return this; }
        @Override public Span event(String name) { return this; }
        @Override public Span event(String name, long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span tag(String key, String value) { return this; }
        @Override public Span error(Throwable throwable) { return this; }
        @Override public void end() {}
        @Override public void end(long timestamp, java.util.concurrent.TimeUnit unit) {}
        @Override public void abandon() {}
        @Override public Span remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span remoteIpAndPort(String ip, int port) { return this; }
    }

    record FakeTraceContext(String traceId, String spanId) implements TraceContext {
        @Override public String parentId() { return null; }
        @Override public Boolean sampled() { return true; }
    }
}
