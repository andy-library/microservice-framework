package com.microservice.framework.common.time;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;

/**
 * 框架统一时钟
 * <p>
 * 封装 Java {@link Clock}，所有日期时间操作必须使用配置的时区，
 * 禁止依赖 {@code ZoneId.systemDefault()}。
 * <p>
 * 通过自动配置注入，业务代码使用 {@code frameworkClock.now()} 获取当前时间。
 *
 * @author Andy Yang
 */
public class FrameworkClock {

    private final ZoneId zoneId;
    private final Clock clock;

    /**
     * 构造框架时钟
     *
     * @param zoneId 配置的时区，不可为 null
     */
    FrameworkClock(ZoneId zoneId) {
        this.zoneId = zoneId;
        this.clock = Clock.system(zoneId);
    }

    /**
     * 获取当前日期时间（配置时区）
     *
     * @return 当前 LocalDateTime，使用配置的时区
     */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 获取当前时刻（UTC Instant）
     *
     * @return 当前 Instant
     */
    public Instant nowInstant() {
        return Instant.now(clock);
    }

    /**
     * 获取配置的时区
     *
     * @return 配置的 ZoneId，保证不返回 systemDefault
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * 静态工厂方法，根据时区 ID 创建框架时钟
     * <p>
     * 如果时区字符串无效，抛出 {@link FrameworkException} 并附带明确错误码。
     *
     * @param zoneId 时区标识，不可为 null
     * @return 框架时钟实例
     * @throws FrameworkException 时区无效时抛出
     */
    public static FrameworkClock of(ZoneId zoneId) {
        if (zoneId == null) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_TIME_INVALID_ZONE,
                    "ZoneId must not be null");
        }
        return new FrameworkClock(zoneId);
    }

    /**
     * 静态工厂方法，根据时区字符串创建框架时钟
     * <p>
     * 如果时区字符串无效，抛出 {@link FrameworkException} 并附带明确错误码，
     * 而不是 Java 原生的 {@link ZoneRulesException}。
     *
     * @param timeZone 时区字符串（如 "UTC"、"Asia/Shanghai"）
     * @return 框架时钟实例
     * @throws FrameworkException 时区字符串无效时抛出
     */
    public static FrameworkClock of(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_TIME_INVALID_ZONE,
                    "Time zone string must not be null or blank");
        }
        try {
            ZoneId zoneId = ZoneId.of(timeZone);
            return new FrameworkClock(zoneId);
        } catch (java.time.DateTimeException e) {
            throw new FrameworkException(
                    FrameworkErrorCode.COMMON_TIME_INVALID_ZONE,
                    "Invalid time zone: " + timeZone, e);
        }
    }
}
