package com.microservice.framework.feign.api;

import com.microservice.framework.common.context.ContextSnapshot;
import com.microservice.framework.common.context.FrameworkContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FeignContextPropagator 接口契约测试
 * <p>
 * 验证上下文传播的实现：propagate 应提取指定键的值，
 * restore 应将头信息写入上下文。
 *
 * @author Andy Yang
 */
class FeignContextPropagatorTest {

    @Nested
    @DisplayName("上下文传播")
    class ContextPropagation {

        @Test
        @DisplayName("propagate 应提取配置的上下文键")
        void propagateShouldExtractConfiguredKeys() {
            Set<String> keys = Set.of("requestId", "traceId", "userId");
            FeignContextPropagator propagator = new StubPropagator(keys);

            FrameworkContext context = FrameworkContext.create()
                    .put("requestId", "req-001")
                    .put("traceId", "trace-001")
                    .put("userId", "user-001")
                    .put("tenantId", "tenant-001"); // not in propagate keys

            ContextSnapshot snapshot = context.snapshot();
            Map<String, String> headers = propagator.propagate(snapshot);

            assertThat(headers).containsEntry("requestId", "req-001");
            assertThat(headers).containsEntry("traceId", "trace-001");
            assertThat(headers).containsEntry("userId", "user-001");
            assertThat(headers).doesNotContainKey("tenantId");
        }

        @Test
        @DisplayName("propagate 对空快照应返回空 Map")
        void propagateWithEmptySnapshotShouldReturnEmptyMap() {
            Set<String> keys = Set.of("requestId", "traceId", "userId");
            FeignContextPropagator propagator = new StubPropagator(keys);

            ContextSnapshot snapshot = FrameworkContext.create().snapshot();
            Map<String, String> headers = propagator.propagate(snapshot);

            assertThat(headers).isEmpty();
        }

        @Test
        @DisplayName("propagate 对部分缺失键应只返回存在的键")
        void propagateWithPartialKeysShouldReturnOnlyPresent() {
            Set<String> keys = Set.of("requestId", "traceId", "userId");
            FeignContextPropagator propagator = new StubPropagator(keys);

            FrameworkContext context = FrameworkContext.create()
                    .put("requestId", "req-001"); // only requestId present
            ContextSnapshot snapshot = context.snapshot();
            Map<String, String> headers = propagator.propagate(snapshot);

            assertThat(headers).containsOnlyKeys("requestId");
            assertThat(headers).containsEntry("requestId", "req-001");
        }

        @Test
        @DisplayName("propagate 对 null 快照应返回空 Map")
        void propagateWithNullSnapshotShouldReturnEmptyMap() {
            Set<String> keys = Set.of("requestId");
            FeignContextPropagator propagator = new StubPropagator(keys);

            Map<String, String> headers = propagator.propagate(null);
            assertThat(headers).isEmpty();
        }
    }

    @Nested
    @DisplayName("上下文恢复")
    class ContextRestore {

        @Test
        @DisplayName("restore 应将头信息写入当前线程上下文")
        void restoreShouldWriteHeadersToContext() {
            Set<String> keys = Set.of("requestId", "traceId");
            FeignContextPropagator propagator = new StubPropagator(keys);

            Map<String, String> headers = Map.of("requestId", "req-002", "traceId", "trace-002");
            propagator.restore(headers);

            // StubPropagator stores in local context for testing
            assertThat(((StubPropagator) propagator).restoredHeaders)
                    .containsEntry("requestId", "req-002")
                    .containsEntry("traceId", "trace-002");
        }
    }

    // Stub implementation for contract testing
    static class StubPropagator implements FeignContextPropagator {

        private final Set<String> propagateKeys;
        Map<String, String> restoredHeaders;

        StubPropagator(Set<String> propagateKeys) {
            this.propagateKeys = propagateKeys;
        }

        @Override
        public Map<String, String> propagate(ContextSnapshot snapshot) {
            if (snapshot == null) {
                return Map.of();
            }
            Map<String, String> headers = new java.util.HashMap<>();
            for (String key : propagateKeys) {
                String value = snapshot.get(key);
                if (value != null) {
                    headers.put(key, value);
                }
            }
            return headers;
        }

        @Override
        public void restore(Map<String, String> headers) {
            this.restoredHeaders = headers;
        }
    }
}
