package com.microservice.framework.common.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable sort field specifying a property name and sort direction.
 *
 * @author Andy Yang
 */
public final class SortField {

    /** Sort direction enum. */
    public enum Direction {
        ASC,
        DESC
    }

    private final String field;
    private final Direction direction;

    private SortField(String field, Direction direction) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("field must be non-null and non-empty");
        }
        this.field = field;
        this.direction = Objects.requireNonNull(direction, "direction must be non-null");
    }

    /**
     * Static factory method to create a {@code SortField}.
     *
     * @param field     the property name to sort by
     * @param direction the sort direction
     * @return a new {@code SortField}
     * @throws IllegalArgumentException if field is null or empty
     * @throws NullPointerException     if direction is null
     */
    public static SortField of(String field, Direction direction) {
        return new SortField(field, direction);
    }

    /**
     * Convenience factory for ascending sort.
     *
     * @param field the property name
     * @return a new ascending {@code SortField}
     */
    public static SortField asc(String field) {
        return of(field, Direction.ASC);
    }

    /**
     * Convenience factory for descending sort.
     *
     * @param field the property name
     * @return a new descending {@code SortField}
     */
    public static SortField desc(String field) {
        return of(field, Direction.DESC);
    }

    public String getField() {
        return field;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SortField)) {
            return false;
        }
        SortField other = (SortField) obj;
        return field.equals(other.field) && direction == other.direction;
    }

    @Override
    public int hashCode() {
        return 31 * field.hashCode() + direction.hashCode();
    }

    @Override
    public String toString() {
        return field + " " + direction.name();
    }
}
