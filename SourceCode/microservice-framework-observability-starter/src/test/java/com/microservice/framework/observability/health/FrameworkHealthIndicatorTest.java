package com.microservice.framework.observability.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FrameworkHealthIndicator 单元测试
 */
class FrameworkHealthIndicatorTest {

    private FrameworkHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        // 使用匿名内部类实现抽象类
        healthIndicator = new FrameworkHealthIndicator() {
            @Override
            protected Health doHealthCheck() {
                return Health.up()
                        .withDetail("version", "1.0.0-alpha.1")
                        .withDetail("module", "observability-starter")
                        .withDetail("startupTime", System.currentTimeMillis())
                        .build();
            }
        };
    }

    @Test
    @DisplayName("health: 返回 UP 状态")
    void testHealth_ReturnsUp() {
        Health health = healthIndicator.health();

        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    @DisplayName("health: 包含版本信息")
    void testHealth_IncludesVersion() {
        Health health = healthIndicator.health();

        assertNotNull(health.getDetails());
        assertTrue(health.getDetails().containsKey("version"),
                "健康信息应包含版本");
        assertEquals("1.0.0-alpha.1", health.getDetails().get("version"));
    }

    @Test
    @DisplayName("health: 包含模块信息")
    void testHealth_IncludesModuleInfo() {
        Health health = healthIndicator.health();

        assertNotNull(health.getDetails());
        assertTrue(health.getDetails().containsKey("module"),
                "健康信息应包含模块");
        assertEquals("observability-starter", health.getDetails().get("module"));
    }

    @Test
    @DisplayName("health: 异常时返回 DOWN")
    void testHealth_ReturnsDownOnException() {
        FrameworkHealthIndicator failingIndicator = new FrameworkHealthIndicator() {
            @Override
            protected Health doHealthCheck() {
                throw new RuntimeException("模拟健康检查失败");
            }
        };

        Health health = failingIndicator.health();

        assertNotNull(health);
        assertEquals(Status.DOWN, health.getStatus());
    }
}
