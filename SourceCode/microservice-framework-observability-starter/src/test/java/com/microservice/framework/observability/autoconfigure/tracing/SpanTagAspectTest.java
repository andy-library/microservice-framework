package com.microservice.framework.observability.autoconfigure.tracing;

import com.microservice.framework.observability.annotation.SpanTag;
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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpanTagAspect unit tests.
 * <p>
 * Tests the actual {@code SpanTagAspect.addSpanTags()} behavior using
 * hand-written fakes instead of Mockito mocks. All fakes match
 * micrometer-tracing 1.3.x API. Uses Proxy for ProceedingJoinPoint
 * to avoid implementing the full AspectJ interface.
 */
class SpanTagAspectTest {

    private FakeTracer fakeTracer;
    private SpanTagAspectAutoConfiguration.SpanTagAspect aspect;

    @BeforeEach
    void setUp() {
        fakeTracer = new FakeTracer();
        aspect = new SpanTagAspectAutoConfiguration.SpanTagAspect(fakeTracer);
    }

    @Test
    @DisplayName("@SpanTag: 参数值被注入到 Span 标签")
    void spanTag_addsTagFromParameter() throws Throwable {
        FakeSpan fakeSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(fakeSpan);

        Method method = TaggedService.class.getMethod("processOrder", String.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs("result", method, new Object[]{"order123"}));

        assertThat(result).isEqualTo("result");
        assertThat(fakeSpan.getTagValue("orderId")).isEqualTo("order123");
    }

    @Test
    @DisplayName("@SpanTag: null 参数值被跳过不注入标签")
    void spanTag_nullValueIsSkipped() throws Throwable {
        FakeSpan fakeSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(fakeSpan);

        Method method = TaggedService.class.getMethod("processOrder", String.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs("result", method, new Object[]{null}));

        assertThat(result).isEqualTo("result");
        assertThat(fakeSpan.getTagValue("orderId")).isNull();
    }

    @Test
    @DisplayName("@SpanTag: 自定义 key 被正确注入")
    void spanTag_customKeyIsUsed() throws Throwable {
        FakeSpan fakeSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(fakeSpan);

        Method method = TaggedService.class.getMethod("processWithCustomKey", String.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs("result", method, new Object[]{"customValue"}));

        assertThat(result).isEqualTo("result");
        assertThat(fakeSpan.getTagValue("custom.key")).isEqualTo("customValue");
    }

    @Test
    @DisplayName("@SpanTag: 多个 @SpanTag 参数都被注入")
    void spanTag_multipleTagsAreInjected() throws Throwable {
        FakeSpan fakeSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(fakeSpan);

        Method method = TaggedService.class.getMethod("processMultiple", String.class, Integer.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs("result", method, new Object[]{"value1", 42}));

        assertThat(result).isEqualTo("result");
        assertThat(fakeSpan.getTagValue("key1")).isEqualTo("value1");
        assertThat(fakeSpan.getTagValue("key2")).isEqualTo("42");
    }

    @Test
    @DisplayName("@SpanTag: 敏感键和值不得进入 Span")
    void spanTag_sensitiveValuesAreRedacted() throws Throwable {
        FakeSpan fakeSpan = new FakeSpan();
        fakeTracer.setCurrentSpan(fakeSpan);

        Method method = TaggedService.class.getMethod("processSecret", String.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs(
                "result", method, new Object[]{"Bearer top-secret-token"}));

        assertThat(result).isEqualTo("result");
        assertThat(fakeSpan.getTagValue("authorization")).isEqualTo("[REDACTED]");
    }

