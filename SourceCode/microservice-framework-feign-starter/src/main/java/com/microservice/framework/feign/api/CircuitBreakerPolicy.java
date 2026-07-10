package com.microservice.framework.feign.api;

import java.util.Objects;

/**
 * Circuit breaker policy configuration — immutable specification for
 * resilience4j-style circuit breaker behavior.
 *
 * <p>Defines three states and threshold parameters that control when
 * the circuit transitions between states:</p>
 * <ul>
 *   <li>{@code CLOSED} — normal operation, requests pass through; transitions
 *       to OPEN when failure rate or slow-call rate exceeds thresholds.</li>
 *   <li>{@code OPEN} — all requests are rejected immediately; transitions
 *       to HALF_OPEN after a configurable wait duration.</li>
 *   <li>{@code HALF_OPEN} — a limited number of permitted requests are allowed;
 *       transitions back to CLOSED if thresholds are met, or to OPEN if not.</li>
 * </ul>
 *
 * <p>Use the static factory methods to create instances:</p>
 * <pre>
 * CircuitBreakerPolicy policy = CircuitBreakerPolicy.of(50f, 3000L, 100f);
 * </pre>
 *
 * @author Andy Yang
 */
public final class CircuitBreakerPolicy {

    /**
     * Circuit breaker state enumeration.
     */
    public enum State {

        /** Normal operation — requests flow through. */
        CLOSED,

        /** Circuit tripped — all requests are rejected. */
        OPEN,

        /** Probing state — limited requests are permitted to test recovery. */
        HALF_OPEN
    }

    /** Failure rate threshold as a percentage (0-100). */
    private final float failureRateThreshold;

    /** Duration in milliseconds that qualifies a call as "slow". */
    private final long slowCallDuration;

    /** Slow-call rate threshold as a percentage (0-100). */
    private final float slowCallRateThreshold;

    private CircuitBreakerPolicy(float failureRateThreshold, long slowCallDuration,
                                 float slowCallRateThreshold) {
        if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
            throw new IllegalArgumentException(
                    "failureRateThreshold must be between 1 and 100, but was: " + failureRateThreshold);
        }
        if (slowCallDuration <= 0) {
            throw new IllegalArgumentException(
                    "slowCallDuration must be positive, but was: " + slowCallDuration);
        }
        if (slowCallRateThreshold <= 0 || slowCallRateThreshold > 100) {
            throw new IllegalArgumentException(
                    "slowCallRateThreshold must be between 1 and 100, but was: " + slowCallRateThreshold);
        }
        this.failureRateThreshold = failureRateThreshold;
        this.slowCallDuration = slowCallDuration;
        this.slowCallRateThreshold = slowCallRateThreshold;
    }

    /**
     * Creates a circuit breaker policy with the specified thresholds.
     *
     * @param failureRateThreshold   failure rate percentage threshold (1-100)
     * @param slowCallDuration       slow-call duration threshold in milliseconds
     * @param slowCallRateThreshold  slow-call rate percentage threshold (1-100)
     * @return a new immutable {@link CircuitBreakerPolicy}
     * @throws IllegalArgumentException if any parameter is out of range
     */
    public static CircuitBreakerPolicy of(float failureRateThreshold, long slowCallDuration,
                                          float slowCallRateThreshold) {
        return new CircuitBreakerPolicy(failureRateThreshold, slowCallDuration, slowCallRateThreshold);
    }

    /**
     * Creates a circuit breaker policy with default thresholds.
     *
     * <p>Defaults: failureRateThreshold=50, slowCallDuration=3000ms,
     * slowCallRateThreshold=100.</p>
     *
     * @return a new immutable {@link CircuitBreakerPolicy} with defaults
     */
    public static CircuitBreakerPolicy defaults() {
        return new CircuitBreakerPolicy(50f, 3000L, 100f);
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public long getSlowCallDuration() {
        return slowCallDuration;
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CircuitBreakerPolicy)) {
            return false;
        }
        CircuitBreakerPolicy other = (CircuitBreakerPolicy) obj;
        return Float.compare(failureRateThreshold, other.failureRateThreshold) == 0
                && slowCallDuration == other.slowCallDuration
                && Float.compare(slowCallRateThreshold, other.slowCallRateThreshold) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(failureRateThreshold, slowCallDuration, slowCallRateThreshold);
    }

    @Override
    public String toString() {
        return "CircuitBreakerPolicy{failureRateThreshold=" + failureRateThreshold
                + ", slowCallDuration=" + slowCallDuration
                + ", slowCallRateThreshold=" + slowCallRateThreshold + '}';
    }
}
