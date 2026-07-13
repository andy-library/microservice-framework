package com.microservice.framework.database.api;

/**
 * Thread-bound read/write routing policy.
 * <p>
 * The context centralizes routing decisions for normal reads, transaction
 * scopes, explicit hints, and read-after-write safety. It does not perform JDBC
 * routing by itself; data-source adapters can call {@link #currentRoute()} to
 * make a consistent decision.
 */
public class ReadWriteRoutingContext {

    private final boolean readAfterWriteRoutePrimary;
    private final ThreadLocal<State> state = ThreadLocal.withInitial(State::new);

    public ReadWriteRoutingContext(boolean readAfterWriteRoutePrimary) {
        this.readAfterWriteRoutePrimary = readAfterWriteRoutePrimary;
    }

    public RoutingHint currentRoute() {
        State current = state.get();
        if (current.forcedRoute != null) {
            return current.forcedRoute;
        }
        if (current.transactionDepth > 0) {
            return RoutingHint.PRIMARY;
        }
        if (readAfterWriteRoutePrimary && current.writeSeen) {
            return RoutingHint.PRIMARY;
        }
        return RoutingHint.READ_REPLICA;
    }

    public void markWrite() {
        state.get().writeSeen = true;
    }

    public Scope transaction() {
        State before = state.get().copy();
        state.get().transactionDepth++;
        return new Scope(before);
    }

    public Scope force(RoutingHint routingHint) {
        if (routingHint == null) {
            throw new IllegalArgumentException("routingHint must not be null");
        }
        State before = state.get().copy();
        state.get().forcedRoute = routingHint;
        return new Scope(before);
    }

    public void clear() {
        state.remove();
    }

    public final class Scope implements AutoCloseable {

        private final State before;
        private boolean closed;

        private Scope(State before) {
            this.before = before;
        }

        @Override
        public void close() {
            if (!closed) {
                state.set(before);
                closed = true;
            }
        }
    }

    private static final class State {

        private int transactionDepth;
        private RoutingHint forcedRoute;
        private boolean writeSeen;

        private State copy() {
            State copy = new State();
            copy.transactionDepth = transactionDepth;
            copy.forcedRoute = forcedRoute;
            copy.writeSeen = writeSeen;
            return copy;
        }
    }
}