    @Test
    @DisplayName("@SpanTag: 配置的敏感键应被脱敏")
    void spanTag_configuredSensitiveKeysAreRedacted() {
        FakeTracer tracer = new FakeTracer();
        FakeSpan span = new FakeSpan();
        tracer.setCurrentSpan(span);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpanTagAspectAutoConfiguration.class))
                .withBean(Tracer.class, () -> tracer)
                .withPropertyValues("framework.observability.tracing.span-tags.sensitive-keys[0]=customerId")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SpanTagAspectAutoConfiguration.SpanTagAspect configuredAspect =
                            context.getBean(SpanTagAspectAutoConfiguration.SpanTagAspect.class);
                    Method method = TaggedService.class.getMethod("processCustomer", String.class);

                    Object result = configuredAspect.addSpanTags(
                            createJoinPointWithMethodAndArgs("result", method, new Object[]{"customer-123"}));

                    assertThat(result).isEqualTo("result");
                    assertThat(span.getTagValue("customerId")).isEqualTo("[REDACTED]");
                });
    }

    @Test
    @DisplayName("@SpanTag: 配置的高基数字段应被脱敏")
    void spanTag_configuredHighCardinalityKeysAreRedacted() {
        FakeTracer tracer = new FakeTracer();
        FakeSpan span = new FakeSpan();
        tracer.setCurrentSpan(span);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpanTagAspectAutoConfiguration.class))
                .withBean(Tracer.class, () -> tracer)
                .withPropertyValues("framework.observability.tracing.span-tags.high-cardinality-keys[0]=orderId")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SpanTagAspectAutoConfiguration.SpanTagAspect configuredAspect =
                            context.getBean(SpanTagAspectAutoConfiguration.SpanTagAspect.class);
                    Method method = TaggedService.class.getMethod("processOrder", String.class);

                    Object result = configuredAspect.addSpanTags(
                            createJoinPointWithMethodAndArgs("result", method, new Object[]{"order-123456789"}));

                    assertThat(result).isEqualTo("result");
                    assertThat(span.getTagValue("orderId")).isEqualTo("[REDACTED]");
                });
    }

    @Test
    @DisplayName("@SpanTag: 配置的最大标签值长度应限制输出")
    void spanTag_configuredMaxValueLengthIsApplied() {
        FakeTracer tracer = new FakeTracer();
        FakeSpan span = new FakeSpan();
        tracer.setCurrentSpan(span);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SpanTagAspectAutoConfiguration.class))
                .withBean(Tracer.class, () -> tracer)
                .withPropertyValues("framework.observability.tracing.span-tags.max-value-length=8")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    SpanTagAspectAutoConfiguration.SpanTagAspect configuredAspect =
                            context.getBean(SpanTagAspectAutoConfiguration.SpanTagAspect.class);
                    Method method = TaggedService.class.getMethod("processWithCustomKey", String.class);

                    Object result = configuredAspect.addSpanTags(
                            createJoinPointWithMethodAndArgs("result", method, new Object[]{"1234567890"}));

                    assertThat(result).isEqualTo("result");
                    assertThat(span.getTagValue("custom.key")).isEqualTo("12345678");
                });
    }

    @Test
    @DisplayName("@SpanTag: 无活跃 Span 时不报错")
    void spanTag_noActiveSpanDoesNotError() throws Throwable {
        fakeTracer.setCurrentSpan(null);

        Method method = TaggedService.class.getMethod("processOrder", String.class);
        Object result = aspect.addSpanTags(createJoinPointWithMethodAndArgs("result", method, new Object[]{"order123"}));

        assertThat(result).isEqualTo("result");
    }

    // ======================================================================
    // Test service class with @SpanTag annotations
    // ======================================================================

    static class TaggedService {
        public String processOrder(@SpanTag("orderId") String orderId) {
            return "processed:" + orderId;
        }

        public String processCustomer(@SpanTag("customerId") String customerId) {
            return "customer:" + customerId;
        }

        public String processWithCustomKey(@SpanTag("custom.key") String value) {
            return "custom:" + value;
        }

        public String processMultiple(
                @SpanTag("key1") String first,
                @SpanTag("key2") Integer second) {
            return "multi:" + first + ":" + second;
        }

        public String processSecret(@SpanTag("authorization") String authorization) {
            return authorization;
        }
    }

    // ======================================================================
    // JoinPoint helpers — using Proxy for ProceedingJoinPoint
    // ======================================================================

    private ProceedingJoinPoint createJoinPointWithMethodAndArgs(String result, Method method, Object[] args) {
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ProceedingJoinPoint.class},
                (proxy, m, methodArgs) -> {
                    if ("proceed".equals(m.getName())) return result;
                    if ("getSignature".equals(m.getName())) {
                        return createMethodSignature(method);
                    }
                    if ("getArgs".equals(m.getName())) return args;
                    return null;
                });
    }

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

        void setCurrentSpan(Span span) { this.currentSpan = span; }

        @Override public Span currentSpan() { return currentSpan; }
        @Override public Span nextSpan() { return new FakeSpan(); }
        @Override public Span nextSpan(Span parent) { return new FakeSpan(); }
        @Override public SpanInScope withSpan(Span span) { return () -> {}; }
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
        private final Map<String, String> tags = new HashMap<>();
        private boolean ended = false;
        private Throwable recordedError;

        String getTagValue(String key) { return tags.get(key); }
        boolean isEnded() { return ended; }
        Throwable getRecordedError() { return recordedError; }

        @Override public TraceContext context() { return new FakeTraceContext(); }
        @Override public boolean isNoop() { return false; }
        @Override public Span start() { return this; }
        @Override public Span name(String name) { return this; }
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
        @Override public Span start() { return new FakeSpan(); }
    }
}
