package cz.tacr.elza.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.controller.vo.AbstractFilter;
import cz.tacr.elza.controller.vo.AipField;
import cz.tacr.elza.controller.vo.AipFieldName;
import cz.tacr.elza.controller.vo.BoolValueFilter;
import cz.tacr.elza.controller.vo.DateValueFilter;
import cz.tacr.elza.controller.vo.EnumValueFilter;
import cz.tacr.elza.controller.vo.FieldType;
import cz.tacr.elza.controller.vo.FieldValueFilter;
import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.controller.vo.LogicalFilter;
import cz.tacr.elza.controller.vo.MultimatchContainsFilter;
import cz.tacr.elza.controller.vo.NumberValueFilter;
import cz.tacr.elza.controller.vo.OperationCompareType;
import cz.tacr.elza.controller.vo.OperationEqualityType;
import cz.tacr.elza.controller.vo.OperationLogicalType;
import cz.tacr.elza.controller.vo.OperationNumberType;
import cz.tacr.elza.controller.vo.OperationRangeType;
import cz.tacr.elza.controller.vo.OperationTextType;
import cz.tacr.elza.controller.vo.RefValueFilter;
import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.controller.vo.Sorting;
import cz.tacr.elza.controller.vo.SortingOrder;
import cz.tacr.elza.controller.vo.TextValueFilter;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.exception.BusinessException;

/**
 * The AIP filter against the database - verifies that valid SQL is generated for every column
 * type, that is, that each parameter is bound in the type of its column.
 */
public class AipRepositoryFilterTest extends AbstractTest {

    @Autowired
    private AipRepository aipRepository;

    // --- filter construction ------------------------------------------------------------

    private static AipField field(final AipFieldName name) {
        return new AipField(name, FieldType.AIP_FIELD);
    }

    private static SearchParams params(final AbstractFilter... filters) {
        SearchParams params = new SearchParams();
        params.setFilters(List.of(filters));
        params.setOffset(0);
        params.setSize(25);
        return params;
    }

    private static BoolValueFilter bool(final AipFieldName name, final Boolean value) {
        BoolValueFilter filter = new BoolValueFilter(field(name), OperationEqualityType.EQ, FilterType.BOOL_VALUE);
        filter.setValue(value);
        return filter;
    }

    private static RefValueFilter ref(final AipFieldName name, final Integer value) {
        RefValueFilter filter = new RefValueFilter(field(name), OperationEqualityType.EQ, FilterType.REF_VALUE);
        filter.setValue(value);
        return filter;
    }

    private static NumberValueFilter number(final AipFieldName name, final OperationNumberType operation,
                                            final Long value) {
        NumberValueFilter filter = new NumberValueFilter(field(name), operation, FilterType.NUMBER_VALUE);
        filter.setValue(value);
        return filter;
    }

    private static TextValueFilter text(final AipFieldName name, final OperationTextType operation,
                                        final String value) {
        TextValueFilter filter = new TextValueFilter(field(name), operation, FilterType.TEXT_VALUE);
        filter.setValue(value);
        return filter;
    }

    private static EnumValueFilter enumValue(final AipFieldName name, final String value) {
        EnumValueFilter filter = new EnumValueFilter(field(name), OperationEqualityType.EQ, FilterType.ENUM_VALUE);
        filter.setValue(value);
        return filter;
    }

    // --- the query the UI actually sends -------------------------------------------------

    /**
     * Exactly the filters the AIP screen in a fund (ArrAipPage) sends: an integer fund
     * reference and two boolean flags, all compared for equality.
     */
    @Test
    public void testArrAipPageInitialFilters() {
        FilteredResult<DaAip> result = aipRepository.findAipsByFilter(params(
                ref(AipFieldName.FUND, 1),
                bool(AipFieldName.METADATA_LOAD, Boolean.TRUE),
                bool(AipFieldName.METADATA_ERROR, Boolean.FALSE)));

        assertNotNull(result);
        assertNotNull(result.getList());
    }

