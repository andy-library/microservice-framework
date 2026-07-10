package com.microservice.framework.common.context;

/**
 * ThreadLocal adapter for {@link FrameworkContext}, with try/finally cleanup support.
 *
 * <p>Each thread owns its own {@link FrameworkContext} instance. If no context
 * has been set, {@link #get()} auto-creates an empty one.</p>
 *
 * <p>Typical usage pattern in a filter or interceptor:</p>
 * <pre>
 * ThreadLocalContextAdapter adapter = ...;
 * try {
 *     adapter.get().put(ContextKeys.REQUEST_ID, requestId);
 *     // business logic ...
 * } finally {
 *     adapter.clear();
 * }
 * </pre>
 *
 * <p>For cross-thread propagation:</p>
 * <pre>
 * ContextSnapshot snapshot = adapter.snapshot();
 * adapter.runWithSnapshot(snapshot, () -&gt; {
 *     // the async thread now has access to the same context
 * });
 * </pre>
 *
 * @author Andy Yang
 */
public class ThreadLocalContextAdapter {

    private final ThreadLocal<FrameworkContext> threadLocal = new ThreadLocal<>();

    /**
     * Returns the current thread's {@link FrameworkContext}.
     *
     * <p>If no context has been set for the current thread, a new empty
     * context is automatically created and stored.</p>
     *
     * @return the current thread's context, never {@code null}
     */
    public FrameworkContext get() {
        FrameworkContext context = threadLocal.get();
        if (context == null) {
            context = FrameworkContext.create();
            threadLocal.set(context);
        }
        return context;
    }

    /**
     * Explicitly sets the context for the current thread.
     *
     * @param context the context to set; may be {@code null} to clear the binding
     */
    public void set(FrameworkContext context) {
        threadLocal.set(context);
    }

    /**
     * Removes the current thread's context binding.
     */
    public void remove() {
        threadLocal.remove();
    }

    /**
     * Creates an immutable snapshot of the current thread's context.
     *
     * <p>If no context is set, an empty snapshot is returned.</p>
     *
     * @return a {@link ContextSnapshot} capturing the current entries
     */
    public ContextSnapshot snapshot() {
        FrameworkContext context = threadLocal.get();
        if (context == null) {
            return ContextSnapshot.of(java.util.Collections.emptyMap());
        }
        return context.snapshot();
    }

    /**
     * Restores a snapshot into the current thread, replacing any existing context.
     *
     * @param snapshot the snapshot to restore
     */
    public void restore(ContextSnapshot snapshot) {
        if (snapshot == null) {
            clear();
            return;
        }
        threadLocal.set(snapshot.toContext());
    }

    /**
     * Executes an action with the given snapshot context, restoring the
     * previous context afterward in a try/finally block.
     *
     * <p>This is the primary mechanism for cross-thread context propagation.
     * The snapshot is restored into the current thread before the action runs,
     * and the original context (or no context) is restored after the action
     * completes — even if the action throws.</p>
     *
     * @param snapshot the snapshot to apply
     * @param action   the action to execute with the snapshot context
     */
    public void runWithSnapshot(ContextSnapshot snapshot, Runnable action) {
        FrameworkContext previous = threadLocal.get();
        try {
            restore(snapshot);
            action.run();
        } finally {
            threadLocal.set(previous);
        }
    }

    /**
     * Clears the current thread's context — equivalent to {@link #remove()}.
     *
     * <p>Intended for explicit cleanup in try/finally blocks to prevent
     * ThreadLocal leaks in thread-pool environments.</p>
     */
    public void clear() {
        threadLocal.remove();
    }
}
