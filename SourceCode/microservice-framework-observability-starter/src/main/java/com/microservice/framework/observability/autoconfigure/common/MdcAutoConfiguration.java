package com.microservice.framework.observability.autoconfigure.common;

import com.microservice.framework.observability.common.ObservabilityUtils;
import com.microservice.framework.observability.common.constants.MdcKeys;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean({ Tracer.class, Propagator.class })
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
    @ConditionalOnBean({ Tracer.class, Propagator.class })
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

        private static final String PREVIOUS_TRACE_ID = MdcTracingObservationHandler.class.getName() + ".previousTraceId";
        private static final String PREVIOUS_SPAN_ID = MdcTracingObservationHandler.class.getName() + ".previousSpanId";
        private static final String PREVIOUS_REQUEST_ID = MdcTracingObservationHandler.class.getName() + ".previousRequestId";
        private static final Object ABSENT_MDC_VALUE = new Object();

        private final Tracer tracer;

        MdcTracingObservationHandler(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public void onStart(io.micrometer.observation.Observation.Context context) {
            context.put(PREVIOUS_TRACE_ID, previousValue(MdcKeys.TRACE_ID));
            context.put(PREVIOUS_SPAN_ID, previousValue(MdcKeys.SPAN_ID));
            context.put(PREVIOUS_REQUEST_ID, previousValue(MdcKeys.REQUEST_ID));

            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                MDC.put(MdcKeys.TRACE_ID, currentSpan.context().traceId());
                MDC.put(MdcKeys.SPAN_ID, currentSpan.context().spanId());
            }
        }

        @Override
        public void onStop(io.micrometer.observation.Observation.Context context) {
            restore(MdcKeys.TRACE_ID, context.get(PREVIOUS_TRACE_ID));
            restore(MdcKeys.SPAN_ID, context.get(PREVIOUS_SPAN_ID));
            restore(MdcKeys.REQUEST_ID, context.get(PREVIOUS_REQUEST_ID));
        }

        private void restore(String key, Object value) {
            if (value == null || value == ABSENT_MDC_VALUE) {
                MDC.remove(key);
                return;
            }
            MDC.put(key, value.toString());
        }

        private Object previousValue(String key) {
            String value = MDC.get(key);
            return value == null ? ABSENT_MDC_VALUE : value;
        }

        @Override
        public boolean supportsContext(io.micrometer.observation.Observation.Context context) {
            return true;
        }
    }
}
