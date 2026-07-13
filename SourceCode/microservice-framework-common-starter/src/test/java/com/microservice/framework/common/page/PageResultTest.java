package com.microservice.framework.common.page;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PageResult 单元测试
 * <p>
 * 验证分页结果的工厂方法、hasNext 计算、totalPages 计算以及空结果。
 *
 * @author Andy Yang
 */
class PageResultTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("of() 工厂应创建完整分页结果")
        void ofFactoryShouldCreateFullPageResult() {
            List<String> items = Arrays.asList("a", "b", "c");
            PageResult<String> result = PageResult.of(100, items, 1, 20);

            assertThat(result.getTotal()).isEqualTo(100);
            assertThat(result.getItems()).isEqualTo(items);
            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("empty() 工厂应创建空分页结果")
        void emptyFactoryShouldCreateEmptyPageResult() {
            PageResult<String> result = PageResult.empty(1, 20);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getPageNumber()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("hasNext 计算")
    class HasNextCalculation {

        @Test
        @DisplayName("当总数超过当前页范围时 hasNext 应为 true")
        void hasNextShouldBeTrueWhenMorePagesExist() {
            List<String> items = Arrays.asList("a", "b");
            PageResult<String> result = PageResult.of(100, items, 1, 20);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("当总数刚好等于当前页范围时 hasNext 应为 false")
        void hasNextShouldBeFalseWhenNoMorePages() {
            List<String> items = Arrays.asList("a", "b");
            PageResult<String> result = PageResult.of(20, items, 1, 20);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("最后一页时 hasNext 应为 false")
        void hasNextShouldBeFalseOnLastPage() {
            List<String> items = Arrays.asList("a");
            PageResult<String> result = PageResult.of(21, items, 2, 20);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("空结果时 hasNext 应为 false")
        void hasNextShouldBeFalseForEmptyResult() {
            PageResult<String> result = PageResult.empty(1, 20);
            assertThat(result.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("totalPages 计算")
    class TotalPagesCalculation {

        @Test
        @DisplayName("总页数应向上取整")
        void totalPagesShouldRoundUp() {
            PageResult<String> result = PageResult.of(21, Collections.emptyList(), 1, 20);
            assertThat(result.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("整除时总页数应精确")
        void totalPagesShouldBeExactWhenDivisible() {
            PageResult<String> result = PageResult.of(100, Collections.emptyList(), 1, 20);
            assertThat(result.totalPages()).isEqualTo(5);
        }

        @Test
        @DisplayName("总数为 0 时总页数应为 0")
        void totalPagesShouldBeZeroWhenTotalIsZero() {
            PageResult<String> result = PageResult.empty(1, 20);
            assertThat(result.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("总数小于 pageSize 时总页数应为 1")
        void totalPagesShouldBeOneWhenTotalLessThanPageSize() {
            PageResult<String> result = PageResult.of(10, Collections.emptyList(), 1, 20);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("不可变性")
    class Immutability {

        @Test
        @DisplayName("items 列表应为不可变")
        void itemsShouldBeImmutable() {
            List<String> mutableItems = Arrays.asList("a", "b");
            PageResult<String> result = PageResult.of(2, mutableItems, 1, 20);
            assertThatThrownBy(() -> result.getItems().add("c"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("items 应与调用方的可变列表隔离")
        void itemsShouldBeDefensivelyCopied() {
            List<String> mutableItems = new ArrayList<>(Arrays.asList("a", "b"));
            PageResult<String> result = PageResult.of(2, mutableItems, 1, 20);

            mutableItems.clear();

            assertThat(result.getItems()).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("相等性与 toString")
    class EqualityAndToString {

        @Test
        @DisplayName("相同参数的 PageResult 应相等")
        void sameParametersShouldBeEqual() {
            List<String> items = Arrays.asList("a", "b");
            PageResult<String> result1 = PageResult.of(2, items, 1, 20);
            PageResult<String> result2 = PageResult.of(2, items, 1, 20);
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("toString 应包含关键信息")
        void toStringShouldContainKeyInfo() {
            PageResult<String> result = PageResult.of(100, Arrays.asList("a", "b"), 1, 20);
            assertThat(result.toString()).contains("total=100", "pageNumber=1", "pageSize=20");
        }
    }
}
