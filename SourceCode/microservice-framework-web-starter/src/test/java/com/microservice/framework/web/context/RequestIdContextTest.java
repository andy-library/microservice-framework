package com.microservice.framework.web.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RequestIdContext ThreadLocal behavior tests.
 *
 * @author Andy Yang
 */
class RequestIdContextTest {

    // ======================================================================
    // Basic get/set/remove
    // ======================================================================

    @Nested
    @DisplayName("基本操作")
    class BasicOperations {

        @Test
        @DisplayName("初始状态下 get() 应返回 null")
        void initiallyNull() {
            RequestIdContext.remove(); // ensure clean state
            assertThat(RequestIdContext.get()).isNull();
        }

        @Test
        @DisplayName("set() 后 get() 应返回设置的值")
        void setAndGet() {
            RequestIdContext.set("req-123");
            assertThat(RequestIdContext.get()).isEqualTo("req-123");
            RequestIdContext.remove();
        }

        @Test
        @DisplayName("remove() 后 get() 应返回 null")
        void remove() {
            RequestIdContext.set("req-456");
            assertThat(RequestIdContext.get()).isEqualTo("req-456");
            RequestIdContext.remove();
            assertThat(RequestIdContext.get()).isNull();
        }

        @Test
        @DisplayName("set() 可以覆盖已有值")
        void overrideExistingValue() {
            RequestIdContext.set("req-old");
            assertThat(RequestIdContext.get()).isEqualTo("req-old");
            RequestIdContext.set("req-new");
            assertThat(RequestIdContext.get()).isEqualTo("req-new");
            RequestIdContext.remove();
        }
    }

    // ======================================================================
    // Thread isolation
    // ======================================================================

    @Nested
    @DisplayName("线程隔离")
    class ThreadIsolation {

        @Test
        @DisplayName("不同线程应有独立的 request ID")
        void threadIsolation() throws Exception {
            RequestIdContext.set("main-thread-id");

            Thread otherThread = new Thread(() -> {
                assertThat(RequestIdContext.get()).isNull();
                RequestIdContext.set("other-thread-id");
                assertThat(RequestIdContext.get()).isEqualTo("other-thread-id");
                RequestIdContext.remove();
            });
            otherThread.start();
            otherThread.join();

            assertThat(RequestIdContext.get()).isEqualTo("main-thread-id");
            RequestIdContext.remove();
        }
    }

    // ======================================================================
    // Null value handling
    // ======================================================================

    @Test
    @DisplayName("set(null) 应允许 null 值")
    void setNull() {
        RequestIdContext.set("req-1");
        RequestIdContext.set(null);
        assertThat(RequestIdContext.get()).isNull();
    }
}
