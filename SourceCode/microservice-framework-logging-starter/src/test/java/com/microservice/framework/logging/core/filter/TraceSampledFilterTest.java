package com.microservice.framework.logging.core.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceSampledFilter 单元测试
 * <p>
 * 使用手写 fake/stub 替代 Mockito mock，避免 ByteBuddy inline mock maker 初始化失败。
 * 所有 fake 严格匹配 micrometer-tracing 1.3.x API。
 */
class TraceSampledFilterTest {

    private TraceSampledFilter filter;
    private FakeTracer fakeTracer;

    @BeforeEach
    void setUp() {
        fakeTracer = new FakeTracer();
        filter = new TraceSampledFilter(fakeTracer);
    }

    @Test
    @DisplayName("decide: tracer 为 null 时返回 NEUTRAL")
    void testDecide_TracerNull() {
        filter = new TraceSampledFilter(null);

        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    @DisplayName("decide: currentSpan 为 null 时返回 NEUTRAL")
    void testDecide_CurrentSpanNull() {
        fakeTracer.setCurrentSpan(null);

        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
        assertTrue(fakeTracer.isCurrentSpanCalled());
    }

    @Test
    @DisplayName("decide: sampled span 返回 NEUTRAL")
    void testDecide_SampledSpan() {
        fakeTracer.setCurrentSpan(new FakeSpan(true));

        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    @DisplayName("decide: non-sampled span 的阈值级别返回 NEUTRAL")
    void testDecide_NonSampledSpanAtThreshold() {
        fakeTracer.setCurrentSpan(new FakeSpan(false));

        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    @DisplayName("decide: non-sampled span 低于阈值返回 DENY")
    void testDecide_NonSampledSpanBelowThreshold() {
        fakeTracer.setCurrentSpan(new FakeSpan(false));

        FilterReply reply = filter.decide(null, null, Level.DEBUG, "test", null, null);

        assertEquals(FilterReply.DENY, reply);
    }

    @Test
    @DisplayName("decide: non-sampled span 高于阈值返回 NEUTRAL")
    void testDecide_NonSampledSpanAboveThreshold() {
        fakeTracer.setCurrentSpan(new FakeSpan(false));

        FilterReply reply = filter.decide(null, null, Level.WARN, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
    }

    @Test
    @DisplayName("decide: 禁用过滤器时返回 NEUTRAL 且不访问 tracer")
    void testDecide_Disabled() {
        filter.setEnabled(false);

        FilterReply reply = filter.decide(null, null, Level.DEBUG, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
        assertFalse(fakeTracer.isCurrentSpanCalled());
    }

    @Test
    @DisplayName("decide: 自定义未采样阈值生效")
    void testDecide_CustomUnsampledThreshold() {
        filter.setLevelForUnsampled("WARN");
        fakeTracer.setCurrentSpan(new FakeSpan(false));

        assertEquals(FilterReply.DENY,
                filter.decide(null, null, Level.INFO, "test", null, null));
        assertEquals(FilterReply.NEUTRAL,
                filter.decide(null, null, Level.WARN, "test", null, null));
    }

    @Test
    @DisplayName("构造器注入 tracer 成功")
    void testConstructorTracer() {
        fakeTracer.setCurrentSpan(null);

        FilterReply reply = filter.decide(null, null, Level.INFO, "test", null, null);

        assertEquals(FilterReply.NEUTRAL, reply);
    }

    // ======================================================================
    // Fake implementations — matching micrometer-tracing 1.3.x API
    // ======================================================================

    static class FakeTracer implements Tracer {

        private Span currentSpan;
        private boolean currentSpanCalled = false;

        void setCurrentSpan(Span span) { this.currentSpan = span; }
        boolean isCurrentSpanCalled() { return currentSpanCalled; }

        @Override public Span currentSpan() { currentSpanCalled = true; return currentSpan; }
        @Override public Span nextSpan() { return new FakeSpan(true); }
        @Override public Span nextSpan(Span parent) { return new FakeSpan(true); }
        @Override public SpanInScope withSpan(Span span) { return () -> {}; }
        @Override public ScopedSpan startScopedSpan(String name) { return null; }
        @Override public Span.Builder spanBuilder() { return new FakeSpanBuilder(); }
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

        private final boolean sampled;
        private final java.util.Map<String, String> tags = new java.util.HashMap<>();

        FakeSpan(boolean sampled) {
            this.sampled = sampled;
        }

        @Override public TraceContext context() { return new FakeTraceContext(sampled); }
        @Override public boolean isNoop() { return false; }
        @Override public Span start() { return this; }
        @Override public Span name(String name) { return this; }
        @Override public Span event(String name) { return this; }
        @Override public Span event(String name, long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span tag(String key, String value) { tags.put(key, value); return this; }
        @Override public Span error(Throwable throwable) { return this; }
        @Override public void end() {}
        @Override public void end(long timestamp, java.util.concurrent.TimeUnit unit) {}
        @Override public void abandon() {}
        @Override public Span remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span remoteIpAndPort(String ip, int port) { return this; }
    }

    static class FakeTraceContext implements TraceContext {

        private final boolean sampled;

        FakeTraceContext(boolean sampled) {
            this.sampled = sampled;
        }

        @Override public String traceId() { return "fakeTraceId"; }
        @Override public String spanId() { return "fakeSpanId"; }
        @Override public String parentId() { return null; }
        @Override public Boolean sampled() { return sampled; }
    }

    static class FakeSpanBuilder implements Span.Builder {
        @Override public Span.Builder setParent(TraceContext context) { return this; }
        @Override public Span.Builder setNoParent() { return this; }
        @Override public Span.Builder name(String name) { return this; }
        @Override public Span.Builder event(String name) { return this; }
        @Override public Span.Builder tag(String key, String value) { return this; }
        @Override public Span.Builder error(Throwable throwable) { return this; }
        @Override public Span.Builder kind(Span.Kind kind) { return this; }
        @Override public Span.Builder remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span.Builder remoteIpAndPort(String ip, int port) { return this; }
        @Override public Span.Builder startTimestamp(long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span start() { return new FakeSpan(true); }
    }
}
