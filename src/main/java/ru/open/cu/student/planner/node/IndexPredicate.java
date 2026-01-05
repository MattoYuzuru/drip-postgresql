package ru.open.cu.student.planner.node;

import java.util.Objects;

public class IndexPredicate {

    public enum Type {
        EQUAL,
        RANGE,
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL
    }

    private final Type type;
    private final Comparable<?> value;
    private final Comparable<?> rangeFrom;
    private final Comparable<?> rangeTo;
    private final boolean includeFrom;
    private final boolean includeTo;

    private IndexPredicate(Type type,
                           Comparable<?> value,
                           Comparable<?> rangeFrom,
                           Comparable<?> rangeTo,
                           boolean includeFrom,
                           boolean includeTo) {
        this.type = type;
        this.value = value;
        this.rangeFrom = rangeFrom;
        this.rangeTo = rangeTo;
        this.includeFrom = includeFrom;
        this.includeTo = includeTo;
    }

    public static IndexPredicate equal(Comparable<?> value) {
        Objects.requireNonNull(value, "value");
        return new IndexPredicate(Type.EQUAL, value, null, null, true, true);
    }

    public static IndexPredicate range(Comparable<?> from, boolean includeFrom,
                                       Comparable<?> to, boolean includeTo) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return new IndexPredicate(Type.RANGE, null, from, to, includeFrom, includeTo);
    }

    public static IndexPredicate greaterThan(Comparable<?> value, boolean inclusive) {
        return new IndexPredicate(inclusive ? Type.GREATER_OR_EQUAL : Type.GREATER_THAN,
                Objects.requireNonNull(value), null, null, inclusive, false);
    }

    public static IndexPredicate lessThan(Comparable<?> value, boolean inclusive) {
        return new IndexPredicate(inclusive ? Type.LESS_OR_EQUAL : Type.LESS_THAN,
                Objects.requireNonNull(value), null, null, false, inclusive);
    }

    public Type getType() {
        return type;
    }

    public Comparable<?> getValue() {
        return value;
    }

    public Comparable<?> getRangeFrom() {
        return rangeFrom;
    }

    public Comparable<?> getRangeTo() {
        return rangeTo;
    }

    public boolean isIncludeFrom() {
        return includeFrom;
    }

    public boolean isIncludeTo() {
        return includeTo;
    }
}
