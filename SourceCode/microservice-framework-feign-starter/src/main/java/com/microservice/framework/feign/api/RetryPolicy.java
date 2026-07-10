package com.microservice.framework.feign.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Retry policy configuration — immutable specification for Feign retry behavior.
 *
 * <p>Defines the maximum number of retry attempts, the interval between retries,
 * and the HTTP status codes and exception types that trigger a retry.</p>
 *
 * <p>Use the static factory methods to create instances:</p>
 * <pre>
 * RetryPolicy policy = RetryPolicy.of(3, 100, Set.of(502, 503));
 * </pre>
 *
 * @author Andy Yang
 */
public final class RetryPolicy {

    /** Default maximum retry attempts. */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /** Default retry interval in milliseconds. */
    private static final int DEFAULT_RETRY_INTERVAL = 100;

    /** Default HTTP status codes that trigger a retry. */
    private static final Set<Integer> DEFAULT_RETRY_ON_STATUSES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(502, 503)));

    /** Maximum number of retry attempts. */
    private final int maxRetries;

    /** Interval between retries in milliseconds. */
    private final int retryInterval;

    /** HTTP status codes that trigger a retry. */
    private final Set<Integer> retryOnStatuses;

    /** Exception class names that trigger a retry. */
    private final Set<String> retryOnExceptions;

    private RetryPolicy(int maxRetries, int retryInterval, Set<Integer> retryOnStatuses,
                        Set<String> retryOnExceptions) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "maxRetries must be non-negative, but was: " + maxRetries);
        }
        if (retryInterval < 0) {
            throw new IllegalArgumentException(
                    "retryInterval must be non-negative, but was: " + retryInterval);
        }
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
        this.retryOnStatuses = retryOnStatuses != null
                ? Collections.unmodifiableSet(new HashSet<>(retryOnStatuses))
                : Collections.emptySet();
        this.retryOnExceptions = retryOnExceptions != null
                ? Collections.unmodifiableSet(new HashSet<>(retryOnExceptions))
                : Collections.emptySet();
    }

    /**
     * Creates a retry policy with the specified parameters.
     *
     * @param maxRetries       maximum number of retry attempts (must be non-negative)
     * @param retryInterval    interval between retries in milliseconds (must be non-negative)
     * @param retryOnStatuses  HTTP status codes that trigger a retry; may be {@code null}
     * @return a new immutable {@link RetryPolicy}
     * @throws IllegalArgumentException if maxRetries or retryInterval is negative
     */
    public static RetryPolicy of(int maxRetries, int retryInterval, Set<Integer> retryOnStatuses) {
        return new RetryPolicy(maxRetries, retryInterval, retryOnStatuses, Collections.emptySet());
    }

    /**
     * Creates a retry policy with the specified parameters including exception types.
     *
     * @param maxRetries        maximum number of retry attempts (must be non-negative)
     * @param retryInterval     interval between retries in milliseconds (must be non-negative)
     * @param retryOnStatuses   HTTP status codes that trigger a retry; may be {@code null}
     * @param retryOnExceptions exception class names that trigger a retry; may be {@code null}
     * @return a new immutable {@link RetryPolicy}
     * @throws IllegalArgumentException if maxRetries or retryInterval is negative
     */
    public static RetryPolicy of(int maxRetries, int retryInterval, Set<Integer> retryOnStatuses,
                                 Set<String> retryOnExceptions) {
        return new RetryPolicy(maxRetries, retryInterval, retryOnStatuses, retryOnExceptions);
    }

    /**
     * Creates a retry policy with default values.
     *
     * <p>Defaults: maxRetries=3, retryInterval=100ms,
     * retryOnStatuses=[502,503], retryOnExceptions=[].</p>
     *
     * @return a new immutable {@link RetryPolicy} with defaults
     */
    public static RetryPolicy defaults() {
        return new RetryPolicy(DEFAULT_MAX_RETRIES, DEFAULT_RETRY_INTERVAL,
                DEFAULT_RETRY_ON_STATUSES, Collections.emptySet());
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public Set<Integer> getRetryOnStatuses() {
        return retryOnStatuses;
    }

    public Set<String> getRetryOnExceptions() {
        return retryOnExceptions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RetryPolicy)) {
            return false;
        }
        RetryPolicy other = (RetryPolicy) obj;
        return maxRetries == other.maxRetries
                && retryInterval == other.retryInterval
                && retryOnStatuses.equals(other.retryOnStatuses)
                && retryOnExceptions.equals(other.retryOnExceptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxRetries, retryInterval, retryOnStatuses, retryOnExceptions);
    }

    @Override
    public String toString() {
        return "RetryPolicy{maxRetries=" + maxRetries
                + ", retryInterval=" + retryInterval
                + ", retryOnStatuses=" + retryOnStatuses
                + ", retryOnExceptions=" + retryOnExceptions + '}';
    }
}
