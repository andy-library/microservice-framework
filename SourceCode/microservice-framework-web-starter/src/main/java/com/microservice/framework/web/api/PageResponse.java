package com.microservice.framework.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Paginated API response extending {@link ApiResponse} with pagination metadata.
 * <p>
 * Adds total count, current page, page size, and a "has next page" flag
 * to the standard response envelope. Items are carried in the {@code data}
 * field as a {@link List}.
 * <p>
 * Instances are immutable; use static factory methods to create.
 *
 * @param <T> the type of items in the page
 * @author Andy Yang
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PageResponse<T> extends ApiResponse<List<T>> {

    private final long total;
    private final int page;
    private final int pageSize;
    private final boolean hasNext;

    private PageResponse(int code, String message, List<T> data,
                         Long timestamp, String requestId,
                         long total, int page, int pageSize, boolean hasNext) {
        super(code, message, data, timestamp, requestId);
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
    }

    // ======================================================================
    // Static factory methods
    // ======================================================================

    /**
     * Create a paginated success response.
     *
     * @param items    the page items
     * @param total    total number of items across all pages
     * @param page     current page number (1-based)
     * @param pageSize items per page
     * @return PageResponse with default success code
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int pageSize) {
        boolean hasNext = total > (long) page * pageSize;
        return new PageResponse<>(0, "success", items,
                System.currentTimeMillis(), null,
                total, page, pageSize, hasNext);
    }

    /**
     * Create a paginated success response with custom message.
     *
     * @param message  custom success message
     * @param items    the page items
     * @param total    total number of items across all pages
     * @param page     current page number (1-based)
     * @param pageSize items per page
     * @return PageResponse with default success code and custom message
     */
    public static <T> PageResponse<T> of(String message, List<T> items, long total, int page, int pageSize) {
        boolean hasNext = total > (long) page * pageSize;
        return new PageResponse<>(0, message, items,
                System.currentTimeMillis(), null,
                total, page, pageSize, hasNext);
    }

    /**
     * Create an empty paginated success response.
     *
     * @param page     current page number (1-based)
     * @param pageSize items per page
     * @return PageResponse with empty data and total=0
     */
    public static <T> PageResponse<T> empty(int page, int pageSize) {
        return new PageResponse<>(0, "success", List.of(),
                System.currentTimeMillis(), null,
                0, page, pageSize, false);
    }

    /**
     * Create a paginated error response.
     *
     * @param code    error code
     * @param message error message
     * @return PageResponse with error code and message, no data
     */
    public static <T> PageResponse<T> pageError(int code, String message) {
        return new PageResponse<>(code, message, null,
                System.currentTimeMillis(), null,
                0, 0, 0, false);
    }

    /**
     * Create a response with all fields explicitly set.
     *
     * @param code      response code
     * @param message   response message
     * @param data      page items (may be null)
     * @param timestamp epoch millis (may be null)
     * @param requestId request ID (may be null)
     * @param total     total count
     * @param page      page number
     * @param pageSize  page size
     * @param hasNext   whether next page exists
     * @return PageResponse with all fields set
     */
    public static <T> PageResponse<T> full(int code, String message, List<T> data,
                                            Long timestamp, String requestId,
                                            long total, int page, int pageSize, boolean hasNext) {
        return new PageResponse<>(code, message, data, timestamp, requestId,
                total, page, pageSize, hasNext);
    }

    // ======================================================================
    // Builder-style withXxx methods
    // ======================================================================

    /**
     * Return a copy of this response with the request ID set.
     */
    public PageResponse<T> withRequestId(String requestId) {
        return new PageResponse<>(this.getCode(), this.getMessage(), this.getData(),
                this.getTimestamp(), requestId,
                this.total, this.page, this.pageSize, this.hasNext);
    }

    /**
     * Return a copy of this response with the timestamp removed.
     */
    public PageResponse<T> withoutTimestamp() {
        return new PageResponse<>(this.getCode(), this.getMessage(), this.getData(),
                null, this.getRequestId(),
                this.total, this.page, this.pageSize, this.hasNext);
    }

    /**
     * Return a copy of this response with the request ID removed.
     */
    public PageResponse<T> withoutRequestId() {
        return new PageResponse<>(this.getCode(), this.getMessage(), this.getData(),
                this.getTimestamp(), null,
                this.total, this.page, this.pageSize, this.hasNext);
    }

    // ======================================================================
    // Getters
    // ======================================================================

    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public boolean getHasNext() { return hasNext; }

    // ======================================================================
    // equals / hashCode / toString
    // ======================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PageResponse)) return false;
        PageResponse<?> other = (PageResponse<?>) obj;
        return super.equals(obj)
                && total == other.total
                && page == other.page
                && pageSize == other.pageSize
                && hasNext == other.hasNext;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Long.hashCode(total);
        result = 31 * result + Integer.hashCode(page);
        result = 31 * result + Integer.hashCode(pageSize);
        result = 31 * result + Boolean.hashCode(hasNext);
        return result;
    }

    @Override
    public String toString() {
        return "PageResponse{code=" + getCode()
                + ", message='" + getMessage() + '\''
                + ", total=" + total
                + ", page=" + page
                + ", pageSize=" + pageSize
                + ", hasNext=" + hasNext
                + ", requestId='" + getRequestId() + '\''
                + '}';
    }
}
