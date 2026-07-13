package com.microservice.framework.common.page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, generic page result model.
 * <p>
 * Not a Java record for Jackson serialization compatibility, but follows
 * record-style semantics (final fields, getters, static factories).
 *
 * @param <T> the type of items in the page
 * @author Andy Yang
 */
public final class PageResult<T> {

    private final long total;
    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;

    private PageResult(long total, List<T> items, int pageNumber, int pageSize) {
        this.total = total;
        this.items = List.copyOf(Objects.requireNonNull(items, "items must be non-null"));
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    /**
     * Static factory for a populated page result.
     *
     * @param total      total number of items across all pages
     * @param items      items on the current page
     * @param pageNumber current page number (1-based)
     * @param pageSize   items per page
     * @param <T>        item type
     * @return a new {@code PageResult}
     */
    public static <T> PageResult<T> of(long total, List<T> items, int pageNumber, int pageSize) {
        return new PageResult<>(total, items, pageNumber, pageSize);
    }

    /**
     * Static factory for an empty page result.
     *
     * @param pageNumber current page number (1-based)
     * @param pageSize   items per page
     * @param <T>        item type
     * @return an empty {@code PageResult} with total=0
     */
    public static <T> PageResult<T> empty(int pageNumber, int pageSize) {
        return new PageResult<>(0, Collections.emptyList(), pageNumber, pageSize);
    }

    public long getTotal() {
        return total;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns whether there are more pages after the current one.
     *
     * @return true if total exceeds the items covered up to this page
     */
    public boolean hasNext() {
        return total > (long) pageNumber * pageSize;
    }

    /**
     * Returns the total number of pages.
     *
     * @return total pages, computed as ceil(total / pageSize)
     */
    public long totalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (total + pageSize - 1) / pageSize;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageResult)) {
            return false;
        }
        PageResult<?> other = (PageResult<?>) obj;
        return total == other.total
                && pageNumber == other.pageNumber
                && pageSize == other.pageSize
                && items.equals(other.items);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(total);
        result = 31 * result + items.hashCode();
        result = 31 * result + Integer.hashCode(pageNumber);
        result = 31 * result + Integer.hashCode(pageSize);
        return result;
    }

    @Override
    public String toString() {
        return "PageResult{total=" + total
                + ", items=" + items.size()
                + ", pageNumber=" + pageNumber
                + ", pageSize=" + pageSize + "}";
    }
}
