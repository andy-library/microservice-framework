package com.microservice.framework.security.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable value object representing the authenticated security context.
 * <p>
 * Carries the identity and authorization information extracted from
 * JWT claims: user ID, roles, permissions, service identity,
 * and service call indicator.
 * <p>
 * Instances are created via static factory methods and are
 * never mutated after construction.
 *
 * @author Andy Yang
 */
public final class SecurityContext {

    private final String userId;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final String serviceId;
    private final boolean serviceCall;

    /**
     * Creates a SecurityContext from JWT claims.
     * <p>
     * Extracts standard fields from the claims map:
     * <ul>
     *   <li>{@code sub} → userId</li>
     *   <li>{@code roles} or the configured claim key → roles set</li>
     *   <li>{@code permissions} or the configured claim key → permissions set</li>
     *   <li>{@code service_id} → serviceId</li>
     *   <li>{@code service_call} → serviceCall flag</li>
     * </ul>
     *
     * @param claims          the JWT claims map, must not be null
     * @param rolesClaimKey   the JWT claim key for roles (default: "roles")
     * @param permissionsClaimKey the JWT claim key for permissions (default: "permissions")
     * @return a new SecurityContext derived from the claims
     */
    public static SecurityContext fromClaims(Map<String, Object> claims,
                                             String rolesClaimKey,
                                             String permissionsClaimKey) {
        Objects.requireNonNull(claims, "claims must not be null");

        String userId = extractString(claims, "sub");
        Set<String> roles = extractSet(claims, rolesClaimKey != null ? rolesClaimKey : "roles");
        Set<String> permissions = extractSet(claims, permissionsClaimKey != null ? permissionsClaimKey : "permissions");
        String serviceId = extractString(claims, "service_id");
        boolean serviceCall = extractBoolean(claims, "service_call");

        return new SecurityContext(userId, roles, permissions, serviceId, serviceCall);
    }

    /**
     * Creates a SecurityContext from explicit field values.
     *
     * @param userId      the user identifier, may be null for service calls
     * @param roles       the user's roles, may be empty
     * @param permissions the user's permissions, may be empty
     * @param serviceId   the service identifier, may be null for user calls
     * @param serviceCall whether this context represents a service-to-service call
     * @return a new SecurityContext
     */
    public static SecurityContext of(String userId,
                                     Set<String> roles,
                                     Set<String> permissions,
                                     String serviceId,
                                     boolean serviceCall) {
        return new SecurityContext(
                userId,
                roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet(),
                permissions != null ? Collections.unmodifiableSet(new HashSet<>(permissions)) : Collections.emptySet(),
                serviceId,
                serviceCall
        );
    }

    /**
     * Private constructor — use {@link #fromClaims} or {@link #of}.
     */
    private SecurityContext(String userId,
                           Set<String> roles,
                           Set<String> permissions,
                           String serviceId,
                           boolean serviceCall) {
        this.userId = userId;
        this.roles = roles;
        this.permissions = permissions;
        this.serviceId = serviceId;
        this.serviceCall = serviceCall;
    }

    /**
     * Returns the authenticated user's identifier.
     *
     * @return the user ID, or null if this is a service call without user context
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the authenticated user's roles.
     *
     * @return an unmodifiable set of role names, never null
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Returns the authenticated user's permissions.
     *
     * @return an unmodifiable set of permission strings, never null
     */
    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * Returns the service identifier for service-to-service calls.
     *
     * @return the service ID, or null if this is a direct user call
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Returns whether this context represents a service-to-service call.
     *
     * @return true if the call originates from another service
     */
    public boolean isServiceCall() {
        return serviceCall;
    }

    /**
     * Checks whether the user possesses the specified role.
     *
     * @param role the role to check
     * @return true if the role is present
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Checks whether the user possesses the specified permission.
     *
     * @param permission the permission to check
     * @return true if the permission is present
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityContext that = (SecurityContext) o;
        return serviceCall == that.serviceCall
                && Objects.equals(userId, that.userId)
                && Objects.equals(roles, that.roles)
                && Objects.equals(permissions, that.permissions)
                && Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roles, permissions, serviceId, serviceCall);
    }

    @Override
    public String toString() {
        return "SecurityContext{" +
                "userId='" + userId + '\'' +
                ", roles=" + roles +
                ", permissions=" + permissions +
                ", serviceId='" + serviceId + '\'' +
                ", serviceCall=" + serviceCall +
                '}';
    }

    // ======================================================================
    // Private helper methods for claims extraction
    // ======================================================================

    private static String extractString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value != null ? value.toString() : null;
    }

    private static Set<String> extractSet(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return Collections.emptySet();
        }
        if (value instanceof Collection) {
            Set<String> result = new HashSet<>();
            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return Collections.unmodifiableSet(result);
        }
        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.isEmpty()) {
                return Collections.emptySet();
            }
            Set<String> result = new HashSet<>();
            for (String part : strValue.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return Collections.unmodifiableSet(result);
        }
        return Collections.emptySet();
    }

    private static boolean extractBoolean(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}
