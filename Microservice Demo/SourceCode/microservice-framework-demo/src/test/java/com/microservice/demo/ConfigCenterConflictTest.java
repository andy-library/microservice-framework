package com.microservice.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.microservice.demo.DemoApplication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Config Center Mutual Exclusion Test — verifies that Nacos and Apollo starters
 * cannot be enabled simultaneously, per the "one config center only" governance rule.
 *
 * <p>The framework enforces mutual exclusion through
 * {@link com.microservice.framework.nacos.autoconfigure.ConfigCenterMutualExclusionAutoConfiguration},
 * which detects when both {@code framework.nacos.enabled=true} and
 * {@code framework.apollo.enabled=true} are set simultaneously and throws
 * {@link com.microservice.framework.nacos.autoconfigure.ConfigCenterMutualExclusionException}.
 *
 * <p>This property-based check works regardless of whether real Apollo/Nacos client
 * libraries are on the classpath — it checks the property values directly, ensuring
 * the mutual exclusion constraint is enforced even with only framework wrapper starters.
 */
@DisplayName("配置中心互斥治理测试")
class ConfigCenterConflictTest {

    @Test
    @DisplayName("nacos=true, apollo=false → 成功启动，只有 nacos 配置治理 Bean 存在")
    void testNacosOnlyConfigurationGovernance() {
        ConfigurableApplicationContext context = null;
        try {
            context = SpringApplication.run(DemoApplication.class,
                    "--spring.profiles.active=full-embedded",
                    "--framework.nacos.enabled=true",
                    "--framework.apollo.enabled=false",
                    "--framework.config.enabled=true",
                    "--server.port=0",
                    "--spring.datasource.url=jdbc:h2:mem:conflict-nacos;DB_CLOSE_DELAY=-1;MODE=MySQL");
        } catch (Exception e) {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
            throw new AssertionError("Application should start with nacos-only config: " + e.getMessage(), e);
        }

        // Only nacos governance beans should exist
        assertTrue(context.containsBean("nacosConfigValidator"), "nacosConfigValidator should exist");
        assertTrue(context.containsBean("nacosSensitiveConfigMasker"), "nacosSensitiveConfigMasker should exist");
        assertFalse(context.containsBean("apolloConfigValidator"), "apolloConfigValidator should NOT exist");
        assertFalse(context.containsBean("apolloSensitiveConfigMasker"), "apolloSensitiveConfigMasker should NOT exist");

        try { context.close(); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("apollo=true, nacos=false → 成功启动，只有 apollo 配置治理 Bean 存在")
    void testApolloOnlyConfigurationGovernance() {
        ConfigurableApplicationContext context = null;
        try {
            context = SpringApplication.run(DemoApplication.class,
                    "--spring.profiles.active=full-embedded",
                    "--framework.apollo.enabled=true",
                    "--framework.nacos.enabled=false",
                    "--framework.config.enabled=true",
                    "--server.port=0",
                    "--spring.datasource.url=jdbc:h2:mem:conflict-apollo;DB_CLOSE_DELAY=-1;MODE=MySQL");
        } catch (Exception e) {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
            throw new AssertionError("Application should start with apollo-only config: " + e.getMessage(), e);
        }

        // Only apollo governance beans should exist
        assertTrue(context.containsBean("apolloConfigValidator"), "apolloConfigValidator should exist");
        assertTrue(context.containsBean("apolloSensitiveConfigMasker"), "apolloSensitiveConfigMasker should exist");
        assertFalse(context.containsBean("nacosConfigValidator"), "nacosConfigValidator should NOT exist");
        assertFalse(context.containsBean("nacosSensitiveConfigMasker"), "nacosSensitiveConfigMasker should NOT exist");

        try { context.close(); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("nacos=false, apollo=false → 成功启动，配置治理 Bean 不存在")
    void testBothDisabledNoGovernance() {
        ConfigurableApplicationContext context = null;
        try {
            context = SpringApplication.run(DemoApplication.class,
                    "--spring.profiles.active=full-embedded",
                    "--framework.nacos.enabled=false",
                    "--framework.apollo.enabled=false",
                    "--server.port=0",
                    "--spring.datasource.url=jdbc:h2:mem:conflict-none;DB_CLOSE_DELAY=-1;MODE=MySQL");
        } catch (Exception e) {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
            throw new AssertionError("Application should start with both disabled: " + e.getMessage(), e);
        }

        // No config center governance beans should exist
        assertFalse(context.containsBean("nacosConfigValidator"), "nacosConfigValidator should NOT exist");
        assertFalse(context.containsBean("apolloConfigValidator"), "apolloConfigValidator should NOT exist");
        assertFalse(context.containsBean("nacosSensitiveConfigMasker"), "nacosSensitiveConfigMasker should NOT exist");
        assertFalse(context.containsBean("apolloSensitiveConfigMasker"), "apolloSensitiveConfigMasker should NOT exist");

        try { context.close(); } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("nacos=true, apollo=true → 启动失败，互斥冲突错误信息明确")
    void testBothEnabledCausesStartupFailure() {
        ConfigurableApplicationContext context = null;
        Throwable exception = null;

        try {
            // Both config centers enabled simultaneously — must fail with mutual exclusion error
            context = SpringApplication.run(DemoApplication.class,
                    "--spring.profiles.active=full-embedded",
                    "--framework.nacos.enabled=true",
                    "--framework.apollo.enabled=true",
                    "--framework.config.enabled=true",
                    "--server.port=0",
                    "--spring.datasource.url=jdbc:h2:mem:conflict-both;DB_CLOSE_DELAY=-1;MODE=MySQL");
        } catch (Exception e) {
            exception = e;
        } finally {
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
        }

        // Application must fail to start when both config centers are enabled
        assertNotNull(exception, "Application should fail to start when both nacos and apollo are enabled");

        // Check the exception chain for the mutual exclusion error
        boolean foundMutualExclusion = false;
        Throwable current = exception;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Nacos and Apollo") ||
                    msg.contains("cannot be enabled simultaneously") ||
                    msg.contains("ConfigCenterMutualExclusionException") ||
                    msg.contains("mutual exclusion") ||
                    msg.contains("cannot coexist") ||
                    msg.contains("Conflict"))) {
                foundMutualExclusion = true;
                break;
            }
            // Also check exception type
            if (current.getClass().getSimpleName().contains("ConfigCenterMutualExclusion")) {
                foundMutualExclusion = true;
                break;
            }
            current = current.getCause();
        }

        assertTrue(foundMutualExclusion,
                "Failure should explain the nacos/apollo mutual exclusion: " + exception.getMessage());
    }
}
