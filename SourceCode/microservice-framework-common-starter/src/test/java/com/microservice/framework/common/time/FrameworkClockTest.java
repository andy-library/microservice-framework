package com.microservice.framework.common.time;

import com.microservice.framework.common.error.FrameworkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FrameworkClock 单元测试
 * <p>
 * 验证框架时钟的创建、时区配置、时间获取以及异常处理。
 *
 * @author Andy Yang
 */
class FrameworkClockTest {

    @Test
    @DisplayName("使用有效时区字符串创建 FrameworkClock")
    void shouldCreateWithValidTimeZone() {
        FrameworkClock clock = FrameworkClock.of("UTC");
        assertThat(clock).isNotNull();
        assertThat(clock.getZoneId()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("使用无效时区字符串应抛出 FrameworkException")
    void shouldThrowFrameworkExceptionForInvalidTimeZone() {
        assertThatThrownBy(() -> FrameworkClock.of("Invalid/Zone"))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("Invalid time zone");
    }

    @Test
    @DisplayName("使用 null 时区字符串应抛出 FrameworkException")
    void shouldThrowFrameworkExceptionForNullTimeZoneString() {
        assertThatThrownBy(() -> FrameworkClock.of((String) null))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("Time zone string must not be null or blank");
    }

    @Test
    @DisplayName("使用空白时区字符串应抛出 FrameworkException")
    void shouldThrowFrameworkExceptionForBlankTimeZoneString() {
        assertThatThrownBy(() -> FrameworkClock.of("   "))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("Time zone string must not be null or blank");
    }

    @Test
    @DisplayName("使用 null ZoneId 应抛出 FrameworkException")
    void shouldThrowFrameworkExceptionForNullZoneId() {
        assertThatThrownBy(() -> FrameworkClock.of((ZoneId) null))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("ZoneId must not be null");
    }

    @Test
    @DisplayName("now() 应返回配置时区的 LocalDateTime")
    void nowShouldReturnLocalDateTimeInConfiguredTimeZone() {
        FrameworkClock clock = FrameworkClock.of("UTC");
        LocalDateTime now = clock.now();
        assertThat(now).isNotNull();
        // 时间应在合理范围内（当前年份附近）
        assertThat(now.getYear()).isBetween(2020, 2030);
    }

    @Test
    @DisplayName("nowInstant() 应返回 Instant")
    void nowInstantShouldReturnInstant() {
        FrameworkClock clock = FrameworkClock.of("UTC");
        Instant instant = clock.nowInstant();
        assertThat(instant).isNotNull();
        assertThat(instant.getEpochSecond()).isPositive();
    }

    @Test
    @DisplayName("getZoneId() 应返回配置的 ZoneId")
    void getZoneIdShouldReturnConfiguredZoneId() {
        FrameworkClock clock = FrameworkClock.of("Asia/Shanghai");
        assertThat(clock.getZoneId()).isEqualTo(ZoneId.of("Asia/Shanghai"));
    }

    @Test
    @DisplayName("静态工厂 of(String) 应支持常见时区")
    void staticFactoryShouldSupportCommonTimeZones() {
        assertThat(FrameworkClock.of("UTC").getZoneId().getId()).isEqualTo("UTC");
        assertThat(FrameworkClock.of("Asia/Shanghai").getZoneId().getId()).isEqualTo("Asia/Shanghai");
        assertThat(FrameworkClock.of("America/New_York").getZoneId().getId()).isEqualTo("America/New_York");
    }

    @Test
    @DisplayName("使用 ZoneId 对象创建 FrameworkClock")
    void shouldCreateWithZoneIdObject() {
        ZoneId zoneId = ZoneId.of("Europe/Berlin");
        FrameworkClock clock = FrameworkClock.of(zoneId);
        assertThat(clock.getZoneId()).isEqualTo(zoneId);
    }
}
