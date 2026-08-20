package cz.tacr.elza.repository.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Value type of a column the AIP list can be filtered by.
 *
 * Ties a field of the contract to the filter model that may carry its value, and knows which
 * Java types the column behind it may have. A column is always compared in its own type; the
 * value is never re-typed to String.
 *
 * In Hibernate 6 {@code Expression.as(String.class)} is not a SQL cast but a wrapper that only
 * changes the reported expression type, so the bind parameter would be sent as varchar against
 * a numeric or boolean column - which PostgreSQL rejects, as it has no such operator.
 */
public enum AipFilterValueType {

    TEXT(FilterType.TEXT_VALUE),

    NUMBER(FilterType.NUMBER_VALUE),

    BOOLEAN(FilterType.BOOL_VALUE),

    DATE(FilterType.DATE_VALUE),

    ENUM(FilterType.ENUM_VALUE),

    /** Reference to another entity, carried as its id. */
    REF(FilterType.REF_VALUE);

    private final FilterType filterType;

    AipFilterValueType(final FilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * The filter model that may carry a value of this type.
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**
     * Whether a column of the given Java type can hold a value of this type.
     *
     * NUMBER and REF are both backed by an integer column, so this is a compatibility check
     * rather than an exact match - the distinction between them is a contract decision (a
     * range over an entity id has no meaning), not something the Java type can express.
     */
    public boolean accepts(final Class<?> javaType) {
        return switch (this) {
            case TEXT -> String.class.equals(javaType);
            case BOOLEAN -> Boolean.class.equals(javaType) || boolean.class.equals(javaType);
            case NUMBER, REF -> Number.class.isAssignableFrom(javaType)
                    || int.class.equals(javaType) || long.class.equals(javaType) || short.class.equals(javaType);
            case DATE -> LocalDateTime.class.equals(javaType) || LocalDate.class.equals(javaType);
            case ENUM -> javaType.isEnum();
        };
    }

    /**
     * Checks that a filter of the given type may carry a value for a field of this type.
     *
     * @throws BusinessException when the client applies a filter model to a field it does not
     *             fit, e.g. a text filter to a boolean column
     */
    public void checkFilterType(final FilterType actual, final String fieldName) {
        if (filterType != actual) {
            throw new BusinessException("Filtr " + actual.getValue() + " nelze použít pro pole '" + fieldName
                    + "', které je typu " + name() + "; použijte filtr " + filterType.getValue(),
                    BaseCode.PROPERTY_IS_INVALID)
                            .set(BaseCode.PARAM_PROPERTY, fieldName)
                            .set("filterType", actual.getValue())
                            .set("expectedFilterType", filterType.getValue());
        }
    }

    /**
     * Converts an enum constant name to the constant of the column's enum.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object parseEnum(final Class<?> javaType, final String value, final String fieldName) {
        requireValue(value, fieldName);
        try {
            return Enum.valueOf((Class<? extends Enum>) javaType, value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Hodnota '" + value + "' není platnou hodnotou pole '" + fieldName + "'",
                    e, BaseCode.PROPERTY_HAS_INVALID_TYPE)
                            .set(BaseCode.PARAM_PROPERTY, fieldName)
                            .set("value", value);
        }
    }

    /**
     * Lower bound of a date range - the start of the day, so the boundary day is part of it.
     */
    public static LocalDateTime rangeStart(final LocalDate value, final String fieldName) {
        requireValue(value, fieldName);
        return value.atStartOfDay();
    }

    /**
     * Upper bound of a date range - the end of the day, so the boundary day is part of it.
     */
    public static LocalDateTime rangeEnd(final LocalDate value, final String fieldName) {
        requireValue(value, fieldName);
        return value.atTime(LocalTime.MAX);
    }

    /**
     * A date column may be mapped as LocalDateTime or LocalDate; adapts the bound to either.
     */
    public static Object toDateBound(final Class<?> javaType, final LocalDateTime bound) {
        if (LocalDate.class.equals(javaType)) {
            return bound.toLocalDate();
        }
        if (LocalDateTime.class.equals(javaType)) {
            return bound;
        }
        throw new SystemException("Sloupec typu " + javaType.getName() + " není datum",
                BaseCode.INVATID_TYPE);
    }

    public static <T> T requireValue(final T value, final String fieldName) {
        if (value == null || (value instanceof String s && s.isEmpty())) {
            throw new BusinessException("Hodnota filtru pro pole '" + fieldName + "' není vyplněna",
                    BaseCode.PROPERTY_NOT_EXIST).set(BaseCode.PARAM_PROPERTY, fieldName);
        }
        return value;
    }
}
