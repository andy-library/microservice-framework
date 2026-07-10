package com.microservice.framework.common.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SortField 单元测试
 * <p>
 * 验证排序字段的工厂方法、方向以及空字段校验。
 *
 * @author Andy Yang
 */
class SortFieldTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("of() 应创建指定方向的排序字段")
        void ofShouldCreateSortFieldWithDirection() {
            SortField field = SortField.of("name", SortField.Direction.ASC);
            assertThat(field.getField()).isEqualTo("name");
            assertThat(field.getDirection()).isEqualTo(SortField.Direction.ASC);
        }

        @Test
        @DisplayName("asc() 应创建升序排序字段")
        void ascShouldCreateAscendingSortField() {
            SortField field = SortField.asc("name");
            assertThat(field.getField()).isEqualTo("name");
            assertThat(field.getDirection()).isEqualTo(SortField.Direction.ASC);
        }

        @Test
        @DisplayName("desc() 应创建降序排序字段")
        void descShouldCreateDescendingSortField() {
            SortField field = SortField.desc("createdAt");
            assertThat(field.getField()).isEqualTo("createdAt");
            assertThat(field.getDirection()).isEqualTo(SortField.Direction.DESC);
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ParameterValidation {

        @Test
        @DisplayName("null 字段应抛出 IllegalArgumentException")
        void nullFieldShouldThrow() {
            assertThatThrownBy(() -> SortField.of(null, SortField.Direction.ASC))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field must be non-null and non-empty");
        }

        @Test
        @DisplayName("空字段应抛出 IllegalArgumentException")
        void emptyFieldShouldThrow() {
            assertThatThrownBy(() -> SortField.of("", SortField.Direction.ASC))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("field must be non-null and non-empty");
        }

        @Test
        @DisplayName("null 方向应抛出 NullPointerException")
        void nullDirectionShouldThrow() {
            assertThatThrownBy(() -> SortField.of("name", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 toString")
    class EqualityAndToString {

        @Test
        @DisplayName("相同字段和方向的 SortField 应相等")
        void sameFieldAndDirectionShouldBeEqual() {
            SortField field1 = SortField.asc("name");
            SortField field2 = SortField.asc("name");
            assertThat(field1).isEqualTo(field2);
            assertThat(field1.hashCode()).isEqualTo(field2.hashCode());
        }

        @Test
        @DisplayName("不同方向的 SortField 应不相等")
        void differentDirectionShouldNotBeEqual() {
            SortField ascField = SortField.asc("name");
            SortField descField = SortField.desc("name");
            assertThat(ascField).isNotEqualTo(descField);
        }

        @Test
        @DisplayName("toString 应包含字段名和方向")
        void toStringShouldContainFieldAndDirection() {
            assertThat(SortField.asc("name").toString()).isEqualTo("name ASC");
            assertThat(SortField.desc("date").toString()).isEqualTo("date DESC");
        }
    }
}
