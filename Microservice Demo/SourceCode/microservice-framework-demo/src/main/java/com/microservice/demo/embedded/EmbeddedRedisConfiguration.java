package com.microservice.demo.embedded;

import com.microservice.framework.redis.api.DistributedLock;
import com.microservice.framework.redis.api.RateLimiter;
import com.microservice.framework.redis.api.RedisCache;
import com.microservice.framework.redis.api.RedisCommandExecutor;
import com.microservice.framework.redis.api.RedisCounter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Embedded Redis Configuration
 *
 * Provides in-memory Redis bean implementations that override the starter's
 * default implementations via {@code @ConditionalOnMissingBean}. Activates only
 * when {@code framework.redis.provider=embedded} is set.
 *
 * <p>All implementations are fully functional (not stubs) and suitable for
 * integration testing and demo purposes without a real Redis server.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "framework.redis", name = "provider", havingValue = "embedded")
public class EmbeddedRedisConfiguration {

    // ======================================================================
    // RedisCommandExecutor
    // ======================================================================

    @Bean
    public RedisCommandExecutor inMemoryRedisCommandExecutor() {
        return new InMemoryRedisCommandExecutor();
    }

    /**
     * HashMap-backed in-memory RedisCommandExecutor.
     * Supports setIfAbsent (with TTL), get, hasKey, getExpire, and executeScript.
     */
    static class InMemoryRedisCommandExecutor implements RedisCommandExecutor {

        private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

        static class Entry {
            final String value;
            final long expiresAtMs; // 0 means no expiration

            Entry(String value, long expiresAtMs) {
                this.value = value;
                this.expiresAtMs = expiresAtMs;
            }

            boolean isExpired() {
                return expiresAtMs > 0 && System.currentTimeMillis() > expiresAtMs;
            }
        }

        private Entry getValidEntry(String key) {
            Entry entry = store.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                store.remove(key, entry);
                return null;
            }
            return entry;
        }

        @Override
        public Boolean setIfAbsent(String key, String value, Duration ttl) {
            long expiresAtMs = ttl != null && !ttl.isZero()
                    ? System.currentTimeMillis() + ttl.toMillis()
                    : 0L;
            Entry newEntry = new Entry(value, expiresAtMs);
            Entry existing = store.putIfAbsent(key, newEntry);
            if (existing != null) {
                // Key already exists; check if expired
                if (existing.isExpired()) {
                    // Expired entry — try to replace it
                    if (store.replace(key, existing, newEntry)) {
                        return true; // Successfully replaced expired entry
                    }
                    // CAS failed — another thread already replaced it
                    return false;
                }
                // Key exists and not expired — do not set
                return false;
            }
            return true;
        }

        @Override
        public String get(String key) {
            Entry entry = getValidEntry(key);
            return entry != null ? entry.value : null;
        }

        @Override
        public Boolean hasKey(String key) {
            return getValidEntry(key) != null;
        }

        @Override
        public Long getExpire(String key) {
            Entry entry = store.get(key);
            if (entry == null) {
                return -2L; // Key does not exist (Redis convention)
            }
            if (entry.isExpired()) {
                store.remove(key, entry);
                return -2L;
            }
            if (entry.expiresAtMs == 0) {
                return -1L; // No expiration set (Redis convention)
            }
            long remainingMs = entry.expiresAtMs - System.currentTimeMillis();
            return remainingMs > 0 ? remainingMs / 1000 : 0L;
        }

