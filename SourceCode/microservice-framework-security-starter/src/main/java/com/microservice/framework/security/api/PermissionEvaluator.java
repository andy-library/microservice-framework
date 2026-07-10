package com.microservice.framework.security.api;

import java.util.Collection;

/**
 * Permission evaluator interface for fine-grained permission checking.
 * <p>
 * Provides role-based and permission-based authorization checks,
 * supporting both coarse-grained role verification and
 * fine-grained interface permission control.
 *
 * @author Andy Yang
 */
public interface PermissionEvaluator {

    /**
     * Checks whether the current user has the specified permission.
     * <p>
     * Permission strings follow the format: {@code resource:action}
     * (e.g., {@code order:create}, {@code user:delete}).
     *
     * @param permission the permission to check, never null
     * @return true if the current user possesses the specified permission
     */
    boolean hasPermission(String permission);

    /**
     * Checks whether the current user has the specified role.
     * <p>
     * Role names are case-sensitive and must match exactly
     * the roles declared in JWT claims or role configuration.
     *
     * @param role the role to check, never null
     * @return true if the current user possesses the specified role
     */
    boolean hasRole(String role);

    /**
     * Checks whether the current user has any of the specified roles.
     * <p>
     * Returns true if the user possesses at least one role from the
     * provided collection.
     *
     * @param roles the roles to check, must contain at least one role
     * @return true if the current user possesses at least one of the specified roles
     */
    boolean hasAnyRole(Collection<String> roles);
}
