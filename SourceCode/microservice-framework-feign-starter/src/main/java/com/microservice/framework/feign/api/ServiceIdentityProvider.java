package com.microservice.framework.feign.api;

import java.util.Map;

/**
 * Service identity provider for inter-service authentication.
 *
 * <p>Provides the calling service's identity information — service ID,
 * service token, and additional headers — to be attached to outgoing
 * Feign requests for inter-service authentication and authorization.</p>
 *
 * <p>Implementations are registered as Spring beans and picked up by
 * {@link com.microservice.framework.feign.autoconfigure.FeignAutoConfiguration}
 * to inject identity headers into every Feign request.</p>
 *
 * @author Andy Yang
 */
public interface ServiceIdentityProvider {

    /**
     * Returns the logical service identifier (e.g., "order-service").
     *
     * @return the service ID, never {@code null}
     */
    String getServiceId();

    /**
     * Returns the authentication token for inter-service calls.
     *
     * <p>The token may be a JWT, an API key, or any credential format
     * recognized by the target service's authentication layer.</p>
     *
     * @return the service token, never {@code null}
     */
    String getServiceToken();

    /**
     * Returns additional headers to attach to outgoing Feign requests.
     *
     * <p>These headers are merged with the identity headers
     * (service ID and token) before being applied to the request.</p>
     *
     * @return a map of header name-value pairs; never {@code null},
     *         but may be empty
     */
    Map<String, String> getHeaders();
}
