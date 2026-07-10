package com.microservice.framework.common.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Unified, framework-agnostic page request model.
 * <p>
 * Page numbers are 1-based. Page size defaults to 20 and is capped at 100.
 * Supports sorting via an immutable list of {@link SortField}.
 * <p>
 * Instances are immutable; use the builder or {@link #toBuilder()} to create variants.
 *
 * @author Andy Yang
 */
public final class PageRequest {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final List<SortField> sortFields;

    private PageRequest(Builder builder) {
        this.pageNumber = builder.pageNumber;
        this.pageSize = builder.pageSize;
        this.sortFields = Collections.unmodifiableList(new ArrayList<>(builder.sortFields));
    }

    /**
     * Returns a new builder with default values (pageNumber=1, pageSize=20).
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder pre-populated with this request's values for modification.
     *
     * @return a builder copied from this instance
     */
    public Builder toBuilder() {
        return new Builder()
                .pageNumber(this.pageNumber)
                .pageSize(this.pageSize)
                .sortFields(this.sortFields);
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<SortField> getSortFields() {
        return sortFields;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageRequest)) {
            return false;
        }
        PageRequest other = (PageRequest) obj;
        return pageNumber == other.pageNumber
                && pageSize == other.pageSize
                && sortFields.equals(other.sortFields);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(pageNumber);
        result = 31 * result + Integer.hashCode(pageSize);
        result = 31 * result + sortFields.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PageRequest{pageNumber=" + pageNumber
                + ", pageSize=" + pageSize
                + ", sortFields=" + sortFields + "}";
    }

    /**
     * Builder for {@link PageRequest}.
     */
    public static final class Builder {

        private int pageNumber = DEFAULT_PAGE_NUMBER;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private final ArrayList<SortField> sortFields = new ArrayList<>();

        private Builder() {
        }

        /**
         * Sets the page number (1-based). Must be at least 1.
         *
         * @param pageNumber 1-based page number
         * @return this builder
         * @throws IllegalArgumentException if pageNumber &lt; 1
         */
        public Builder pageNumber(int pageNumber) {
            if (pageNumber < 1) {
                throw new IllegalArgumentException("pageNumber must be >= 1, but was: " + pageNumber);
            }
            this.pageNumber = pageNumber;
            return this;
        }

        /**
         * Sets the page size. Must be between 1 and 100.
         *
         * @param pageSize number of items per page
         * @return this builder
         * @throws IllegalArgumentException if pageSize is out of range
         */
        public Builder pageSize(int pageSize) {
            if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException(
                        "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", but was: " + pageSize);
            }
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Adds a sort field.
         *
         * @param sortField the sort specification
         * @return this builder
         */
        public Builder sortField(SortField sortField) {
            this.sortFields.add(Objects.requireNonNull(sortField, "sortField must be non-null"));
            return this;
        }

        /**
         * Replaces all sort fields with the given list.
         *
         * @param sortFields sort specifications (may be empty)
         * @return this builder
         */
        public Builder sortFields(List<SortField> sortFields) {
            this.sortFields.clear();
            this.sortFields.addAll(Objects.requireNonNull(sortFields, "sortFields must be non-null"));
            return this;
        }

        /**
         * Builds an immutable {@link PageRequest}.
         *
         * @return a new {@link PageRequest}
         */
        public PageRequest build() {
            return new PageRequest(this);
        }
    }
}
