package com.microservice.framework.feign.api;

import com.microservice.framework.common.context.ContextSnapshot;

import java.util.Map;

/**
 * Feign context propagator — copies {@link com.microservice.framework.common.context.FrameworkContext}
 * keys to outgoing Feign requests and restores them from incoming responses.
 *
 * <p>The propagator works in two phases:</p>
 * <ul>
 *   <li>{@link #propagate(ContextSnapshot)} — extracts the specified context keys
 *       (requestId, traceId, userId, etc.) from the current thread's context snapshot
 *       and returns them as a map suitable for Feign request headers.</li>
 *   <li>{@link #restore(Map)} — takes a map of incoming headers and writes them
 *       back into the current thread's context, enabling downstream services to
 *       continue the propagation chain.</li>
 * </ul>
 *
 * @author Andy Yang
 */
public interface FeignContextPropagator {

    /**
     * Propagates context entries from the given snapshot into Feign request headers.
     *
     * <p>Only the keys configured in
     * {@code framework.feign.context.propagate-keys} are included
     * (typically requestId, traceId, userId).</p>
     *
     * @param snapshot the current thread's context snapshot
     * @return a map of header name-value pairs to attach to the Feign request;
     *         never {@code null}, but may be empty if no keys are configured
     */
    Map<String, String> propagate(ContextSnapshot snapshot);

    /**
     * Restores context entries from incoming headers into the current thread's context.
     *
     * <p>This is used when receiving a response or in a downstream service to
     * continue the propagation chain.</p>
     *
     * @param headers the incoming headers containing context values
     */
    void restore(Map<String, String> headers);
}
