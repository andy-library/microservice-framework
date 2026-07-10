package com.microservice.framework.web.context;

/**
 * ThreadLocal-based request ID storage.
 * <p>
 * Provides static access to the current request's ID within the
 * processing thread. The request ID is set by {@link RequestIdFilter}
 * at the beginning of each request and cleared at the end.
 * <p>
 * Typical usage:
 * <pre>
 * String requestId = RequestIdContext.get();
 * ApiResponse&lt;T&gt; response = ApiResponse.success(data).withRequestId(requestId);
 * </pre>
 *
 * @author Andy Yang
 */
public final class RequestIdContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestIdContext() {}

    /**
     * Get the current request ID.
     *
     * @return the request ID, or null if not set
     */
    public static String get() {
        return REQUEST_ID.get();
    }

    /**
     * Set the current request ID.
     *
     * @param requestId the request ID to store
     */
    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    /**
     * Remove the current request ID.
     * <p>
     * Must be called at the end of each request to prevent ThreadLocal
     * leakage in thread-pool environments.
     */
    public static void remove() {
        REQUEST_ID.remove();
    }
}
