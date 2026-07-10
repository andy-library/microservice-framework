package com.microservice.framework.logging.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * 日志工具类
 * <p>
 * 提供两种日志记录方式：
 * 
 * <h3>1. Fluent API（推荐，结构化场景）</h3>
 * 
 * <pre>
 * Log.info("订单创建成功")
 *         .with("orderId", orderId)
 *         .with("userId", userId)
 *         .log();
 * </pre>
 * 
 * <h3>2. SLF4J 占位符语法（快速日志）</h3>
 * 
 * <pre>
 * Log.info("订单创建成功, orderId={}", orderId);
 * Log.error("处理失败, orderId={}", orderId, exception);
 * </pre>
 * 
 * <h3>建议</h3>
 * <ul>
 * <li>业务监控日志：使用 Fluent API，便于日志系统解析</li>
 * <li>调试/临时日志：使用占位符语法，编写更快</li>
 * </ul>
 *
 * @author Andy Yang
 */
public final class Log {

    private static final Logger log = LoggerFactory.getLogger(Log.class);

    private Log() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========================================================================
    // Fluent API 入口
    // ========================================================================

    /**
     * 创建 TRACE 级别的日志构建器
     *
     * @param message 日志消息
     * @return LogBuilder 实例
     */
    public static LogBuilder trace(String message) {
        return new LogBuilder(Level.TRACE, message);
    }

    /**
     * 创建 DEBUG 级别的日志构建器
     *
     * @param message 日志消息
     * @return LogBuilder 实例
     */
    public static LogBuilder debug(String message) {
        return new LogBuilder(Level.DEBUG, message);
    }

    /**
     * 创建 INFO 级别的日志构建器
     *
     * @param message 日志消息
     * @return LogBuilder 实例
     */
    public static LogBuilder info(String message) {
        return new LogBuilder(Level.INFO, message);
    }

    /**
     * 创建 WARN 级别的日志构建器
     *
     * @param message 日志消息
     * @return LogBuilder 实例
     */
    public static LogBuilder warn(String message) {
        return new LogBuilder(Level.WARN, message);
    }

    /**
     * 创建 ERROR 级别的日志构建器
     *
     * @param message 日志消息
     * @return LogBuilder 实例
     */
    public static LogBuilder error(String message) {
        return new LogBuilder(Level.ERROR, message);
    }

    // ========================================================================
    // SLF4J 占位符语法兼容
    // ========================================================================

    /**
     * TRACE 级别日志（占位符语法）
     *
     * @param format 格式化字符串，使用 {} 作为占位符
     * @param args   参数
     */
    public static void trace(String format, Object... args) {
        log.trace(format, args);
    }

    /**
     * DEBUG 级别日志（占位符语法）
     *
     * @param format 格式化字符串，使用 {} 作为占位符
     * @param args   参数
     */
    public static void debug(String format, Object... args) {
        log.debug(format, args);
    }

    /**
     * INFO 级别日志（占位符语法）
     *
     * @param format 格式化字符串，使用 {} 作为占位符
     * @param args   参数
     */
    public static void info(String format, Object... args) {
        log.info(format, args);
    }

    /**
     * WARN 级别日志（占位符语法）
     *
     * @param format 格式化字符串，使用 {} 作为占位符
     * @param args   参数
     */
    public static void warn(String format, Object... args) {
        log.warn(format, args);
    }

    /**
     * ERROR 级别日志（占位符语法）
     *
     * @param format 格式化字符串，使用 {} 作为占位符
     * @param args   参数
     */
    public static void error(String format, Object... args) {
        log.error(format, args);
    }

    /**
     * ERROR 级别异常日志
     *
     * @param message 日志消息
     * @param t       异常对象
     */
    public static void error(String message, Throwable t) {
        log.error(message, t);
    }
}