    /**
     * Every column type has to work with an operation that makes sense for it.
     */
    @Test
    public void testEachValueTypeProducesValidSql() {
        assertNotNull(aipRepository.findAipsByFilter(
                params(text(AipFieldName.CODE, OperationTextType.CONTAINS, "AIP"))));
        assertNotNull(aipRepository.findAipsByFilter(
                params(text(AipFieldName.CODE, OperationTextType.EQ, "AIP-1"))));
        assertNotNull(aipRepository.findAipsByFilter(
                params(number(AipFieldName.AIP_ID, OperationNumberType.EQ, 1L))));
        assertNotNull(aipRepository.findAipsByFilter(
                params(number(AipFieldName.AIP_SIZE, OperationNumberType.EQ, 9999999999L))));
        assertNotNull(aipRepository.findAipsByFilter(params(ref(AipFieldName.ORIGINATOR, 1))));
        assertNotNull(aipRepository.findAipsByFilter(params(enumValue(AipFieldName.IMPORT_STATE, "IMPORT_OK"))));

        NumberValueFilter sizeRange = number(AipFieldName.AIP_SIZE, OperationNumberType.BETWEEN, null);
        sizeRange.setFrom(0L);
        sizeRange.setTo(9999999999L);
        assertNotNull(aipRepository.findAipsByFilter(params(sizeRange)));

        DateValueFilter dating = new DateValueFilter(field(AipFieldName.UNITDATE), OperationRangeType.BETWEEN,
                FilterType.DATE_VALUE);
        dating.setFrom(LocalDate.of(2020, 1, 1));
        dating.setTo(LocalDate.of(2020, 12, 31));
        assertNotNull(aipRepository.findAipsByFilter(params(dating)));

        assertNotNull(aipRepository.findAipsByFilter(
                params(text(AipFieldName.FUND_CODE, OperationTextType.IS_NULL, null))));
    }

    /**
     * Nesting comes from the shared contract; the AIP list implements it recursively.
     */
    @Test
    public void testLogicalNesting() {
        LogicalFilter either = new LogicalFilter(
                List.of(bool(AipFieldName.METADATA_LOAD, Boolean.TRUE),
                        bool(AipFieldName.METADATA_ERROR, Boolean.TRUE)),
                OperationLogicalType.OR, FilterType.LOGICAL);

        assertNotNull(aipRepository.findAipsByFilter(params(ref(AipFieldName.FUND, 1), either)));
    }

    @Test
    public void testSorting() {
        SearchParams params = params(bool(AipFieldName.METADATA_LOAD, Boolean.TRUE));
        params.setSort(List.of(new Sorting("aipSize").order(SortingOrder.DESC), new Sorting("code")));
        assertNotNull(aipRepository.findAipsByFilter(params));
    }

    @Test
    public void testSortingByUnknownFieldRejected() {
        SearchParams params = params(bool(AipFieldName.METADATA_LOAD, Boolean.TRUE));
        params.setSort(List.of(new Sorting("neexistuje")));
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(params));
    }

    // --- the declared capability subset ---------------------------------------------------

    /**
     * The AIP list implements part of the shared contract and refuses the rest by name,
     * rather than ignoring it or turning it into a query that means something else.
     */
    @Test
    public void testUnsupportedFilterTypesRejected() {
        MultimatchContainsFilter fulltext = new MultimatchContainsFilter("cokoliv", FilterType.CONTAINS);
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(params(fulltext)));

        FieldValueFilter untyped = new FieldValueFilter(field(AipFieldName.CODE), OperationCompareType.EQ,
                FilterType.FIELD_VALUE);
        untyped.setValue("AIP-1");
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(params(untyped)));
    }

    /**
     * A filter model applied to a field of another type is refused before any SQL is built.
     */
    @Test
    public void testFilterTypeMustMatchTheField() {
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(
                params(text(AipFieldName.METADATA_LOAD, OperationTextType.CONTAINS, "true"))));
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(
                params(text(AipFieldName.FUND, OperationTextType.NOT_CONTAINS, "1"))));
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(
                params(number(AipFieldName.FUND, OperationNumberType.EQ, 1L))));
    }

    @Test
    public void testUnsupportedOperationRejected() {
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(
                params(text(AipFieldName.CODE, OperationTextType.STARTWITH, "AIP"))));
        assertThrows(BusinessException.class, () -> aipRepository.findAipsByFilter(
                params(number(AipFieldName.AIP_SIZE, OperationNumberType.GT, 1L))));
    }

    @Test
    public void testInvalidValueRejected() {
        assertThrows(BusinessException.class,
                () -> aipRepository.findAipsByFilter(params(enumValue(AipFieldName.IMPORT_STATE, "NEEXISTUJE"))));
        assertThrows(BusinessException.class,
                () -> aipRepository.findAipsByFilter(params(bool(AipFieldName.METADATA_LOAD, null))));
    }

    /**
     * An empty search returns the whole list rather than failing.
     */
    @Test
    public void testEmptyFilter() {
        assertNotNull(aipRepository.findAipsByFilter(new SearchParams()));
    }
}
