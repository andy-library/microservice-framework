package com.microservice.framework.common.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lightweight, request-scoped context for key-value propagation.
 *
 * <p>Designed for {@link ThreadLocal} usage — this is NOT a Spring bean.
 * A new instance is created per request via the static factory methods
 * and stored in a thread-local adapter.</p>
 *
 * <p>Mutation methods return the context itself to enable fluent chaining:</p>
 * <pre>
 * FrameworkContext.create()
 *     .put(ContextKeys.REQUEST_ID, "abc-123")
 *     .put(ContextKeys.USER_ID, "user-42");
 * </pre>
 *
 * @author Andy Yang
 */
public class FrameworkContext {

    private final Map<String, String> entries;

    /**
     * Creates a new empty context.
     *
     * @return a new empty {@link FrameworkContext}
     */
    public static FrameworkContext create() {
        return new FrameworkContext();
    }

    /**
     * Creates a context from existing entries.
     *
     * @param entries the initial key-value pairs; null keys or values are silently ignored
     * @return a new {@link FrameworkContext} containing the provided entries
     */
    public static FrameworkContext of(Map<String, String> entries) {
        FrameworkContext context = new FrameworkContext();
        if (entries != null) {
            entries.forEach((key, value) -> {
                if (key != null && value != null) {
                    context.entries.put(key, value);
                }
            });
        }
        return context;
    }

    /**
     * Private constructor — use {@link #create()} or {@link #of(Map)}.
     */
    private FrameworkContext() {
        this.entries = new HashMap<>();
    }

    /**
     * Puts a key-value pair into the context.
     *
     * <p>null keys or values are silently ignored to keep the context clean.</p>
     *
     * @param key   the key
     * @param value the value
     * @return this context (fluent API)
     */
    public FrameworkContext put(String key, String value) {
        if (key != null && value != null) {
            entries.put(key, value);
        }
        return this;
    }

    /**
     * Returns the value associated with the given key, or {@code null} if absent.
     *
     * @param key the key to look up
     * @return the value, or {@code null}
     */
    public String get(String key) {
        return entries.get(key);
    }

    /**
     * Returns the value associated with the given key, or the default value if absent.
     *
     * @param key          the key to look up
     * @param defaultValue the fallback value
     * @return the value if present, otherwise {@code defaultValue}
     */
    public String getOrDefault(String key, String defaultValue) {
        return entries.getOrDefault(key, defaultValue);
    }

    /**
     * Removes the entry associated with the given key.
     *
     * @param key the key to remove
     * @return this context (fluent API)
     */
    public FrameworkContext remove(String key) {
        entries.remove(key);
        return this;
    }

    /**
     * Checks whether the context contains the given key.
     *
     * @param key the key to check
     * @return {@code true} if the key is present
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    /**
     * Returns all keys currently stored in the context.
     *
     * @return an unmodifiable set of keys
     */
    public Set<String> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    /**
     * Returns an unmodifiable copy of all entries.
     *
     * @return an unmodifiable map of all key-value pairs
     */
    public Map<String, String> toMap() {
        return Collections.unmodifiableMap(new HashMap<>(entries));
    }

    /**
     * Creates an immutable snapshot of the current context state.
     *
     * <p>The snapshot is suitable for cross-thread propagation — it captures
     * the current entries and is never mutated afterward.</p>
     *
     * @return an immutable {@link ContextSnapshot}
     */
    public ContextSnapshot snapshot() {
        return new ContextSnapshot(Collections.unmodifiableMap(new HashMap<>(entries)));
    }

    /**
     * Removes all entries from the context.
     */
    public void clear() {
        entries.clear();
    }

    /**
     * Checks whether the context has no entries.
     *
     * @return {@code true} if the context is empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public String toString() {
        return "FrameworkContext{entries=" + entries + '}';
    }
}
