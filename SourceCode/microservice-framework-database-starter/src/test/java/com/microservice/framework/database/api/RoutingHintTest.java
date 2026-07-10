package com.microservice.framework.database.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RoutingHint 单元测试
 * <p>
 * 验证枚举值、静态工厂方法和判断方法的正确性。
 *
 * @author Andy Yang
 */
class RoutingHintTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValues {

        @Test
        @DisplayName("应包含 PRIMARY 和 READ_REPLICA 两个枚举值")
        void shouldContainTwoValues() {
            assertThat(RoutingHint.values()).hasSize(2);
            assertThat(RoutingHint.values()).containsExactly(RoutingHint.PRIMARY, RoutingHint.READ_REPLICA);
        }

        @Test
        @DisplayName("PRIMARY 名称应为 PRIMARY")
        void primaryShouldHaveCorrectName() {
            assertThat(RoutingHint.PRIMARY.name()).isEqualTo("PRIMARY");
        }

        @Test
        @DisplayName("READ_REPLICA 名称应为 READ_REPLICA")
        void readReplicaShouldHaveCorrectName() {
            assertThat(RoutingHint.READ_REPLICA.name()).isEqualTo("READ_REPLICA");
        }
    }

    @Nested
    @DisplayName("静态工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("primary() 应返回 PRIMARY")
        void primaryFactoryShouldReturnPrimary() {
            assertThat(RoutingHint.primary()).isEqualTo(RoutingHint.PRIMARY);
        }

        @Test
        @DisplayName("readReplica() 应返回 READ_REPLICA")
        void readReplicaFactoryShouldReturnReadReplica() {
            assertThat(RoutingHint.readReplica()).isEqualTo(RoutingHint.READ_REPLICA);
        }
    }

    @Nested
    @DisplayName("判断方法")
    class CheckMethods {

        @Test
        @DisplayName("PRIMARY.isPrimary() 应返回 true")
        void primaryIsPrimaryShouldReturnTrue() {
            assertThat(RoutingHint.PRIMARY.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("PRIMARY.isReadReplica() 应返回 false")
        void primaryIsReadReplicaShouldReturnFalse() {
            assertThat(RoutingHint.PRIMARY.isReadReplica()).isFalse();
        }

        @Test
        @DisplayName("READ_REPLICA.isPrimary() 应返回 false")
        void readReplicaIsPrimaryShouldReturnFalse() {
            assertThat(RoutingHint.READ_REPLICA.isPrimary()).isFalse();
        }

        @Test
        @DisplayName("READ_REPLICA.isReadReplica() 应返回 true")
        void readReplicaIsReadReplicaShouldReturnTrue() {
            assertThat(RoutingHint.READ_REPLICA.isReadReplica()).isTrue();
        }
    }

    @Test
    @DisplayName("valueOf() 应正确解析枚举名称")
    void valueOfShouldParseCorrectly() {
        assertThat(RoutingHint.valueOf("PRIMARY")).isEqualTo(RoutingHint.PRIMARY);
        assertThat(RoutingHint.valueOf("READ_REPLICA")).isEqualTo(RoutingHint.READ_REPLICA);
    }
}
