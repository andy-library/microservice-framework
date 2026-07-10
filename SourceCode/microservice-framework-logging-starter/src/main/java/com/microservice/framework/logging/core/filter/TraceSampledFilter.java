package com.microservice.framework.logging.core.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Marker;

/**
 * Trace 关联的日志采样过滤器
 * 根据 Trace 是否被采样来决定是否记录日志
 * 
 * @author Andy Yang
 */
public class TraceSampledFilter extends TurboFilter {

    private final Tracer tracer;
    private volatile Level levelForUnsampled = Level.INFO;
    private volatile boolean enabled = true;

    public TraceSampledFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (!enabled) {
            return FilterReply.NEUTRAL;
        }

        if (tracer == null) {
            return FilterReply.NEUTRAL;
        }

        // 获取当前 Span
        Span currentSpan = tracer.currentSpan();

        // 如果没有 Span，允许通过
        if (currentSpan == null) {
            return FilterReply.NEUTRAL;
        }

        // 判断 Trace 是否被采样
        boolean isSampled = currentSpan.context().sampled();

        if (isSampled) {
            // Trace 被采样，允许所有级别的日志
            return FilterReply.NEUTRAL;
        } else {
            // Trace 未被采样，只允许高于阈值的日志
            if (level.isGreaterOrEqual(levelForUnsampled)) {
                return FilterReply.NEUTRAL;
            } else {
                return FilterReply.DENY;
            }
        }
    }

    public void setLevelForUnsampled(String level) {
        this.levelForUnsampled = Level.toLevel(level, Level.INFO);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
