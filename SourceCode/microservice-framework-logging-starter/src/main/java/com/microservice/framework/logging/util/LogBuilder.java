package com.microservice.framework.logging.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import ch.qos.logback.classic.LoggerContext;
import com.microservice.framework.logging.core.masking.PatternMaskingConverter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent 日志构建器
 * <p>
 * 提供链式调用的结构化日志记录能力，自动将上下文信息写入 MDC
 * <p>
 * 使用示例：
 * 
 * <pre>
 * Log.info("订单创建成功")
 *         .with("orderId", orderId)
 *         .with("userId", userId)
 *         .with("amount", amount)
 *         .log();
 * </pre>
 *
 * @author Andy Yang
 */
public class LogBuilder {

    private static final Logger log = LoggerFactory.getLogger(LogBuilder.class);

    private final Level level;
    private final String message;
    private final Map<String, Object> context;
    private Throwable throwable;

    LogBuilder(Level level, String message) {
        this.level = level;
        this.message = message;
        this.context = new LinkedHashMap<>();
    }

    /**
     * 添加上下文键值对
     *
     * @param key   键（会作为 JSON 字段名）
     * @param value 值
     * @return 当前 Builder 实例
     */
    public LogBuilder with(String key, Object value) {
        if (key != null) {
            context.put(key, value);
        }
        return this;
    }

    /**
     * 添加多个上下文键值对
     *
     * @param pairs 键值对 Map
     * @return 当前 Builder 实例
     */
    public LogBuilder withAll(Map<String, Object> pairs) {
        if (pairs != null) {
            context.putAll(pairs);
        }
        return this;
    }

    /**
     * 添加异常信息
     *
     * @param t 异常对象
     * @return 当前 Builder 实例
     */
    public LogBuilder withException(Throwable t) {
        this.throwable = t;
        return this;
    }

    /**
     * 执行日志记录
     * <p>
     * 此方法会：
     * 1. 将上下文信息序列化为 JSON 并写入 MDC
     * 2. 根据日志级别记录日志
     * 3. 清理 MDC 中的临时数据
     */
    public void log() {
        if (!isLevelEnabled()) {
            return;
        }

        // 将上下文写入 MDC
        String structuredData = null;
        if (!context.isEmpty()) {
            structuredData = toJson(context);
            MDC.put("structured_data", structuredData);
        }

        // 添加异常信息到 MDC
        if (throwable != null) {
            MDC.put("error.class", throwable.getClass().getName());
            MDC.put("error.message", throwable.getMessage());
            MDC.put("error.stack_hash", computeStackHash(throwable));
        }

        try {
            doLog();
        } finally {
            // 清理 MDC
            if (structuredData != null) {
                MDC.remove("structured_data");
            }
            if (throwable != null) {
                MDC.remove("error.class");
                MDC.remove("error.message");
                MDC.remove("error.stack_hash");
            }
        }
    }

    private boolean isLevelEnabled() {
        return switch (level) {
            case TRACE -> log.isTraceEnabled();
            case DEBUG -> log.isDebugEnabled();
            case INFO -> log.isInfoEnabled();
            case WARN -> log.isWarnEnabled();
            case ERROR -> log.isErrorEnabled();
        };
    }

    private void doLog() {
        switch (level) {
            case TRACE -> {
                if (throwable != null)
                    log.trace(message, throwable);
                else
                    log.trace(message);
            }
            case DEBUG -> {
                if (throwable != null)
                    log.debug(message, throwable);
                else
                    log.debug(message);
            }
            case INFO -> {
                if (throwable != null)
                    log.info(message, throwable);
                else
                    log.info(message);
            }
            case WARN -> {
                if (throwable != null)
                    log.warn(message, throwable);
                else
                    log.warn(message);
            }
            case ERROR -> {
                if (throwable != null)
                    log.error(message, throwable);
                else
                    log.error(message);
            }
        }
    }

    /**
     * 计算堆栈哈希
     */
    private String computeStackHash(Throwable e) {
        if (e == null || e.getStackTrace() == null || e.getStackTrace().length == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, e.getStackTrace().length);
        for (int i = 0; i < limit; i++) {
            StackTraceElement element = e.getStackTrace()[i];
            sb.append(element.getClassName())
                    .append(".")
                    .append(element.getMethodName())
                    .append(":")
                    .append(element.getLineNumber());
        }
        return String.valueOf(Math.abs(sb.toString().hashCode()));
    }

    /**
     * 转换为 JSON（简化版本，优先使用 JsonUtils 如果可用）
     */
    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        // 尝试使用 JsonUtils (如果 json-starter 可用)
        try {
            Class<?> jsonUtilsClass = Class.forName("com.microservice.framework.json.util.JsonUtils");
            java.lang.reflect.Method toJsonMethod = jsonUtilsClass.getMethod("toJson", Object.class);
            return maskStructuredData((String) toJsonMethod.invoke(null, map));
        } catch (Exception ignored) {
            // 降级到简单实现
        }

        // 简单 JSON 序列化
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return maskStructuredData(sb.toString());
    }

    private String maskStructuredData(String value) {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
            Object converter = context.getObject("patternMaskingConverter");
            if (converter instanceof PatternMaskingConverter maskingConverter) {
                return maskingConverter.mask(value);
            }
        }
        return value;
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
