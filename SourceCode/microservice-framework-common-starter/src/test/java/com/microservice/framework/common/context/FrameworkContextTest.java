package com.microservice.framework.common.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FrameworkContext 单元测试
 * <p>
 * 验证轻量上下文的创建、put/get 流式 API、remove、containsKey、
 * snapshot 不可变快照、clear 以及 of(Map) 工厂方法。
 *
 * @author Andy Yang
 */
class FrameworkContextTest {

    @Nested
    @DisplayName("创建")
    class Creation {

        @Test
        @DisplayName("create() 应创建空上下文")
        void createShouldReturnEmptyContext() {
            FrameworkContext context = FrameworkContext.create();
            assertThat(context.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("put/get 流式 API")
    class PutGetFluentApi {

        @Test
        @DisplayName("put/get 应正确存取值")
        void putAndGetShouldStoreAndRetrieveValue() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            assertThat(context.get("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("put 应返回自身支持链式调用")
        void putShouldReturnSelfForChaining() {
            FrameworkContext context = FrameworkContext.create();
            FrameworkContext same = context.put("a", "1").put("b", "2");
            assertThat(same).isSameAs(context);
            assertThat(context.get("a")).isEqualTo("1");
            assertThat(context.get("b")).isEqualTo("2");
        }

        @Test
        @DisplayName("put null key 应被忽略")
        void putWithNullKeyShouldBeIgnored() {
            FrameworkContext context = FrameworkContext.create();
            context.put(null, "value");
            assertThat(context.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("put null value 应被忽略")
        void putWithNullValueShouldBeIgnored() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", null);
            assertThat(context.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("get 不存在的 key 应返回 null")
        void getNonExistentKeyShouldReturnNull() {
            FrameworkContext context = FrameworkContext.create();
            assertThat(context.get("missing")).isNull();
        }

        @Test
        @DisplayName("getOrDefault 应返回默认值")
        void getOrDefaultShouldReturnFallback() {
            FrameworkContext context = FrameworkContext.create();
            assertThat(context.getOrDefault("missing", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("getOrDefault 存在 key 时应返回实际值")
        void getOrDefaultShouldReturnActualValueWhenPresent() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            assertThat(context.getOrDefault("key", "default")).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("remove")
    class RemoveOperation {

        @Test
        @DisplayName("remove 应移除指定 key")
        void removeShouldRemoveKey() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            context.remove("key");
            assertThat(context.containsKey("key")).isFalse();
        }

        @Test
        @DisplayName("remove 应返回自身支持链式调用")
        void removeShouldReturnSelfForChaining() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            FrameworkContext same = context.remove("key");
            assertThat(same).isSameAs(context);
        }
    }

    @Nested
    @DisplayName("containsKey")
    class ContainsKey {

        @Test
        @DisplayName("containsKey 存在时应返回 true")
        void containsKeyShouldReturnTrueWhenPresent() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            assertThat(context.containsKey("key")).isTrue();
        }

        @Test
        @DisplayName("containsKey 不存在时应返回 false")
        void containsKeyShouldReturnFalseWhenAbsent() {
            FrameworkContext context = FrameworkContext.create();
            assertThat(context.containsKey("key")).isFalse();
        }
    }

    @Nested
    @DisplayName("snapshot")
    class Snapshot {

        @Test
        @DisplayName("snapshot() 应返回不可变快照")
        void snapshotShouldReturnImmutableCopy() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            ContextSnapshot snapshot = context.snapshot();

            assertThat(snapshot.get("key")).isEqualTo("value");

            // 修改原上下文不应影响快照
            context.put("key", "newValue");
            assertThat(snapshot.get("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("修改原上下文不应影响已创建的快照")
        void modifyingOriginalShouldNotAffectSnapshot() {
            FrameworkContext context = FrameworkContext.create();
            context.put("a", "1");
            ContextSnapshot snapshot = context.snapshot();

            context.put("b", "2");
            assertThat(snapshot.containsKey("b")).isFalse();
            assertThat(snapshot.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearOperation {

        @Test
        @DisplayName("clear() 应清空所有 entries")
        void clearShouldRemoveAllEntries() {
            FrameworkContext context = FrameworkContext.create();
            context.put("a", "1").put("b", "2");
            context.clear();
            assertThat(context.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("of(Map) 工厂")
    class OfMapFactory {

        @Test
        @DisplayName("of(Map) 应创建包含指定 entries 的上下文")
        void ofMapShouldCreateContextWithEntries() {
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            FrameworkContext context = FrameworkContext.of(map);

            assertThat(context.get("key1")).isEqualTo("value1");
            assertThat(context.get("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("of(null) 应创建空上下文")
        void ofNullShouldCreateEmptyContext() {
            FrameworkContext context = FrameworkContext.of(null);
            assertThat(context.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("of(Map) 中 null key 或 value 应被忽略")
        void ofMapShouldIgnoreNullKeyOrValue() {
            Map<String, String> map = new HashMap<>();
            map.put("valid", "value");
            map.put(null, "ignored");
            map.put("key", null);
            FrameworkContext context = FrameworkContext.of(map);

            assertThat(context.containsKey("valid")).isTrue();
            assertThat(context.containsKey(null)).isFalse();
            assertThat(context.containsKey("key")).isFalse();
        }
    }

    @Nested
    @DisplayName("keys 和 toMap")
    class KeysAndToMap {

        @Test
        @DisplayName("keys() 应返回所有 key 的不可变集合")
        void keysShouldReturnUnmodifiableSet() {
            FrameworkContext context = FrameworkContext.create();
            context.put("a", "1").put("b", "2");
            assertThat(context.keys()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("toMap() 应返回不可变 map")
        void toMapShouldReturnUnmodifiableMap() {
            FrameworkContext context = FrameworkContext.create();
            context.put("key", "value");
            Map<String, String> map = context.toMap();
            assertThat(map.get("key")).isEqualTo("value");
            assertThatThrownBy(() -> map.put("new", "value"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
