package com.microservice.framework.common.context;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of a {@link FrameworkContext} for cross-thread propagation.
 *
 * <p>Snapshots are created via {@link FrameworkContext#snapshot()} or the static
 * factory {@link #of(Map)}. Once created, a snapshot cannot be modified — it is
 * safe to share across threads without synchronization.</p>
 *
 * <p>To restore a snapshot into a mutable context, use {@link #toContext()}.</p>
 *
 * @author Andy Yang
 */
public final class ContextSnapshot {

    private final Map<String, String> entries;

    /**
     * Creates a snapshot from an existing map.
     *
     * <p>null keys or values are silently ignored. The resulting snapshot
     * holds an unmodifiable copy of the filtered entries.</p>
     *
     * @param entries the source entries
     * @return an immutable {@link ContextSnapshot}
     */
    public static ContextSnapshot of(Map<String, String> entries) {
        Map<String, String> filtered = new java.util.HashMap<>();
        if (entries != null) {
            entries.forEach((key, value) -> {
                if (key != null && value != null) {
                    filtered.put(key, value);
                }
            });
        }
        return new ContextSnapshot(Collections.unmodifiableMap(filtered));
    }

    /**
     * Package-private constructor — created only via
     * {@link FrameworkContext#snapshot()} or {@link #of(Map)}.
     *
     * @param entries an unmodifiable map of entries
     */
    ContextSnapshot(Map<String, String> entries) {
        this.entries = entries;
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
     * Checks whether the snapshot contains the given key.
     *
     * @param key the key to check
     * @return {@code true} if the key is present
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    /**
     * Returns all keys stored in the snapshot.
     *
     * @return an unmodifiable set of keys
     */
    public Set<String> keys() {
        return entries.keySet();
    }

    /**
     * Returns the entries map (already unmodifiable).
     *
     * @return the unmodifiable entries map
     */
    public Map<String, String> toMap() {
        return entries;
    }

    /**
     * Creates a new mutable {@link FrameworkContext} from this snapshot.
     *
     * @return a new {@link FrameworkContext} containing the same entries
     */
    public FrameworkContext toContext() {
        return FrameworkContext.of(entries);
    }

    /**
     * Checks whether the snapshot has no entries.
     *
     * @return {@code true} if the snapshot is empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns the number of entries in the snapshot.
     *
     * @return the entry count
     */
    public int size() {
        return entries.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContextSnapshot)) {
            return false;
        }
        ContextSnapshot that = (ContextSnapshot) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "ContextSnapshot{entries=" + entries + '}';
    }
}
