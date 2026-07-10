package com.microservice.framework.common.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ContextSnapshot 单元测试
 * <p>
 * 验证上下文快照的创建、不可变性、toContext 恢复以及查询操作。
 *
 * @author Andy Yang
 */
class ContextSnapshotTest {

    @Nested
    @DisplayName("从 FrameworkContext 创建快照")
    class FromFrameworkContext {

        @Test
        @DisplayName("snapshot() 应包含当前 entries")
        void snapshotShouldContainCurrentEntries() {
            FrameworkContext context = FrameworkContext.create();
            context.put("requestId", "abc-123");
            context.put("userId", "user-42");

            ContextSnapshot snapshot = context.snapshot();
            assertThat(snapshot.get("requestId")).isEqualTo("abc-123");
            assertThat(snapshot.get("userId")).isEqualTo("user-42");
            assertThat(snapshot.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("空上下文快照应为空")
        void snapshotOfEmptyContextShouldBeEmpty() {
            FrameworkContext context = FrameworkContext.create();
            ContextSnapshot snapshot = context.snapshot();
            assertThat(snapshot.isEmpty()).isTrue();
            assertThat(snapshot.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("不可变性")
    class Immutability {

        @Test
        @DisplayName("修改原上下文不应影响快照")
        void modifyingOriginalShouldNotAffectSnapshot() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value1");
            ContextSnapshot snapshot = context.snapshot();

            context.put("key", "value2");
            context.put("newKey", "newValue");

            assertThat(snapshot.get("key")).isEqualTo("value1");
            assertThat(snapshot.containsKey("newKey")).isFalse();
        }

        @Test
        @DisplayName("快照的 toMap() 应为不可变 map")
        void snapshotToMapShouldBeImmutable() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            ContextSnapshot snapshot = context.snapshot();

            assertThatThrownBy(() -> snapshot.toMap().put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("toContext() 恢复")
    class ToContextRestoration {

        @Test
        @DisplayName("toContext() 应创建新的可变上下文")
        void toContextShouldCreateNewMutableContext() {
            FrameworkContext original = FrameworkContext.create();
            original.put("key", "value");
            ContextSnapshot snapshot = original.snapshot();

            FrameworkContext restored = snapshot.toContext();
            assertThat(restored.get("key")).isEqualTo("value");

            // 修改恢复的上下文不应影响快照
            restored.put("key", "modified");
            assertThat(snapshot.get("key")).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("of(Map) 工厂方法")
    class OfMapFactory {

        @Test
        @DisplayName("of(Map) 应创建包含指定 entries 的快照")
        void ofMapShouldCreateSnapshotWithEntries() {
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");

            ContextSnapshot snapshot = ContextSnapshot.of(map);
            assertThat(snapshot.get("key1")).isEqualTo("value1");
            assertThat(snapshot.get("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("of(null) 应创建空快照")
        void ofNullShouldCreateEmptySnapshot() {
            ContextSnapshot snapshot = ContextSnapshot.of(null);
            assertThat(snapshot.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("of(Map) 中 null key 或 value 应被忽略")
        void ofMapShouldIgnoreNullKeyOrValue() {
            Map<String, String> map = new HashMap<>();
            map.put("valid", "value");
            map.put(null, "ignored");
            map.put("key", null);

            ContextSnapshot snapshot = ContextSnapshot.of(map);
            assertThat(snapshot.containsKey("valid")).isTrue();
            assertThat(snapshot.containsKey(null)).isFalse();
            assertThat(snapshot.containsKey("key")).isFalse();
        }
    }

    @Nested
    @DisplayName("查询操作")
    class QueryOperations {

        @Test
        @DisplayName("getOrDefault 应返回默认值")
        void getOrDefaultShouldReturnFallback() {
            ContextSnapshot snapshot = ContextSnapshot.of(null);
            assertThat(snapshot.getOrDefault("missing", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("containsKey 应正确判断")
        void containsKeyShouldWorkCorrectly() {
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");
            ContextSnapshot snapshot = ContextSnapshot.of(map);

            assertThat(snapshot.containsKey("key")).isTrue();
            assertThat(snapshot.containsKey("missing")).isFalse();
        }

        @Test
        @DisplayName("keys() 应返回所有 key")
        void keysShouldReturnAllKeys() {
            Map<String, String> map = new HashMap<>();
            map.put("a", "1");
            map.put("b", "2");
            ContextSnapshot snapshot = ContextSnapshot.of(map);

            assertThat(snapshot.keys()).containsExactlyInAnyOrder("a", "b");
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同 entries 的快照应相等")
        void sameEntriesShouldBeEqual() {
            Map<String, String> map1 = new HashMap<>();
            map1.put("key", "value");
            Map<String, String> map2 = new HashMap<>();
            map2.put("key", "value");

            ContextSnapshot snapshot1 = ContextSnapshot.of(map1);
            ContextSnapshot snapshot2 = ContextSnapshot.of(map2);

            assertThat(snapshot1).isEqualTo(snapshot2);
            assertThat(snapshot1.hashCode()).isEqualTo(snapshot2.hashCode());
        }
    }
}
