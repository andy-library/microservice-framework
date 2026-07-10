package com.microservice.framework.elasticsearch.api;

import com.microservice.framework.common.error.FrameworkErrorCode;
import com.microservice.framework.common.error.FrameworkException;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索查询构建器，提供 Fluent API 用于构建 Elasticsearch 查询。
 *
 * <p>本构建器支持 match、term、range、bool 查询以及排序和分页参数，
 * 最终生成可传递给 {@link ElasticsearchOperations#search} 的查询对象。
 *
 * <p>使用示例：
 * <pre>{@code
 * SearchQueryBuilder builder = SearchQueryBuilder.create()
 *     .match("title", "微服务")
 *     .range("createTime", "2024-01-01", "2024-12-31")
 *     .sort("createTime", SortDirection.DESC)
 *     .from(0)
 *     .size(20);
 * }</pre>
 *
 * @see ElasticsearchOperations
 */
public class SearchQueryBuilder {

    /** 错误码模块标识。 */
    private static final String MODULE = "ES";

    /** 查询条件列表。 */
    private final List<Clause> clauses = new ArrayList<>();

    /** 排序条件列表。 */
    private final List<SortClause> sorts = new ArrayList<>();

    /** 分页起始偏移，默认 {@code 0}。 */
    private int from = 0;

    /** 分页返回条数，默认 {@code 10}。 */
    private int size = 10;

    private SearchQueryBuilder() {
    }

    /**
     * 创建新的搜索查询构建器。
     *
     * @return 空的构建器实例
     */
    public static SearchQueryBuilder create() {
        return new SearchQueryBuilder();
    }

    // ========================================================================
    // Match 查询
    // ========================================================================

    /**
     * 添加 match 查询条件（全文搜索）。
     *
     * @param field 字段名
     * @param value 搜索值
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder match(String field, Object value) {
        validateField(field);
        validateValue(value);
        clauses.add(new MatchClause(field, value, null));
        return this;
    }

    /**
     * 添加 match 查询条件（全文搜索，指定 analyzer）。
     *
     * @param field    字段名
     * @param value    搜索值
     * @param analyzer 分词器名称
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder match(String field, Object value, String analyzer) {
        validateField(field);
        validateValue(value);
        clauses.add(new MatchClause(field, value, analyzer));
        return this;
    }

    // ========================================================================
    // Term 查询
    // ========================================================================

    /**
     * 添加 term 查询条件（精确匹配）。
     *
     * @param field 字段名
     * @param value 精确匹配值
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder term(String field, Object value) {
        validateField(field);
        validateValue(value);
        clauses.add(new TermClause(field, value));
        return this;
    }

    // ========================================================================
    // Range 查询
    // ========================================================================

    /**
     * 添加 range 查询条件（范围查询，包含边界）。
     *
     * @param field  字段名
     * @param from   范围下界（gte），可为 null 表示无下界
     * @param to     范围上界（lte），可为 null 表示无上界
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder range(String field, Object from, Object to) {
        validateField(field);
        clauses.add(new RangeClause(field, from, to, true, true));
        return this;
    }

    /**
     * 添加 range 查询条件（范围查询，指定边界包含策略）。
     *
     * @param field       字段名
     * @param from        范围下界，可为 null
     * @param to          范围上界，可为 null
     * @param includeFrom 下界是否包含（gte / gt）
     * @param includeTo   上界是否包含（lte / lt）
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder range(String field, Object from, Object to,
                                     boolean includeFrom, boolean includeTo) {
        validateField(field);
        clauses.add(new RangeClause(field, from, to, includeFrom, includeTo));
        return this;
    }

    // ========================================================================
    // Bool 查询
    // ========================================================================

    /**
     * 添加 bool 查询的 must 子条件。
     *
     * <p>must 子条件会被组合到当前查询的 bool must 分支中。
     *
     * @param field 字段名
     * @param value 匹配值
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder must(String field, Object value) {
        validateField(field);
        validateValue(value);
        clauses.add(new BoolClause(BoolType.MUST, field, value));
        return this;
    }

    /**
     * 添加 bool 查询的 should 子条件。
     *
     * @param field 字段名
     * @param value 匹配值
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder should(String field, Object value) {
        validateField(field);
        validateValue(value);
        clauses.add(new BoolClause(BoolType.SHOULD, field, value));
        return this;
    }

    /**
     * 添加 bool 查询的 mustNot 子条件。
     *
     * @param field 字段名
     * @param value 匹配值
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder mustNot(String field, Object value) {
        validateField(field);
        validateValue(value);
        clauses.add(new BoolClause(BoolType.MUST_NOT, field, value));
        return this;
    }

    // ========================================================================
    // 排序
    // ========================================================================

    /**
     * 添加排序条件。
     *
     * @param field     排序字段名
     * @param direction 排序方向
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder sort(String field, SortDirection direction) {
        validateField(field);
        if (direction == null) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "QUERY", 2),
                    "Sort direction must not be null");
        }
        sorts.add(new SortClause(field, direction));
        return this;
    }

    // ========================================================================
    // 分页
    // ========================================================================

    /**
     * 设置分页起始偏移。
     *
     * @param from 偏移量，必须 >= 0
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder from(int from) {
        if (from < 0) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "QUERY", 3),
                    "From offset must be >= 0, got: " + from);
        }
        this.from = from;
        return this;
    }

    /**
     * 设置分页返回条数。
     *
     * @param size 返回条数，必须 >= 1
     * @return 当前构建器（链式调用）
     */
    public SearchQueryBuilder size(int size) {
        if (size < 1) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "QUERY", 4),
                    "Size must be >= 1, got: " + size);
        }
        this.size = size;
        return this;
    }

    // ========================================================================
    // 构建结果
    // ========================================================================

    /**
     * 获取所有查询条件。
     *
     * @return 查询条件列表（不可修改）
     */
    public List<Clause> getClauses() {
        return List.copyOf(clauses);
    }

    /**
     * 获取所有排序条件。
     *
     * @return 排序条件列表（不可修改）
     */
    public List<SortClause> getSorts() {
        return List.copyOf(sorts);
    }

    /**
     * 获取分页起始偏移。
     *
     * @return 偏移量
     */
    public int getFrom() {
        return from;
    }

    /**
     * 获取分页返回条数。
     *
     * @return 返回条数
     */
    public int getSize() {
        return size;
    }

    /**
     * 判断构建器是否包含任何查询条件。
     *
     * @return 若有至少一个查询条件则返回 {@code true}
     */
    public boolean hasClauses() {
        return !clauses.isEmpty();
    }

    /**
     * 判断构建器是否包含任何排序条件。
     *
     * @return 若有至少一个排序条件则返回 {@code true}
     */
    public boolean hasSorts() {
        return !sorts.isEmpty();
    }

    /**
     * 校验 from + size 是否超过治理上限。
     *
     * @param maxFromSize 最大分页窗口
     */
    public void validateFromSize(int maxFromSize) {
        if ((long) from + size > maxFromSize) {
            throw new IllegalArgumentException("Elasticsearch deep pagination is not allowed: from + size = "
                    + ((long) from + size) + ", max-from-size = " + maxFromSize);
        }
    }

    /**
     * 校验单次查询 size 是否超过治理上限。
     *
     * @param maxSize 最大单次返回条数
     */
    public void validateSize(int maxSize) {
        if (size > maxSize) {
            throw new IllegalArgumentException("Elasticsearch query size is too large: size = "
                    + size + ", max-size = " + maxSize);
        }
    }

    // ========================================================================
    // 验证辅助方法
    // ========================================================================

    private void validateField(String field) {
        if (field == null || field.isBlank()) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "QUERY", 1),
                    "Field name must not be blank");
        }
    }

    private void validateValue(Object value) {
        if (value == null) {
            throw new FrameworkException(
                    FrameworkErrorCode.of(MODULE, "QUERY", 1),
                    "Query value must not be null");
        }
    }

    // ========================================================================
    // 查询类型枚举
    // ========================================================================

    /** 查询类型。 */
    public enum ClauseType {
        MATCH, TERM, RANGE, BOOL
    }

    /** Bool 查询子类型。 */
    public enum BoolType {
        MUST, SHOULD, MUST_NOT
    }

    /** 排序方向。 */
    public enum SortDirection {
        ASC, DESC
    }

    // ========================================================================
    // 内部数据结构 - 查询条件密封接口与具体子类型
    // ========================================================================

    /**
     * 查询条件密封接口，所有查询子句类型实现此接口。
     *
     * <p>通过 {@link #type()} 获取子句类型，用于在查询构建时区分不同查询类型。
     */
    public sealed interface Clause permits MatchClause, TermClause, RangeClause, BoolClause {

        /** 获取查询子句类型。 */
        ClauseType type();

        /** 获取字段名。 */
        String field();
    }

    /**
     * Match 查询条件（全文搜索）。
     *
     * @param field    字段名
     * @param value    搜索值
     * @param analyzer 分词器名称，可为 null
     */
    public record MatchClause(String field, Object value, String analyzer) implements Clause {
        @Override
        public ClauseType type() {
            return ClauseType.MATCH;
        }
    }

    /**
     * Term 查询条件（精确匹配）。
     *
     * @param field 字段名
     * @param value 精确匹配值
     */
    public record TermClause(String field, Object value) implements Clause {
        @Override
        public ClauseType type() {
            return ClauseType.TERM;
        }
    }

    /**
     * Range 查询条件（范围查询）。
     *
     * @param field       字段名
     * @param from        范围下界，可为 null
     * @param to          范围上界，可为 null
     * @param includeFrom 下界是否包含（gte / gt）
     * @param includeTo   上界是否包含（lte / lt）
     */
    public record RangeClause(String field, Object from, Object to,
                               boolean includeFrom, boolean includeTo) implements Clause {
        @Override
        public ClauseType type() {
            return ClauseType.RANGE;
        }
    }

    /**
     * Bool 查询子条件。
     *
     * @param boolType bool 子类型
     * @param field    字段名
     * @param value    匹配值
     */
    public record BoolClause(BoolType boolType, String field, Object value) implements Clause {
        @Override
        public ClauseType type() {
            return ClauseType.BOOL;
        }
    }

    /**
     * 排序条件。
     *
     * @param field     排序字段名
     * @param direction 排序方向
     */
    public record SortClause(String field, SortDirection direction) {
    }
}
