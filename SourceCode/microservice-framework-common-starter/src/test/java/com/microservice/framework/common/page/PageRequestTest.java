package com.microservice.framework.common.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PageRequest 单元测试
 * <p>
 * 验证分页请求的构建器模式、默认值、参数校验以及排序字段。
 *
 * @author Andy Yang
 */
class PageRequestTest {

    @Nested
    @DisplayName("构建器模式")
    class BuilderPattern {

        @Test
        @DisplayName("默认构建应 pageNumber=1 pageSize=20")
        void defaultBuilderShouldHaveDefaultValues() {
            PageRequest request = PageRequest.builder().build();
            assertThat(request.getPageNumber()).isEqualTo(1);
            assertThat(request.getPageSize()).isEqualTo(20);
            assertThat(request.getSortFields()).isEmpty();
        }

        @Test
        @DisplayName("构建器应支持设置所有字段")
        void builderShouldSupportAllFields() {
            PageRequest request = PageRequest.builder()
                    .pageNumber(3)
                    .pageSize(50)
                    .sortField(SortField.asc("name"))
                    .sortField(SortField.desc("createdAt"))
                    .build();
            assertThat(request.getPageNumber()).isEqualTo(3);
            assertThat(request.getPageSize()).isEqualTo(50);
            assertThat(request.getSortFields()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("默认 pageNumber 应为 1")
        void defaultPageNumberShouldBe1() {
            PageRequest request = PageRequest.builder().build();
            assertThat(request.getPageNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("默认 pageSize 应为 20")
        void defaultPageSizeShouldBe20() {
            PageRequest request = PageRequest.builder().build();
            assertThat(request.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("参数校验")
    class Validation {

        @Test
        @DisplayName("pageNumber 小于 1 应抛出 IllegalArgumentException")
        void pageNumberLessThanOneShouldThrow() {
            assertThatThrownBy(() -> PageRequest.builder().pageNumber(0).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageNumber must be >= 1");
        }

        @Test
        @DisplayName("pageNumber 为负数应抛出 IllegalArgumentException")
        void negativePageNumberShouldThrow() {
            assertThatThrownBy(() -> PageRequest.builder().pageNumber(-1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageNumber must be >= 1");
        }

        @Test
        @DisplayName("pageSize 小于 1 应抛出 IllegalArgumentException")
        void pageSizeLessThanOneShouldThrow() {
            assertThatThrownBy(() -> PageRequest.builder().pageSize(0).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageSize must be between 1 and 100");
        }

        @Test
        @DisplayName("pageSize 大于 100 应抛出 IllegalArgumentException")
        void pageSizeAboveMaxShouldThrow() {
            assertThatThrownBy(() -> PageRequest.builder().pageSize(101).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageSize must be between 1 and 100");
        }

        @Test
        @DisplayName("pageSize 边界值 1 应正常")
        void pageSizeAtLowerBoundShouldWork() {
            PageRequest request = PageRequest.builder().pageSize(1).build();
            assertThat(request.getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("pageSize 边界值 100 应正常")
        void pageSizeAtUpperBoundShouldWork() {
            PageRequest request = PageRequest.builder().pageSize(100).build();
            assertThat(request.getPageSize()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("排序字段")
    class SortFields {

        @Test
        @DisplayName("添加排序字段应保留顺序")
        void sortFieldsShouldPreserveOrder() {
            PageRequest request = PageRequest.builder()
                    .sortField(SortField.asc("name"))
                    .sortField(SortField.desc("date"))
                    .build();
            assertThat(request.getSortFields()).hasSize(2);
            assertThat(request.getSortFields().get(0).getField()).isEqualTo("name");
            assertThat(request.getSortFields().get(1).getField()).isEqualTo("date");
        }

        @Test
        @DisplayName("sortFields 列表应为不可变")
        void sortFieldsShouldBeImmutable() {
            PageRequest request = PageRequest.builder()
                    .sortField(SortField.asc("name"))
                    .build();
            assertThatThrownBy(() -> request.getSortFields().add(SortField.desc("date")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("替换排序字段应覆盖之前的")
        void replacingSortFieldsShouldOverridePrevious() {
            PageRequest request = PageRequest.builder()
                    .sortField(SortField.asc("name"))
                    .sortFields(Collections.singletonList(SortField.desc("date")))
                    .build();
            assertThat(request.getSortFields()).hasSize(1);
            assertThat(request.getSortFields().get(0).getField()).isEqualTo("date");
        }
    }

    @Nested
    @DisplayName("toBuilder 修改")
    class ToBuilderModification {

        @Test
        @DisplayName("toBuilder 应复制原有值")
        void toBuilderShouldCopyOriginalValues() {
            PageRequest original = PageRequest.builder()
                    .pageNumber(3)
                    .pageSize(50)
                    .sortField(SortField.asc("name"))
                    .build();
            PageRequest modified = original.toBuilder()
                    .pageSize(30)
                    .build();
            assertThat(modified.getPageNumber()).isEqualTo(3);
            assertThat(modified.getPageSize()).isEqualTo(30);
            assertThat(modified.getSortFields()).hasSize(1);
        }

        @Test
        @DisplayName("toBuilder 修改不应影响原对象")
        void toBuilderModificationShouldNotAffectOriginal() {
            PageRequest original = PageRequest.builder()
                    .pageNumber(3)
                    .pageSize(50)
                    .build();
            PageRequest modified = original.toBuilder()
                    .pageSize(30)
                    .build();
            assertThat(original.getPageSize()).isEqualTo(50);
            assertThat(modified.getPageSize()).isEqualTo(30);
        }
    }
}
