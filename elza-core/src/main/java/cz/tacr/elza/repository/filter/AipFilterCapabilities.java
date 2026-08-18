package cz.tacr.elza.repository.filter;

import java.util.EnumSet;
import java.util.Set;

import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.controller.vo.OperationEqualityType;
import cz.tacr.elza.controller.vo.OperationNumberType;
import cz.tacr.elza.controller.vo.OperationRangeType;
import cz.tacr.elza.controller.vo.OperationTextType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * The part of the shared filter contract that the AIP list implements.
 *
 * The contract is deliberately wider than any single endpoint supports, so each endpoint
 * states what it can do and refuses the rest. Anything not listed here is rejected with a
 * message naming it, rather than being silently ignored or turned into a query that does
 * something else.
 */
public final class AipFilterCapabilities {

    /**
     * Filter models the AIP list understands.
     *
     * MultimatchContainsFilter is absent because da_aip has no fulltext index, and
     * FieldValueFilter because its value is untyped - the typed filters replace it here.
     */
    private static final Set<FilterType> SUPPORTED_FILTERS = EnumSet.of(
            FilterType.TEXT_VALUE,
            FilterType.NUMBER_VALUE,
            FilterType.BOOL_VALUE,
            FilterType.DATE_VALUE,
            FilterType.ENUM_VALUE,
            FilterType.REF_VALUE,
            FilterType.LOGICAL);

    private static final Set<OperationTextType> TEXT_OPERATIONS = EnumSet.of(
            OperationTextType.EQ,
            OperationTextType.CONTAINS,
            OperationTextType.NOT_CONTAINS,
            OperationTextType.IS_NULL,
            OperationTextType.NOT_NULL);

    private static final Set<OperationNumberType> NUMBER_OPERATIONS = EnumSet.of(
            OperationNumberType.EQ,
            OperationNumberType.BETWEEN,
            OperationNumberType.IS_NULL,
            OperationNumberType.NOT_NULL);

    private static final Set<OperationEqualityType> EQUALITY_OPERATIONS = EnumSet.of(
            OperationEqualityType.EQ,
            OperationEqualityType.IS_NULL,
            OperationEqualityType.NOT_NULL);

    private static final Set<OperationRangeType> RANGE_OPERATIONS = EnumSet.of(
            OperationRangeType.BETWEEN,
            OperationRangeType.IS_NULL,
            OperationRangeType.NOT_NULL);

    private AipFilterCapabilities() {
    }

    public static void checkFilterSupported(final FilterType filterType) {
        if (!SUPPORTED_FILTERS.contains(filterType)) {
            throw new BusinessException("Filtr " + filterType.getValue()
                    + " není při vyhledávání AIP podporován, podporované filtry: " + values(SUPPORTED_FILTERS),
                    BaseCode.PROPERTY_IS_INVALID).set("filterType", filterType.getValue());
        }
    }

    public static void checkOperationSupported(final OperationTextType operation, final String fieldName) {
        check(TEXT_OPERATIONS, operation, operation.getValue(), fieldName);
    }

    public static void checkOperationSupported(final OperationNumberType operation, final String fieldName) {
        check(NUMBER_OPERATIONS, operation, operation.getValue(), fieldName);
    }

    public static void checkOperationSupported(final OperationEqualityType operation, final String fieldName) {
        check(EQUALITY_OPERATIONS, operation, operation.getValue(), fieldName);
    }

    public static void checkOperationSupported(final OperationRangeType operation, final String fieldName) {
        check(RANGE_OPERATIONS, operation, operation.getValue(), fieldName);
    }

    private static <E extends Enum<E>> void check(final Set<E> supported, final E operation, final String value,
                                                  final String fieldName) {
        if (!supported.contains(operation)) {
            throw new BusinessException("Operace " + value + " není při vyhledávání AIP podporována pro pole '"
                    + fieldName + "', podporované operace: " + supported,
                    BaseCode.PROPERTY_IS_INVALID)
                            .set(BaseCode.PARAM_PROPERTY, fieldName)
                            .set("operation", value);
        }
    }

    private static String values(final Set<FilterType> filters) {
        return filters.stream().map(FilterType::getValue).sorted().toList().toString();
    }
}