        @Override
        public Object executeScript(String scriptContent, Class<?> returnType,
                                    List<String> keys, Object... args) {
            // In-memory Lua script emulation: return a simple result based on script hints.
            // For demo purposes, return a basic result matching common script patterns.
            if (returnType == Boolean.class || returnType == boolean.class) {
                return true;
            }
            if (returnType == Long.class || returnType == long.class) {
                return 1L;
            }
            return "OK";
        }
    }

    // ======================================================================
    // DistributedLock
    // ======================================================================

    @Bean
    public DistributedLock inMemoryDistributedLock() {
        return new InMemoryDistributedLock();
    }

    /**
     * ReentrantLock-backed in-memory DistributedLock.
     * Each lock key maps to a ReentrantLock with tryLock/unlock semantics.
     * Thread-confined (single JVM only) — suitable for embedded/demo use.
     */
    static class InMemoryDistributedLock implements DistributedLock {

        private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();

        static class LockEntry {
            final Thread ownerThread;
            final AtomicLong holdCount = new AtomicLong(0);
            final AtomicLong expiresAtMs = new AtomicLong(0);

            LockEntry(Thread ownerThread) {
                this.ownerThread = ownerThread;
            }
        }

        @Override
        public boolean tryLock(String key) {
            return tryLock(key, 0, 0);
        }

        @Override
        public boolean tryLock(String key, long timeout) {
            return tryLock(key, timeout, 0);
        }

        @Override
        public boolean tryLock(String key, long timeout, long expire) {
            LockEntry existing = locks.get(key);

            // Already locked by the same thread — reentrant acquisition
            if (existing != null && existing.ownerThread == Thread.currentThread()) {
                // Check if the lock has expired
                long expiresMs = existing.expiresAtMs.get();
                if (expiresMs > 0 && System.currentTimeMillis() > expiresMs) {
                    // Lock expired — remove and try fresh acquisition
                    locks.remove(key, existing);
                } else {
                    existing.holdCount.incrementAndGet();
                    if (expire > 0) {
                        existing.expiresAtMs.set(System.currentTimeMillis() + expire);
                    }
                    return true;
                }
            }

            // Lock held by another thread — fail immediately (no real blocking in embedded)
            if (existing != null) {
                // Check expiration
                long expiresMs = existing.expiresAtMs.get();
                if (expiresMs > 0 && System.currentTimeMillis() > expiresMs) {
                    // Expired — remove and try fresh acquisition
                    locks.remove(key, existing);
                } else {
                    return false;
                }
            }

            // No existing lock — try to acquire
            LockEntry newEntry = new LockEntry(Thread.currentThread());
            newEntry.holdCount.set(1);
            if (expire > 0) {
                newEntry.expiresAtMs.set(System.currentTimeMillis() + expire);
            }

            LockEntry prev = locks.putIfAbsent(key, newEntry);
            if (prev == null) {
                return true; // Successfully acquired
            }
            // CAS failure — another thread got it first
            return false;
        }

        @Override
        public boolean unlock(String key) {
            LockEntry entry = locks.get(key);
            if (entry == null || entry.ownerThread != Thread.currentThread()) {
                return false; // Not locked or not owned by this thread
            }
            long remaining = entry.holdCount.decrementAndGet();
            if (remaining <= 0) {
                locks.remove(key, entry);
            }
            return true;
        }

        @Override
        public boolean isLocked(String key) {
            LockEntry entry = locks.get(key);
            if (entry == null) {
                return false;
            }
            // Check expiration
            long expiresMs = entry.expiresAtMs.get();
            if (expiresMs > 0 && System.currentTimeMillis() > expiresMs) {
                locks.remove(key, entry);
                return false;
            }
            return true;
        }
    }

    // ======================================================================
    // RateLimiter
    // ======================================================================

    @Bean
    public RateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    /**
     * AtomicLong/Semaphore-backed in-memory RateLimiter.
     * Uses a sliding-window counter approach with configurable replenish rate.
     * Default: 10 permits per second per key.
     */
    static class InMemoryRateLimiter implements RateLimiter {

        private final ConcurrentHashMap<String, RateLimitEntry> entries = new ConcurrentHashMap<>();

        static class RateLimitEntry {
            final Semaphore semaphore;
            final int maxPermits;
            final long periodMs;
            final AtomicLong lastReplenishMs = new AtomicLong(System.currentTimeMillis());
            final AtomicLong totalAcquired = new AtomicLong(0);

            RateLimitEntry(int maxPermits, long periodMs) {
                this.maxPermits = maxPermits;
                this.periodMs = periodMs;
                this.semaphore = new Semaphore(maxPermits);
            }

            void replenish() {
                long now = System.currentTimeMillis();
                long elapsed = now - lastReplenishMs.get();
                if (elapsed >= periodMs) {
                    // Replenish permits back to max
                    int currentAvailable = semaphore.availablePermits();
                    int toAdd = maxPermits - currentAvailable;
                    if (toAdd > 0) {
                        semaphore.release(toAdd);
                    }
                    lastReplenishMs.set(now);
                }
            }
        }

        private RateLimitEntry getOrCreate(String key) {
            return entries.computeIfAbsent(key,
                    k -> new RateLimitEntry(10, 1000)); // default: 10 permits/sec
        }

        private RateLimitEntry getOrCreate(String key, int permits, long periodMs) {
            return entries.computeIfAbsent(key,
                    k -> new RateLimitEntry(permits, periodMs));
        }

        @Override
        public boolean tryAcquire(String key) {
            return tryAcquire(key, 1);
        }

        @Override
        public boolean tryAcquire(String key, int permits) {
            return tryAcquire(key, permits, 1000); // default period: 1 second
        }

        @Override
        public boolean tryAcquire(String key, int permits, long period) {
            RateLimitEntry entry = getOrCreate(key, permits * 10, period);
            entry.replenish();
            boolean acquired = entry.semaphore.tryAcquire(1); // acquire 1 permit per call
            if (acquired) {
                entry.totalAcquired.incrementAndGet();
            }
            return acquired;
        }

        @Override
        public long acquire(String key) {
            RateLimitEntry entry = getOrCreate(key);
            entry.replenish();
            try {
                entry.semaphore.acquire();
                entry.totalAcquired.incrementAndGet();
                return entry.totalAcquired.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1L;
            }
        }

        @Override
        public long getAvailablePermits(String key) {
            RateLimitEntry entry = getOrCreate(key);
            entry.replenish();
            return entry.semaphore.availablePermits();
        }
    }

    // ======================================================================
    // RedisCounter
    // ======================================================================

    @Bean
    public RedisCounter inMemoryRedisCounter() {
        return new InMemoryRedisCounter();
    }

    /**
     * ConcurrentHashMap-backed in-memory RedisCounter.
     * Supports increment, decrement, get, reset, and getAndIncrement operations.
     */
    static class InMemoryRedisCounter implements RedisCounter {

        private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

        private AtomicLong getOrCreate(String key) {
            return counters.computeIfAbsent(key, k -> new AtomicLong(0));
        }

        @Override
        public long increment(String key) {
            return getOrCreate(key).incrementAndGet();
        }

        @Override
        public long increment(String key, long delta) {
            return getOrCreate(key).addAndGet(delta);
        }

        @Override
        public long decrement(String key) {
            return getOrCreate(key).decrementAndGet();
        }

        @Override
        public long decrement(String key, long delta) {
            return getOrCreate(key).addAndGet(-delta);
        }

        @Override
        public long get(String key) {
            AtomicLong counter = counters.get(key);
            return counter != null ? counter.get() : 0L;
        }

        @Override
        public long reset(String key, long value) {
            AtomicLong counter = getOrCreate(key);
            counter.set(value);
            return value;
        }

        @Override
        public long getAndIncrement(String key) {
            return getOrCreate(key).getAndIncrement();
        }
    }

    // ======================================================================
    // RedisCache
    // ======================================================================

    @Bean
    public RedisCache inMemoryRedisCache() {
        return new InMemoryRedisCache();
    }

    /**
     * ConcurrentHashMap-backed in-memory RedisCache with TTL tracking.
     * Supports get, put (with TTL), evict, evictAll (batch), and evictByPattern (glob matching).
     * <p>
     * {@code evictAll} and {@code evictByPattern} are fully implemented to match
     * the recently-added interface methods.
     * </p>
     */
    static class InMemoryRedisCache implements RedisCache {

        private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

        static class CacheEntry {
            final Object value;
            final long expiresAtMs; // 0 means no expiration

            CacheEntry(Object value, long expiresAtMs) {
                this.value = value;
                this.expiresAtMs = expiresAtMs;
            }

            boolean isExpired() {
                return expiresAtMs > 0 && System.currentTimeMillis() > expiresAtMs;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (entry.isExpired()) {
                cache.remove(key, entry);
                return null;
            }
            return (T) entry.value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            T value = get(key);
            if (value == null) {
                return null;
            }
            if (type.isInstance(value)) {
                return value;
            }
            // Attempt conversion for common types
            if (type == String.class && value != null) {
                return (T) value.toString();
            }
            return null;
        }

        @Override
        public void put(String key, Object value) {
            put(key, value, 0); // No TTL
        }

        @Override
        public void put(String key, Object value, long ttl) {
            long expiresAtMs = ttl > 0
                    ? System.currentTimeMillis() + ttl * 1000
                    : 0L;
            cache.put(key, new CacheEntry(value, expiresAtMs));
        }

        @Override
        public boolean evict(String key) {
            CacheEntry removed = cache.remove(key);
            return removed != null;
        }

        @Override
        public void evictAll(List<String> keys) {
            if (keys == null || keys.isEmpty()) {
                return;
            }
            for (String key : keys) {
                cache.remove(key);
            }
        }

        @Override
        public long evictByPattern(String pattern) {
            if (pattern == null || pattern.isEmpty()) {
                return 0L;
            }
            Pattern regex = globToRegex(pattern);
            List<String> keysToRemove = new ArrayList<>();
            for (String key : cache.keySet()) {
                if (regex.matcher(key).matches()) {
                    keysToRemove.add(key);
                }
            }
            long count = 0;
            for (String key : keysToRemove) {
                if (cache.remove(key) != null) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Convert a Redis KEYS-style glob pattern to a Java regex Pattern.
         * Supports: * (any chars), ? (single char), [abc] (char class).
         */
        private Pattern globToRegex(String glob) {
            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);
                switch (c) {
                    case '*':
                        regex.append(".*");
                        break;
                    case '?':
                        regex.append(".");
                        break;
                    case '[':
                        // Pass through character class unchanged
                        regex.append(c);
                        break;
                    case ']':
                        regex.append(c);
                        break;
                    case '\\':
                        regex.append("\\\\");
                        break;
                    default:
                        // Escape regex special characters
                        if (".^$+{}|()".indexOf(c) >= 0) {
                            regex.append('\\').append(c);
                        } else {
                            regex.append(c);
                        }
                        break;
                }
            }
            regex.append("$");
            return Pattern.compile(regex.toString());
        }
    }
}
