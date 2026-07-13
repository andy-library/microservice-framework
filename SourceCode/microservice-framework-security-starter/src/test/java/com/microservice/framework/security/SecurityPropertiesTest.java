package com.microservice.framework.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityProperties default value tests.
 * <p>
 * Verifies that all nested configuration properties have
 * correct default values and setter overrides work properly.
 *
 * @author Andy Yang
 */
class SecurityPropertiesTest {

    private final SecurityProperties properties = new SecurityProperties();

    // ======================================================================
    // Jwt defaults
    // ======================================================================

    @Nested
    @DisplayName("Jwt 默认值")
    class JwtDefaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefault() {
            assertThat(properties.getJwt().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("issuerUri 默认应为 null")
        void issuerUriDefault() {
            assertThat(properties.getJwt().getIssuerUri()).isNull();
        }

        @Test
        @DisplayName("jwkSetUri 默认应为 null")
        void jwkSetUriDefault() {
            assertThat(properties.getJwt().getJwkSetUri()).isNull();
        }

        @Test
        @DisplayName("audience 默认应为 null")
        void audienceDefault() {
            assertThat(properties.getJwt().getAudience()).isNull();
        }

        @Test
        @DisplayName("claimRolesKey 默认应为 'roles'")
        void claimRolesKeyDefault() {
            assertThat(properties.getJwt().getClaimRolesKey()).isEqualTo("roles");
        }
    }

    // ======================================================================
    // ServiceIdentity defaults
    // ======================================================================

    @Nested
    @DisplayName("ServiceIdentity 默认值")
    class ServiceIdentityDefaults {

        @Test
        @DisplayName("enabled 默认应为 false，避免未配置凭据时匿名服务身份")
        void enabledDefault() {
            assertThat(properties.getServiceIdentity().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("serviceTokenHeader 默认应为 'X-Service-Token'")
        void serviceTokenHeaderDefault() {
            assertThat(properties.getServiceIdentity().getServiceTokenHeader()).isEqualTo("X-Service-Token");
        }
    }

    // ======================================================================
    // Permission defaults
    // ======================================================================

    @Nested
    @DisplayName("Permission 默认值")
    class PermissionDefaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefault() {
            assertThat(properties.getPermission().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("permissionClaimKey 默认应为 'permissions'")
        void permissionClaimKeyDefault() {
            assertThat(properties.getPermission().getPermissionClaimKey()).isEqualTo("permissions");
        }
    }

    // ======================================================================
    // Cors defaults
    // ======================================================================

    @Nested
    @DisplayName("Cors 默认值")
    class CorsDefaults {

        @Test
        @DisplayName("enabled 默认应为 true")
        void enabledDefault() {
            assertThat(properties.getCors().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("allowedOrigins 默认应包含 '*'")
        void allowedOriginsDefault() {
            assertThat(properties.getCors().getAllowedOrigins()).containsExactly("*");
        }

        @Test
        @DisplayName("allowedMethods 默认应包含常用 HTTP 方法")
        void allowedMethodsDefault() {
            assertThat(properties.getCors().getAllowedMethods())
                    .containsExactly("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }

        @Test
        @DisplayName("maxAge 默认应为 3600")
        void maxAgeDefault() {
            assertThat(properties.getCors().getMaxAge()).isEqualTo(3600);
        }
    }

    // ======================================================================
    // Path defaults
    // ======================================================================

    @Nested
    @DisplayName("Path 默认值")
    class PathDefaults {

        @Test
        @DisplayName("permitPaths 默认仅包含健康和就绪探针")
        void permitPathsDefault() {
            assertThat(properties.getPath().getPermitPaths())
                    .containsExactly(
                            "/actuator/health",
                            "/actuator/health/liveness",
                            "/actuator/health/readiness");
        }

        @Test
        @DisplayName("denyPaths 默认应为空列表")
        void denyPathsDefault() {
            assertThat(properties.getPath().getDenyPaths()).isEmpty();
        }
    }

    // ======================================================================
    // Setter overrides
    // ======================================================================

    @Nested
    @DisplayName("Setter 覆盖")
    class SetterOverrides {

        @Test
        @DisplayName("自定义 jwt.enabled 应生效")
        void jwtEnabledOverride() {
            properties.getJwt().setEnabled(false);
            assertThat(properties.getJwt().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("自定义 jwt.issuerUri 应生效")
        void jwtIssuerUriOverride() {
            properties.getJwt().setIssuerUri("https://keycloak.example.com/realms/my-realm");
            assertThat(properties.getJwt().getIssuerUri()).isEqualTo("https://keycloak.example.com/realms/my-realm");
        }

        @Test
        @DisplayName("自定义 jwt.claimRolesKey 应生效")
        void jwtClaimRolesKeyOverride() {
            properties.getJwt().setClaimRolesKey("realm_access.roles");
            assertThat(properties.getJwt().getClaimRolesKey()).isEqualTo("realm_access.roles");
        }

        @Test
        @DisplayName("自定义 serviceIdentity.serviceTokenHeader 应生效")
        void serviceTokenHeaderOverride() {
            properties.getServiceIdentity().setServiceTokenHeader("X-Custom-Token");
            assertThat(properties.getServiceIdentity().getServiceTokenHeader()).isEqualTo("X-Custom-Token");
        }

        @Test
        @DisplayName("自定义 permission.permissionClaimKey 应生效")
        void permissionClaimKeyOverride() {
            properties.getPermission().setPermissionClaimKey("custom_permissions");
            assertThat(properties.getPermission().getPermissionClaimKey()).isEqualTo("custom_permissions");
        }

        @Test
        @DisplayName("自定义 cors.allowedOrigins 应生效")
        void corsAllowedOriginsOverride() {
            properties.getCors().setAllowedOrigins(java.util.List.of("https://app.example.com"));
            assertThat(properties.getCors().getAllowedOrigins()).containsExactly("https://app.example.com");
        }

        @Test
        @DisplayName("自定义 cors.maxAge 应生效")
        void corsMaxAgeOverride() {
            properties.getCors().setMaxAge(7200);
            assertThat(properties.getCors().getMaxAge()).isEqualTo(7200);
        }

        @Test
        @DisplayName("自定义 path.permitPaths 应生效")
        void permitPathsOverride() {
            properties.getPath().setPermitPaths(java.util.List.of("/public/**", "/api/docs"));
            assertThat(properties.getPath().getPermitPaths()).containsExactly("/public/**", "/api/docs");
        }

        @Test
        @DisplayName("自定义 path.denyPaths 应生效")
        void denyPathsOverride() {
            properties.getPath().setDenyPaths(java.util.List.of("/internal/**"));
            assertThat(properties.getPath().getDenyPaths()).containsExactly("/internal/**");
        }
    }
}
