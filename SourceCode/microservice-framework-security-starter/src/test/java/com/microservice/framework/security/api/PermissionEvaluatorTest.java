package com.microservice.framework.security.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PermissionEvaluator interface contract tests.
 * <p>
 * Verifies that the interface methods are correctly defined
 * and that a stub implementation fulfills the contract.
 *
 * @author Andy Yang
 */
class PermissionEvaluatorTest {

    // ======================================================================
    // Interface contract verification
    // ======================================================================

    @Nested
    @DisplayName("hasPermission() 契约验证")
    class HasPermissionContract {

        @Test
        @DisplayName("hasPermission() 应返回 true 当权限存在")
        void hasPermissionShouldReturnTrueWhenPresent() {
            PermissionEvaluator evaluator = new StubEvaluator();
            assertThat(evaluator.hasPermission("order:create")).isTrue();
        }

        @Test
        @DisplayName("hasPermission() 应返回 false 当权限不存在")
        void hasPermissionShouldReturnFalseWhenAbsent() {
            PermissionEvaluator evaluator = new StubEvaluator();
            assertThat(evaluator.hasPermission("unknown:action")).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRole() 契约验证")
    class HasRoleContract {

        @Test
        @DisplayName("hasRole() 应返回 true 当角色存在")
        void hasRoleShouldReturnTrueWhenPresent() {
            PermissionEvaluator evaluator = new StubEvaluator();
            assertThat(evaluator.hasRole("admin")).isTrue();
        }

        @Test
        @DisplayName("hasRole() 应返回 false 当角色不存在")
        void hasRoleShouldReturnFalseWhenAbsent() {
            PermissionEvaluator evaluator = new StubEvaluator();
            assertThat(evaluator.hasRole("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAnyRole() 契约验证")
    class HasAnyRoleContract {

        @Test
        @DisplayName("hasAnyRole() 应返回 true 当至少一个角色存在")
        void hasAnyRoleShouldReturnTrueWhenAtLeastOneRoleMatches() {
            PermissionEvaluator evaluator = new StubEvaluator();
            Collection<String> roles = Arrays.asList("unknown", "admin", "missing");
            assertThat(evaluator.hasAnyRole(roles)).isTrue();
        }

        @Test
        @DisplayName("hasAnyRole() 应返回 false 当所有角色都不存在")
        void hasAnyRoleShouldReturnFalseWhenNoRoleMatches() {
            PermissionEvaluator evaluator = new StubEvaluator();
            Collection<String> roles = Arrays.asList("unknown", "missing");
            assertThat(evaluator.hasAnyRole(roles)).isFalse();
        }
    }

    // ======================================================================
    // Stub implementation for contract testing
    // ======================================================================

    private static class StubEvaluator implements PermissionEvaluator {

        private final SecurityContext context = SecurityContext.of(
                "stub-user",
                java.util.Set.of("admin", "user"),
                java.util.Set.of("order:create", "order:delete"),
                null, false
        );

        @Override
        public boolean hasPermission(String permission) {
            return context.hasPermission(permission);
        }

        @Override
        public boolean hasRole(String role) {
            return context.hasRole(role);
        }

        @Override
        public boolean hasAnyRole(Collection<String> roles) {
            for (String role : roles) {
                if (context.hasRole(role)) {
                    return true;
                }
            }
            return false;
        }
    }
}
