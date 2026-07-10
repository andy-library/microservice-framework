package com.microservice.framework.observability.common;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.BaggageManager;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;

/**
 * 可观测性工具类
 * 提供便捷的API用于与追踪上下文交互
 * 
 * @author Andy Yang
 */
public final class ObservabilityUtils {

    private static volatile Tracer tracer;
    private static volatile Propagator propagator;
    private static volatile BaggageManager baggageManager;

    /**
     * 初始化（由自动配置调用）
     */
    public static void init(Tracer tracer, Propagator propagator, @Nullable BaggageManager baggageManager) {
        ObservabilityUtils.tracer = tracer;
        ObservabilityUtils.propagator = propagator;
        ObservabilityUtils.baggageManager = baggageManager;
    }

    /**
     * 获取当前 TraceID
     * 
     * @return TraceID，如果不存在则返回 null
     */
    @Nullable
    public static String getTraceId() {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        return currentSpan.context().traceId();
    }

    /**
     * 获取当前 SpanID
     * 
     * @return SpanID，如果不存在则返回 null
     */
    @Nullable
    public static String getSpanId() {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        return currentSpan.context().spanId();
    }

    /**
     * 向调用链添加业务行李（Baggage）
     * 
     * @param key   行李键
     * @param value 行李值
     */
    public static void addBaggage(String key, String value) {
        if (baggageManager == null) {
            return;
        }
        baggageManager.createBaggage(key, value);
    }

    /**
     * 从调用链移除业务行李
     * 
     * @param key 行李键
     */
    public static void removeBaggage(String key) {
        if (baggageManager == null) {
            return;
        }
        baggageManager.createBaggage(key).set(null);
    }

    /**
     * 向当前 Span 添加自定义标签
     * 
     * @param key   标签键
     * @param value 标签值
     */
    public static void addCustomTag(String key, String value) {
        if (tracer == null) {
            return;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }

    /**
     * 从 MDC 获取值
     * 
     * @param key MDC 键
     * @return MDC 值
     */
    @Nullable
    public static String getMdcValue(String key) {
        return MDC.get(key);
    }

    private ObservabilityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}
