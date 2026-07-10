package com.microservice.framework.security.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityContext immutable value object tests.
 * <p>
 * Verifies creation via factory methods, getters,
 * permission/role checking, and immutability guarantees.
 *
 * @author Andy Yang
 */
class SecurityContextTest {

    // ======================================================================
    // Creation via of()
    // ======================================================================

    @Nested
    @DisplayName("of() 工厂方法创建")
    class CreationViaOf {

        @Test
        @DisplayName("of() 应创建包含所有字段的 SecurityContext")
        void ofShouldCreateSecurityContextWithAllFields() {
            Set<String> roles = Set.of("admin", "user");
            Set<String> permissions = Set.of("order:create", "order:delete");

            SecurityContext context = SecurityContext.of(
                    "user-42", roles, permissions, "service-1", true);

            assertThat(context.getUserId()).isEqualTo("user-42");
            assertThat(context.getRoles()).containsExactlyInAnyOrder("admin", "user");
            assertThat(context.getPermissions()).containsExactlyInAnyOrder("order:create", "order:delete");
            assertThat(context.getServiceId()).isEqualTo("service-1");
            assertThat(context.isServiceCall()).isTrue();
        }

        @Test
        @DisplayName("of() 应创建用户调用（非 service call）的 SecurityContext")
        void ofShouldCreateUserCallContext() {
            SecurityContext context = SecurityContext.of(
                    "user-1", Set.of("viewer"), Set.of("report:read"), null, false);

            assertThat(context.getUserId()).isEqualTo("user-1");
            assertThat(context.getServiceId()).isNull();
            assertThat(context.isServiceCall()).isFalse();
        }

        @Test
        @DisplayName("of() 应处理 null roles 和 permissions")
        void ofShouldHandleNullRolesAndPermissions() {
            SecurityContext context = SecurityContext.of("user-1", null, null, null, false);

            assertThat(context.getRoles()).isEmpty();
            assertThat(context.getPermissions()).isEmpty();
        }
    }

    // ======================================================================
    // Creation via fromClaims()
    // ======================================================================

    @Nested
    @DisplayName("fromClaims() JWT claims 工厂方法创建")
    class CreationViaFromClaims {

        @Test
        @DisplayName("fromClaims() 应从 JWT claims 提取所有字段")
        void fromClaimsShouldExtractAllFields() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-42");
            claims.put("roles", java.util.List.of("admin", "user"));
            claims.put("permissions", java.util.List.of("order:create"));
            claims.put("service_id", "svc-order");
            claims.put("service_call", true);

            SecurityContext context = SecurityContext.fromClaims(claims, "roles", "permissions");

            assertThat(context.getUserId()).isEqualTo("user-42");
            assertThat(context.getRoles()).containsExactlyInAnyOrder("admin", "user");
            assertThat(context.getPermissions()).containsExactlyInAnyOrder("order:create");
            assertThat(context.getServiceId()).isEqualTo("svc-order");
            assertThat(context.isServiceCall()).isTrue();
        }

        @Test
        @DisplayName("fromClaims() 应使用默认 claim key 当传入 null")
        void fromClaimsShouldUseDefaultKeysWhenNull() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-1");
            claims.put("roles", java.util.List.of("viewer"));
            claims.put("permissions", java.util.List.of("report:read"));

            SecurityContext context = SecurityContext.fromClaims(claims, null, null);

