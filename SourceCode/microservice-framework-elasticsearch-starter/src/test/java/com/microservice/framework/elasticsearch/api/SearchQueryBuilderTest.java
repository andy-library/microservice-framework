package com.microservice.framework.elasticsearch.api;

import com.microservice.framework.common.error.FrameworkException;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.MatchClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.TermClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.RangeClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.BoolClause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.Clause;
import com.microservice.framework.elasticsearch.api.SearchQueryBuilder.ClauseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SearchQueryBuilder 构建器模式测试。
 *
 * <p>验证 Fluent API 的构建行为、查询条件的正确性和参数验证。
 */
class SearchQueryBuilderTest {

    @Nested
    @DisplayName("构建器创建测试")
    class CreationTest {

        @Test
        @DisplayName("create() 应返回空的构建器实例")
        void createShouldReturnEmptyBuilder() {
            SearchQueryBuilder builder = SearchQueryBuilder.create();
            assertThat(builder.hasClauses()).isFalse();
            assertThat(builder.hasSorts()).isFalse();
            assertThat(builder.getFrom()).isEqualTo(0);
            assertThat(builder.getSize()).isEqualTo(10);
            assertThat(builder.getClauses()).isEmpty();
            assertThat(builder.getSorts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Match 查询测试")
    class MatchQueryTest {

        @Test
        @DisplayName("match() 应添加 MATCH 类型查询条件")
        void matchShouldAddMatchClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .match("title", "微服务");

            assertThat(builder.hasClauses()).isTrue();
            assertThat(builder.getClauses()).hasSize(1);
            Clause clause = builder.getClauses().get(0);
            assertThat(clause.type()).isEqualTo(ClauseType.MATCH);
            assertThat(clause).isInstanceOf(MatchClause.class);
            MatchClause matchClause = (MatchClause) clause;
            assertThat(matchClause.field()).isEqualTo("title");
            assertThat(matchClause.value()).isEqualTo("微服务");
            assertThat(matchClause.analyzer()).isNull();
        }

        @Test
        @DisplayName("match() 带 analyzer 应存储 analyzer 参数")
        void matchWithAnalyzerShouldStoreAnalyzer() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .match("title", "微服务", "ik_max_word");

            MatchClause matchClause = (MatchClause) builder.getClauses().get(0);
            assertThat(matchClause.analyzer()).isEqualTo("ik_max_word");
        }

        @Test
        @DisplayName("match() 空字段名应抛出 FrameworkException")
        void matchWithBlankFieldShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create().match("", "value"))
                    .isInstanceOf(FrameworkException.class);
        }

