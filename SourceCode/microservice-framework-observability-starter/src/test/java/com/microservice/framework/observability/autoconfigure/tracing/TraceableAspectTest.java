package com.microservice.framework.observability.autoconfigure.tracing;

import com.microservice.framework.observability.annotation.Traceable;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * TraceableAspect unit tests.
 * <p>
 * Tests the actual {@code TraceableAspect.traceMethod()} behavior using
 * hand-written fakes instead of Mockito mocks. All fakes match
 * micrometer-tracing 1.3.x API.
 */
class TraceableAspectTest {

    private FakeTracer fakeTracer;
    private TraceableAspectAutoConfiguration.TraceableAspect aspect;

    @BeforeEach
    void setUp() {
        fakeTracer = new FakeTracer();
        aspect = new TraceableAspectAutoConfiguration.TraceableAspect(fakeTracer);
    }

    @Test
    @DisplayName("@Traceable: 已有 Span 时直接 proceed 不创建新 Span")
    void traceMethod_existingSpan_proceedsDirectly() throws Throwable {
        FakeSpan existingSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(existingSpan);

        Object result = aspect.traceMethod(createJoinPoint("resultValue"));

        assertThat(result).isEqualTo("resultValue");
        assertThat(fakeTracer.getCreatedSpanCount()).isEqualTo(0);
        assertThat(existingSpan.isEnded()).isFalse();
    }

    @Test
    @DisplayName("@Traceable: 无 Span 时创建新 Span 并正常结束")
    void traceMethod_noCurrentSpan_createsAndEndsSpan() throws Throwable {
        fakeTracer.setCurrentSpan(null);

        Object result = aspect.traceMethod(createJoinPoint("resultValue"));

        assertThat(result).isEqualTo("resultValue");
        assertThat(fakeTracer.getCreatedSpanCount()).isGreaterThan(0);
        assertThat(fakeTracer.getLastCreatedSpan().isEnded()).isTrue();
    }

    @Test
    @DisplayName("@Traceable: proceed 抛异常时记录 error 并结束 Span")
    void traceMethod_exception_recordsErrorAndEndsSpan() {
        fakeTracer.setCurrentSpan(null);
        RuntimeException expectedException = new RuntimeException("test error");

        assertThatCode(() -> aspect.traceMethod(createExceptionJoinPoint(expectedException)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test error");

        assertThat(fakeTracer.getLastCreatedSpan().getRecordedError()).isSameAs(expectedException);
        assertThat(fakeTracer.getLastCreatedSpan().isEnded()).isTrue();
    }

    @Test
    @DisplayName("@Traceable: @Traceable 注解有自定义 name 时使用该名称")
    void traceMethod_customNameFromAnnotation() throws Throwable {
        fakeTracer.setCurrentSpan(null);

        Method traceableMethod = TestService.class.getMethod("customSpanMethod");
        Object result = aspect.traceMethod(createJoinPointWithMethod("result", traceableMethod));

        assertThat(result).isEqualTo("result");
        assertThat(fakeTracer.getLastCreatedSpan().getSpanName()).isEqualTo("custom-span");
    }

    @Test
    @DisplayName("@Traceable: 无 @Traceable name 时使用类名.方法名")
    void traceMethod_defaultNameUsesClassNameAndMethodName() throws Throwable {
        fakeTracer.setCurrentSpan(null);

        Method defaultMethod = TestService.class.getMethod("defaultMethod");
        Object result = aspect.traceMethod(createJoinPointWithMethod("result", defaultMethod));

        assertThat(result).isEqualTo("result");
        assertThat(fakeTracer.getLastCreatedSpan().getSpanName()).isEqualTo("TestService.defaultMethod");
    }

    // ======================================================================
    // Test service class
    // ======================================================================

    static class TestService {
        @Traceable(name = "custom-span")
        public String customSpanMethod() { return "custom"; }

        public String defaultMethod() { return "default"; }
    }

    // ======================================================================
    // JoinPoint helpers — using Proxy to avoid implementing all interface methods
    // ======================================================================

    /**
     * Creates a ProceedingJoinPoint that returns the given result value on proceed().
     */
    private ProceedingJoinPoint createJoinPoint(String result) {
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, method, args) -> {
                    if ("proceed".equals(method.getName())) return result;
                    if ("getSignature".equals(method.getName())) {
                        return createMethodSignature(Object.class.getMethod("toString"));
                    }
                    return null;
                });
    }