            assertThat(context.getUserId()).isEqualTo("user-1");
            assertThat(context.getRoles()).containsExactly("viewer");
            assertThat(context.getPermissions()).containsExactly("report:read");
        }

        @Test
        @DisplayName("fromClaims() 应处理字符串逗号分隔的 roles")
        void fromClaimsShouldHandleCommaSeparatedRoles() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "user-1");
            claims.put("roles", "admin, user, editor");

            SecurityContext context = SecurityContext.fromClaims(claims, "roles", "permissions");

            assertThat(context.getRoles()).containsExactlyInAnyOrder("admin", "user", "editor");
        }

        @Test
        @DisplayName("fromClaims() 应处理空 claims")
        void fromClaimsShouldHandleEmptyClaims() {
            Map<String, Object> claims = new HashMap<>();

            SecurityContext context = SecurityContext.fromClaims(claims, "roles", "permissions");

            assertThat(context.getUserId()).isNull();
            assertThat(context.getRoles()).isEmpty();
            assertThat(context.getPermissions()).isEmpty();
            assertThat(context.getServiceId()).isNull();
            assertThat(context.isServiceCall()).isFalse();
        }
    }

    // ======================================================================
    // Getters
    // ======================================================================

    @Nested
    @DisplayName("Getter 方法")
    class GetterTests {

        @Test
        @DisplayName("getUserId 应返回正确的用户 ID")
        void getUserIdShouldReturnCorrectUserId() {
            SecurityContext context = SecurityContext.of("user-42", Set.of(), Set.of(), null, false);
            assertThat(context.getUserId()).isEqualTo("user-42");
        }

        @Test
        @DisplayName("getRoles 应返回不可修改的角色集合")
        void getRolesShouldReturnUnmodifiableSet() {
            Set<String> roles = new HashSet<>(Set.of("admin"));
            SecurityContext context = SecurityContext.of("user-1", roles, Set.of(), null, false);

            assertThatThrownBy(() -> context.getRoles().add("new-role"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getPermissions 应返回不可修改的权限集合")
        void getPermissionsShouldReturnUnmodifiableSet() {
            Set<String> permissions = new HashSet<>(Set.of("order:create"));
            SecurityContext context = SecurityContext.of("user-1", Set.of(), permissions, null, false);

            assertThatThrownBy(() -> context.getPermissions().add("new-perm"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ======================================================================
    // Role and permission checking
    // ======================================================================

    @Nested
    @DisplayName("角色和权限检查")
    class RoleAndPermissionChecking {

        @Test
        @DisplayName("hasRole 应返回 true 当角色存在")
        void hasRoleShouldReturnTrueWhenRoleExists() {
            SecurityContext context = SecurityContext.of("user-1", Set.of("admin", "user"), Set.of(), null, false);
            assertThat(context.hasRole("admin")).isTrue();
            assertThat(context.hasRole("unknown")).isFalse();
        }

        @Test
        @DisplayName("hasPermission 应返回 true 当权限存在")
        void hasPermissionShouldReturnTrueWhenPermissionExists() {
            SecurityContext context = SecurityContext.of("user-1", Set.of(), Set.of("order:create", "order:delete"), null, false);
            assertThat(context.hasPermission("order:create")).isTrue();
            assertThat(context.hasPermission("unknown:action")).isFalse();
        }
    }

    // ======================================================================
    // Immutability and equality
    // ======================================================================

    @Nested
    @DisplayName("不可变性和相等性")
    class ImmutabilityAndEquality {

        @Test
        @DisplayName("原始 roles 修改不应影响 SecurityContext")
        void originalRolesModificationShouldNotAffectContext() {
            Set<String> roles = new HashSet<>(Set.of("admin"));
            SecurityContext context = SecurityContext.of("user-1", roles, Set.of(), null, false);
            roles.add("user");

            assertThat(context.getRoles()).containsExactly("admin");
            assertThat(context.getRoles()).doesNotContain("user");
        }

        @Test
        @DisplayName("相等 SecurityContext 应满足 equals 和 hashCode")
        void equalContextsShouldMatchEqualsAndHashCode() {
            SecurityContext ctx1 = SecurityContext.of("user-1", Set.of("admin"), Set.of("order:create"), null, false);
            SecurityContext ctx2 = SecurityContext.of("user-1", Set.of("admin"), Set.of("order:create"), null, false);

            assertThat(ctx1).isEqualTo(ctx2);
            assertThat(ctx1.hashCode()).isEqualTo(ctx2.hashCode());
        }

        @Test
        @DisplayName("不同 SecurityContext 不应相等")
        void differentContextsShouldNotBeEqual() {
            SecurityContext ctx1 = SecurityContext.of("user-1", Set.of("admin"), Set.of(), null, false);
            SecurityContext ctx2 = SecurityContext.of("user-2", Set.of("admin"), Set.of(), null, false);

            assertThat(ctx1).isNotEqualTo(ctx2);
        }

        @Test
        @DisplayName("toString 应包含所有字段")
        void toStringShouldContainAllFields() {
            SecurityContext context = SecurityContext.of("user-1", Set.of("admin"), Set.of("order:create"), "svc-1", true);
            String str = context.toString();

            assertThat(str).contains("user-1");
            assertThat(str).contains("admin");
            assertThat(str).contains("order:create");
            assertThat(str).contains("svc-1");
            assertThat(str).contains("serviceCall=true");
        }
    }
}
