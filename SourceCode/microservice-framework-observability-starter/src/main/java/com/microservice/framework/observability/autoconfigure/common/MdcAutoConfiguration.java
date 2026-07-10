package com.microservice.framework.observability.autoconfigure.common;

import com.microservice.framework.observability.common.ObservabilityUtils;
import com.microservice.framework.observability.common.constants.MdcKeys;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MDC 自动配置
 * 自动将 traceId 和 spanId 注入到 MDC 中
 * 
 * @author Andy Yang
 */
@AutoConfiguration
@ConditionalOnClass({ Tracer.class, MDC.class })
public class MdcAutoConfiguration {

    /**
     * 初始化 ObservabilityUtils
     */
    @Bean
    public Object observabilityUtilsInitializer(Tracer tracer, Propagator propagator,
            @org.springframework.beans.factory.annotation.Autowired(required = false) io.micrometer.tracing.BaggageManager baggageManager) {
        ObservabilityUtils.init(tracer, propagator, baggageManager);
        return new Object();
    }

    /**
     * MDC 追踪上下文配置
     * 通过 Micrometer 的 ObservationHandler 自动注入 MDC
     */
    @Configuration(proxyBeanMethods = false)
    static class MdcObservationConfiguration {

        @Bean
        public io.micrometer.observation.ObservationHandler<?> mdcObservationHandler(Tracer tracer) {
            return new io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler(
                    new MdcTracingObservationHandler(tracer));
        }
    }

    /**
     * MDC 追踪观察处理器
     * <p>
     * 监听 Micrometer Observation 的生命周期事件：
     * 1. onStart: 从当前的 Trace Context 中提取 TraceId/SpanId 并注入到 SLF4J MDC
     * 2. onStop: 清理 MDC，防止内存泄漏或上下文污染
     * </p>
     */
    static class MdcTracingObservationHandler
            implements io.micrometer.observation.ObservationHandler<io.micrometer.observation.Observation.Context> {

        private final Tracer tracer;

        MdcTracingObservationHandler(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public void onStart(io.micrometer.observation.Observation.Context context) {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                MDC.put(MdcKeys.TRACE_ID, currentSpan.context().traceId());
                MDC.put(MdcKeys.SPAN_ID, currentSpan.context().spanId());
            }
        }

        @Override
        public void onStop(io.micrometer.observation.Observation.Context context) {
            MDC.remove(MdcKeys.TRACE_ID);
            MDC.remove(MdcKeys.SPAN_ID);
        }

        @Override
        public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
            return true;
        }
    }
}
