package com.microservice.framework.security.api;

/**
 * Service authenticator interface for JWT and service identity verification.
 * <p>
 * Provides authentication operations including JWT token validation,
 * current user retrieval, service identity verification, and
 * service-to-service call detection.
 *
 * @author Andy Yang
 */
public interface ServiceAuthenticator {

    /**
     * Authenticates the current request by validating JWT claims.
     * <p>
     * Extracts and validates the JWT token from the current security context,
     * verifying issuer, audience, and expiration.
     *
     * @return the {@link SecurityContext} derived from the validated JWT claims
     * @throws com.microservice.framework.common.error.FrameworkException if authentication fails
     */
    SecurityContext authenticate();

    /**
     * Returns the current authenticated user's identity context.
     * <p>
     * This method extracts user information from the already-authenticated
     * security context without re-validating the token.
     *
     * @return the {@link SecurityContext} for the current user, never null
     * @throws com.microservice.framework.common.error.FrameworkException if no authenticated user is present
     */
    SecurityContext getCurrentUser();

    /**
     * Returns the current service identity from the request headers.
     * <p>
     * Extracts the service identifier from the {@code X-Service-Token} header
     * or equivalent service identity claim in the JWT.
     *
     * @return the service identifier, or null if no service identity is present
     */
    String getCurrentServiceId();

    /**
     * Determines whether the current request originates from a service-to-service call.
     * <p>
     * A service call is identified by the presence of a valid service token
     * header (e.g., {@code X-Service-Token}) or a service-level JWT claim.
     *
     * @return true if the current request is a service-to-service call
     */
    boolean isServiceCall();
}
