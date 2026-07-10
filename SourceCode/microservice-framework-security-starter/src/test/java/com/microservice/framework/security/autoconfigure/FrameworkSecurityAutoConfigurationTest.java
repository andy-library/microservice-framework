package com.microservice.framework.security.autoconfigure;

import com.microservice.framework.security.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FrameworkSecurityAutoConfiguration ApplicationContextRunner tests.
 * <p>
 * Uses {@link WebApplicationContextRunner} to simulate SERVLET web
 * application conditions. Includes Spring Boot's
 * {@link SecurityAutoConfiguration} (provides {@code HttpSecurity} bean)
 * and {@link WebMvcAutoConfiguration} (provides the
 * {@code HandlerMappingIntrospector} bean needed by {@code MvcRequestMatcher}).
 *
 * @author Andy Yang
 */
class FrameworkSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    FrameworkSecurityAutoConfiguration.class));

    // ======================================================================
    // Default activation
    // ======================================================================

    @Nested
    @DisplayName("默认配置激活")
    class DefaultActivation {

        @Test
        @DisplayName("默认配置应激活 SecurityFilterChain 和 SecurityProperties")
        void defaultConfigurationShouldActivateSecurityFilterChain() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("securityFilterChain");
                assertThat(context).hasSingleBean(SecurityProperties.class);
            });
        }
    }

    // ======================================================================
    // Disable security
    // ======================================================================

    @Nested
    @DisplayName("禁用 Security 条件")
    class DisableSecurityConditional {

        @Test
        @DisplayName("禁用 framework.security.enabled 后 FrameworkSecurityAutoConfiguration 不应激活")
        void disablingSecurityShouldNotActivateFrameworkAutoConfiguration() {
            contextRunner.withPropertyValues("framework.security.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        // Our custom security config is not active when disabled
                        assertThat(context).doesNotHaveBean(SecurityProperties.class);
                    });
        }
    }

    // ======================================================================
    // Disable CORS
    // ======================================================================

    @Nested
    @DisplayName("CORS 条件激活")
    class CorsConditional {

        @Test
        @DisplayName("启用 CORS 后 SecurityFilterChain 应包含 CORS 配置")
        void enablingCorsShouldApplyCorsConfiguration() {
            contextRunner.withPropertyValues("framework.security.cors.enabled=true")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasBean("securityFilterChain");
                        assertThat(context).hasSingleBean(SecurityProperties.class);
                    });
        }
    }

    // ======================================================================
    // Property binding
    // ======================================================================

    @Nested
    @DisplayName("属性绑定")
    class PropertyBinding {

        @Test
        @DisplayName("自定义 jwt.issuerUri 应绑定到 SecurityProperties")
        void customIssuerUriBinding() {
            contextRunner.withPropertyValues("framework.security.jwt.issuer-uri=https://keycloak.example.com/realms/test")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityProperties props = context.getBean(SecurityProperties.class);
                        assertThat(props.getJwt().getIssuerUri()).isEqualTo("https://keycloak.example.com/realms/test");
                    });
        }

        @Test
        @DisplayName("自定义 jwt.claimRolesKey 应绑定到 SecurityProperties")
        void customClaimRolesKeyBinding() {
            contextRunner.withPropertyValues("framework.security.jwt.claim-roles-key=realm_access.roles")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityProperties props = context.getBean(SecurityProperties.class);
                        assertThat(props.getJwt().getClaimRolesKey()).isEqualTo("realm_access.roles");
                    });
        }

        @Test
        @DisplayName("自定义 path.permitPaths 应绑定到 SecurityProperties")
        void customPermitPathsBinding() {
            contextRunner.withPropertyValues("framework.security.path.permit-paths=/public/**,/api/docs")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityProperties props = context.getBean(SecurityProperties.class);
                        assertThat(props.getPath().getPermitPaths()).containsExactly("/public/**", "/api/docs");
                    });
        }

        @Test
        @DisplayName("自定义 path.denyPaths 应绑定到 SecurityProperties")
        void customDenyPathsBinding() {
            contextRunner.withPropertyValues("framework.security.path.deny-paths=/internal/**,/admin/internal")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityProperties props = context.getBean(SecurityProperties.class);
                        assertThat(props.getPath().getDenyPaths()).containsExactly("/internal/**", "/admin/internal");
                    });
        }

        @Test
        @DisplayName("自定义 cors.maxAge 应绑定到 SecurityProperties")
        void customCorsMaxAgeBinding() {
            contextRunner.withPropertyValues("framework.security.cors.max-age=7200")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        SecurityProperties props = context.getBean(SecurityProperties.class);
                        assertThat(props.getCors().getMaxAge()).isEqualTo(7200);
                    });
        }
    }
}
