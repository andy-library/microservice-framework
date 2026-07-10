package com.microservice.framework.nacos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ConfigChangeEvent 测试
 * <p>
 * 验证配置变更事件的创建、分类判断（新增/修改/删除）和相等性。
 *
 * @author Andy Yang
 */
class ConfigChangeEventTest {

    private final ConfigSourceDescriptor source = ConfigSourceDescriptor.nacos(
            "app.yml", "DEFAULT_GROUP", "dev", 1000L);

    @Nested
    @DisplayName("事件创建")
    class CreationTests {

        @Test
        @DisplayName("应正确创建配置变更事件")
        void shouldCreateChangeEvent() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "database.url", "old_url", "new_url", source, 2000L);

            assertThat(event.getKey()).isEqualTo("database.url");
            assertThat(event.getOldValue()).isEqualTo("old_url");
            assertThat(event.getNewValue()).isEqualTo("new_url");
            assertThat(event.getSource()).isEqualTo(source);
            assertThat(event.getChangeTimestamp()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("null key 应抛出 NullPointerException")
        void nullKeyShouldThrowException() {
            assertThatThrownBy(() -> new ConfigChangeEvent(
                    null, "old", "new", source, 1000L))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("key");
        }

        @Test
        @DisplayName("null source 应抛出 NullPointerException")
        void nullSourceShouldThrowException() {
            assertThatThrownBy(() -> new ConfigChangeEvent(
                    "key", "old", "new", null, 1000L))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("source");
        }

        @Test
        @DisplayName("oldValue 和 newValue 可为 null")
        void nullValuesShouldBeAccepted() {
            ConfigChangeEvent addition = new ConfigChangeEvent(
                    "new.key", null, "value", source, 1000L);
            assertThat(addition.getOldValue()).isNull();

            ConfigChangeEvent deletion = new ConfigChangeEvent(
                    "removed.key", "value", null, source, 1000L);
            assertThat(deletion.getNewValue()).isNull();
        }
    }

    @Nested
    @DisplayName("事件分类判断")
    class ClassificationTests {

        @Test
        @DisplayName("旧值为 null、新值不为 null 时应为新增配置")
        void additionEventShouldReturnTrue() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "new.key", null, "new_value", source, 1000L);

            assertThat(event.isAddition()).isTrue();
            assertThat(event.isModification()).isFalse();
            assertThat(event.isDeletion()).isFalse();
        }

        @Test
        @DisplayName("旧值不为 null、新值为 null 时应为删除配置")
        void deletionEventShouldReturnTrue() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "removed.key", "old_value", null, source, 1000L);

            assertThat(event.isDeletion()).isTrue();
            assertThat(event.isAddition()).isFalse();
            assertThat(event.isModification()).isFalse();
        }

        @Test
        @DisplayName("旧值和新值均不为 null 时应为修改配置")
        void modificationEventShouldReturnTrue() {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    "changed.key", "old_value", "new_value", source, 1000L);

            assertThat(event.isModification()).isTrue();
            assertThat(event.isAddition()).isFalse();
            assertThat(event.isDeletion()).isFalse();
        }
    }

    @Nested
    @DisplayName("相等性判断")
    class EqualityTests {

        @Test
        @DisplayName("相同参数的事件应相等")
        void eventsWithSameParametersShouldBeEqual() {
            ConfigChangeEvent e1 = new ConfigChangeEvent(
                    "key", "old", "new", source, 1000L);
            ConfigChangeEvent e2 = new ConfigChangeEvent(
                    "key", "old", "new", source, 1000L);

            assertThat(e1).isEqualTo(e2);
            assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
        }

        @Test
        @DisplayName("不同参数的事件应不相等")
        void eventsWithDifferentParametersShouldNotBeEqual() {
            ConfigChangeEvent e1 = new ConfigChangeEvent(
                    "key1", "old", "new", source, 1000L);
            ConfigChangeEvent e2 = new ConfigChangeEvent(
                    "key2", "old", "new", source, 1000L);

            assertThat(e1).isNotEqualTo(e2);
        }
    }

    @Test
    @DisplayName("toString 应包含所有字段信息")
    void toStringShouldContainAllFields() {
        ConfigChangeEvent event = new ConfigChangeEvent(
                "database.url", "old_url", "new_url", source, 2000L);

        String str = event.toString();
        assertThat(str).contains("database.url");
        assertThat(str).contains("old_url");
        assertThat(str).contains("new_url");
        assertThat(str).contains("NACOS");
    }
}