    /**
     * Creates a ProceedingJoinPoint that throws the given exception on proceed().
     */
    private ProceedingJoinPoint createExceptionJoinPoint(Throwable exception) {
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, method, args) -> {
                    if ("proceed".equals(method.getName())) throw exception;
                    if ("getSignature".equals(method.getName())) {
                        return createMethodSignature(Object.class.getMethod("toString"));
                    }
                    return null;
                });
    }

    /**
     * Creates a ProceedingJoinPoint with a specific Method for signature resolution.
     */
    private ProceedingJoinPoint createJoinPointWithMethod(String result, Method targetMethod) {
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, method, args) -> {
                    if ("proceed".equals(method.getName())) return result;
                    if ("getSignature".equals(method.getName())) {
                        return createMethodSignature(targetMethod);
                    }
                    if ("getArgs".equals(method.getName())) return new Object[0];
                    return null;
                });
    }

    /**
     * Creates a MethodSignature proxy that returns the given Method.
     */
    private MethodSignature createMethodSignature(Method method) {
        return (MethodSignature) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{MethodSignature.class},
                (proxy, m, args) -> {
                    if ("getMethod".equals(m.getName())) return method;
                    if ("getName".equals(m.getName())) return method.getName();
                    if ("getDeclaringTypeName".equals(m.getName())) return method.getDeclaringClass().getSimpleName();
                    if ("getDeclaringType".equals(m.getName())) return method.getDeclaringClass();
                    return null;
                });
    }

    // ======================================================================
    // Fake implementations — matching micrometer-tracing 1.3.x API
    // ======================================================================

    static class FakeTracer implements Tracer {

        private Span currentSpan;
        private int createdSpanCount = 0;
        private FakeSpan lastCreatedSpan;

        void setCurrentSpan(Span span) { this.currentSpan = span; }
        int getCreatedSpanCount() { return createdSpanCount; }
        FakeSpan getLastCreatedSpan() { return lastCreatedSpan; }

        @Override public Span currentSpan() { return currentSpan; }

        @Override public Span nextSpan() {
            createdSpanCount++;
            lastCreatedSpan = new FakeSpan();
            return lastCreatedSpan;
        }

        @Override public Span nextSpan(Span parent) { return nextSpan(); }

        @Override public SpanInScope withSpan(Span span) {
            Span previous = this.currentSpan;
            this.currentSpan = span;
            return () -> { this.currentSpan = previous; };
        }

        @Override public ScopedSpan startScopedSpan(String name) { return null; }
        @Override public Span.Builder spanBuilder() { return new FakeSpanBuilder(); }
        @Override public TraceContext.Builder traceContextBuilder() { return null; }
        @Override public CurrentTraceContext currentTraceContext() { return null; }
        @Override public SpanCustomizer currentSpanCustomizer() { return null; }

        // BaggageManager methods (Tracer extends BaggageManager)
        @Override public Map<String, String> getAllBaggage() { return new HashMap<>(); }
        @Override public Baggage getBaggage(String name) { return null; }
        @Override public Baggage getBaggage(TraceContext context, String name) { return null; }
        @Override public Baggage createBaggage(String name) { return null; }
        @Override public Baggage createBaggage(String name, String value) { return null; }
    }

    static class FakeSpan implements Span {

        private String spanName;
        private boolean ended = false;
        private Throwable recordedError;
        private final Map<String, String> tags = new HashMap<>();

        String getSpanName() { return spanName; }
        boolean isEnded() { return ended; }
        Throwable getRecordedError() { return recordedError; }

        @Override public TraceContext context() { return new FakeTraceContext(); }
        @Override public boolean isNoop() { return false; }
        @Override public Span start() { return this; }
        @Override public Span name(String name) { this.spanName = name; return this; }
        @Override public Span event(String name) { return this; }
        @Override public Span event(String name, long timestamp, java.util.concurrent.TimeUnit unit) { return this; }
        @Override public Span tag(String key, String value) { tags.put(key, value); return this; }
        @Override public Span error(Throwable throwable) { this.recordedError = throwable; return this; }
        @Override public void end() { this.ended = true; }
        @Override public void end(long timestamp, java.util.concurrent.TimeUnit unit) { this.ended = true; }
        @Override public void abandon() {}
        @Override public Span remoteServiceName(String remoteServiceName) { return this; }
        @Override public Span remoteIpAndPort(String ip, int port) { return this; }
    }

    static class FakeTraceContext implements TraceContext {
        @Override public String traceId() { return "fakeTraceId"; }
        @Override public String spanId() { return "fakeSpanId"; }
        @Override public String parentId() { return null; }
        @Override public Boolean sampled() { return true; }
    }

    static class FakeSpanBuilder implements Span.Builder {
        private String name;

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
        @Override public Span start() { FakeSpan span = new FakeSpan(); span.name(name); return span; }
    }
}
