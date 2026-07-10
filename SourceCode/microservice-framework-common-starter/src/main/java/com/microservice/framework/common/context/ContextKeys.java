package com.microservice.framework.common.context;

/**
 * Standard context key constants for request-scoped key-value propagation.
 *
 * <p>These keys are used with {@link FrameworkContext} and {@link ContextSnapshot}
 * to carry metadata across service boundaries and threading layers.</p>
 *
 * @author Andy Yang
 */
public final class ContextKeys {

    /** Unique identifier for the current request. */
    public static final String REQUEST_ID = "requestId";

    /** Identifier of the authenticated user. */
    public static final String USER_ID = "userId";

    /** Identifier of the tenant (multi-tenancy). */
    public static final String TENANT_ID = "tenantId";

    /** Logical identity of the originating service. */
    public static final String SERVICE_IDENTITY = "serviceIdentity";

    /** Distributed tracing trace identifier. */
    public static final String TRACE_ID = "traceId";

    /** Distributed tracing span identifier. */
    public static final String SPAN_ID = "spanId";

    /**
     * Prevent instantiation — this class holds only constants.
     */
    private ContextKeys() {}
}