        @Test
        @DisplayName("match() null 值应抛出 FrameworkException")
        void matchWithNullValueShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create().match("field", null))
                    .isInstanceOf(FrameworkException.class);
        }
    }

    @Nested
    @DisplayName("Term 查询测试")
    class TermQueryTest {

        @Test
        @DisplayName("term() 应添加 TERM 类型查询条件")
        void termShouldAddTermClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .term("status", "active");

            assertThat(builder.hasClauses()).isTrue();
            assertThat(builder.getClauses()).hasSize(1);
            Clause clause = builder.getClauses().get(0);
            assertThat(clause.type()).isEqualTo(ClauseType.TERM);
            assertThat(clause).isInstanceOf(TermClause.class);
            TermClause termClause = (TermClause) clause;
            assertThat(termClause.field()).isEqualTo("status");
            assertThat(termClause.value()).isEqualTo("active");
        }

        @Test
        @DisplayName("term() 空字段名应抛出 FrameworkException")
        void termWithBlankFieldShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create().term(" ", "value"))
                    .isInstanceOf(FrameworkException.class);
        }
    }

    @Nested
    @DisplayName("Range 查询测试")
    class RangeQueryTest {

        @Test
        @DisplayName("range() 应添加 RANGE 类型查询条件（默认包含边界）")
        void rangeShouldAddRangeClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .range("createTime", "2024-01-01", "2024-12-31");

            assertThat(builder.hasClauses()).isTrue();
            assertThat(builder.getClauses()).hasSize(1);
            Clause clause = builder.getClauses().get(0);
            assertThat(clause.type()).isEqualTo(ClauseType.RANGE);
            assertThat(clause).isInstanceOf(RangeClause.class);
            RangeClause rangeClause = (RangeClause) clause;
            assertThat(rangeClause.field()).isEqualTo("createTime");
            assertThat(rangeClause.from()).isEqualTo("2024-01-01");
            assertThat(rangeClause.to()).isEqualTo("2024-12-31");
            assertThat(rangeClause.includeFrom()).isTrue();
            assertThat(rangeClause.includeTo()).isTrue();
        }

        @Test
        @DisplayName("range() 带 includeFrom/includeTo 应使用 RangeClause")
        void rangeWithIncludeFlagsShouldUseRangeClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .range("price", 100, 500, false, true);

            assertThat(builder.getClauses()).hasSize(1);
            assertThat(builder.getClauses().get(0)).isInstanceOf(RangeClause.class);
            RangeClause rangeClause = (RangeClause) builder.getClauses().get(0);
            assertThat(rangeClause.includeFrom()).isFalse();
            assertThat(rangeClause.includeTo()).isTrue();
        }

        @Test
        @DisplayName("range() null 下界和上界应允许开放范围")
        void rangeWithNullBoundsShouldAllowOpenRange() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .range("price", null, 500);

            assertThat(builder.getClauses()).hasSize(1);
            RangeClause rangeClause = (RangeClause) builder.getClauses().get(0);
            assertThat(rangeClause.from()).isNull();
        }
    }

    @Nested
    @DisplayName("Bool 查询测试")
    class BoolQueryTest {

        @Test
        @DisplayName("must() 应添加 MUST 类型 bool 子条件")
        void mustShouldAddMustClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .must("category", "tech");

            assertThat(builder.getClauses()).hasSize(1);
            assertThat(builder.getClauses().get(0)).isInstanceOf(BoolClause.class);
            BoolClause boolClause = (BoolClause) builder.getClauses().get(0);
            assertThat(boolClause.boolType()).isEqualTo(SearchQueryBuilder.BoolType.MUST);
        }

        @Test
        @DisplayName("should() 应添加 SHOULD 类型 bool 子条件")
        void shouldShouldAddShouldClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .should("tag", "java");

            BoolClause boolClause = (BoolClause) builder.getClauses().get(0);
            assertThat(boolClause.boolType()).isEqualTo(SearchQueryBuilder.BoolType.SHOULD);
        }

        @Test
        @DisplayName("mustNot() 应添加 MUST_NOT 类型 bool 子条件")
        void mustNotShouldAddMustNotClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .mustNot("status", "deleted");

            BoolClause boolClause = (BoolClause) builder.getClauses().get(0);
            assertThat(boolClause.boolType()).isEqualTo(SearchQueryBuilder.BoolType.MUST_NOT);
        }
    }

    @Nested
    @DisplayName("排序测试")
    class SortTest {

        @Test
        @DisplayName("sort() 应添加排序条件")
        void sortShouldAddSortClause() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .sort("createTime", SearchQueryBuilder.SortDirection.DESC);

            assertThat(builder.hasSorts()).isTrue();
            assertThat(builder.getSorts()).hasSize(1);
            SearchQueryBuilder.SortClause sort = builder.getSorts().get(0);
            assertThat(sort.field()).isEqualTo("createTime");
            assertThat(sort.direction()).isEqualTo(SearchQueryBuilder.SortDirection.DESC);
        }

        @Test
        @DisplayName("sort() null 方向应抛出 FrameworkException")
        void sortWithNullDirectionShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create()
                    .sort("field", null))
                    .isInstanceOf(FrameworkException.class);
        }
    }

    @Nested
    @DisplayName("分页测试")
    class PaginationTest {

        @Test
        @DisplayName("from() 应设置分页偏移")
        void fromShouldSetOffset() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().from(20);
            assertThat(builder.getFrom()).isEqualTo(20);
        }

        @Test
        @DisplayName("from() 负数应抛出 FrameworkException")
        void fromWithNegativeValueShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create().from(-1))
                    .isInstanceOf(FrameworkException.class);
        }

        @Test
        @DisplayName("size() 应设置返回条数")
        void sizeShouldSetPageSize() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().size(50);
            assertThat(builder.getSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("size() 0 应抛出 FrameworkException")
        void sizeWithZeroShouldThrow() {
            assertThatThrownBy(() -> SearchQueryBuilder.create().size(0))
                    .isInstanceOf(FrameworkException.class);
        }

        @Test
        @DisplayName("validateFromSize() 超过深分页上限应抛出 IllegalArgumentException")
        void validateFromSizeShouldRejectDeepPagination() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().from(9990).size(20);

            assertThatThrownBy(() -> builder.validateFromSize(10000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("from + size");
        }

        @Test
        @DisplayName("validateFromSize() 未超过深分页上限应通过")
        void validateFromSizeShouldAllowWithinLimit() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().from(9980).size(20);

            builder.validateFromSize(10000);
        }

        @Test
        @DisplayName("validateSize() 超过单次返回上限应抛出 IllegalArgumentException")
        void validateSizeShouldRejectOversizedQuery() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().size(1001);

            assertThatThrownBy(() -> builder.validateSize(1000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("max-size");
        }

        @Test
        @DisplayName("validateSize() 未超过单次返回上限应通过")
        void validateSizeShouldAllowWithinLimit() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().size(1000);

            builder.validateSize(1000);
        }
    }

    @Nested
    @DisplayName("链式调用测试")
    class ChainingTest {

        @Test
        @DisplayName("多个条件应支持链式调用组合")
        void multipleClausesShouldChain() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .match("title", "微服务")
                    .range("createTime", "2024-01-01", "2024-12-31")
                    .sort("createTime", SearchQueryBuilder.SortDirection.DESC)
                    .from(0)
                    .size(20);

            assertThat(builder.getClauses()).hasSize(2);
            assertThat(builder.getSorts()).hasSize(1);
            assertThat(builder.getFrom()).isEqualTo(0);
            assertThat(builder.getSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("getClauses() 应返回不可修改的列表")
        void getClausesShouldReturnUnmodifiableList() {
            SearchQueryBuilder builder = SearchQueryBuilder.create().match("field", "value");
            assertThatThrownBy(() -> builder.getClauses().add(
                    new MatchClause("x", "y", null)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getSorts() 应返回不可修改的列表")
        void getSortsShouldReturnUnmodifiableList() {
            SearchQueryBuilder builder = SearchQueryBuilder.create()
                    .sort("field", SearchQueryBuilder.SortDirection.ASC);
            assertThatThrownBy(() -> builder.getSorts().add(
                    new SearchQueryBuilder.SortClause("x", SearchQueryBuilder.SortDirection.DESC)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
