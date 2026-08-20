package cz.tacr.elza.repository.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.controller.vo.FilterType;
import cz.tacr.elza.domain.DaSyncQueueItem.QueueItemState;
import cz.tacr.elza.exception.BusinessException;

/**
 * Rules tying a column of the AIP list to the filter model that may carry its value.
 */
public class AipFilterValueTypeTest {

    @Test
    public void acceptsMatchingJavaTypes() {
        assertTrue(AipFilterValueType.TEXT.accepts(String.class));
        assertTrue(AipFilterValueType.NUMBER.accepts(Integer.class));
        assertTrue(AipFilterValueType.NUMBER.accepts(Long.class));
        assertTrue(AipFilterValueType.BOOLEAN.accepts(Boolean.class));
        assertTrue(AipFilterValueType.DATE.accepts(LocalDateTime.class));
        assertTrue(AipFilterValueType.DATE.accepts(LocalDate.class));
        assertTrue(AipFilterValueType.ENUM.accepts(QueueItemState.class));
        // a reference is carried as the id of the entity
        assertTrue(AipFilterValueType.REF.accepts(Integer.class));
    }

    @Test
    public void rejectsMismatchedJavaTypes() {
        assertFalse(AipFilterValueType.TEXT.accepts(Integer.class));
        assertFalse(AipFilterValueType.BOOLEAN.accepts(String.class));
        assertFalse(AipFilterValueType.NUMBER.accepts(String.class));
        assertFalse(AipFilterValueType.DATE.accepts(String.class));
        assertFalse(AipFilterValueType.ENUM.accepts(String.class));
    }

    @Test
    public void eachValueTypeHasItsOwnFilterModel() {
        assertEquals(FilterType.TEXT_VALUE, AipFilterValueType.TEXT.getFilterType());
        assertEquals(FilterType.NUMBER_VALUE, AipFilterValueType.NUMBER.getFilterType());
        assertEquals(FilterType.BOOL_VALUE, AipFilterValueType.BOOLEAN.getFilterType());
        assertEquals(FilterType.DATE_VALUE, AipFilterValueType.DATE.getFilterType());
        assertEquals(FilterType.ENUM_VALUE, AipFilterValueType.ENUM.getFilterType());
        assertEquals(FilterType.REF_VALUE, AipFilterValueType.REF.getFilterType());
    }

    /**
     * A filter model applied to a field of another type is refused, so a comparison that
     * cannot be expressed in SQL never reaches the query builder.
     */
    @Test
    public void checkFilterTypeRejectsTheWrongModel() {
        assertThrows(BusinessException.class,
                () -> AipFilterValueType.BOOLEAN.checkFilterType(FilterType.TEXT_VALUE, "metadataLoad"));
        assertThrows(BusinessException.class,
                () -> AipFilterValueType.REF.checkFilterType(FilterType.NUMBER_VALUE, "fund"));
        assertThrows(BusinessException.class,
                () -> AipFilterValueType.TEXT.checkFilterType(FilterType.ENUM_VALUE, "code"));
        // the matching model passes
        AipFilterValueType.BOOLEAN.checkFilterType(FilterType.BOOL_VALUE, "metadataLoad");
        AipFilterValueType.REF.checkFilterType(FilterType.REF_VALUE, "fund");
    }

    @Test
    public void parseEnumResolvesConstantsAndRejectsUnknownOnes() {
        assertEquals(QueueItemState.IMPORT_OK,
                AipFilterValueType.parseEnum(QueueItemState.class, "IMPORT_OK", "importState"));
        assertThrows(BusinessException.class,
                () -> AipFilterValueType.parseEnum(QueueItemState.class, "NEEXISTUJE", "importState"));
        assertThrows(BusinessException.class,
                () -> AipFilterValueType.parseEnum(QueueItemState.class, null, "importState"));
    }

    /**
     * A range covers whole days, so an AIP dated on the boundary day is part of the result.
     */
    @Test
    public void dateRangeCoversWholeBoundaryDays() {
        assertEquals(LocalDateTime.of(2020, 1, 1, 0, 0, 0),
                AipFilterValueType.rangeStart(LocalDate.of(2020, 1, 1), "unitdate"));

        LocalDateTime end = AipFilterValueType.rangeEnd(LocalDate.of(2020, 12, 31), "unitdate");
        assertEquals(LocalDate.of(2020, 12, 31), end.toLocalDate());
        assertEquals(23, end.getHour());
        assertEquals(59, end.getMinute());
    }

    @Test
    public void dateBoundAdaptsToTheColumnType() {
        LocalDateTime bound = LocalDateTime.of(2020, 5, 4, 10, 15);
        assertEquals(bound, AipFilterValueType.toDateBound(LocalDateTime.class, bound));
        assertEquals(LocalDate.of(2020, 5, 4), AipFilterValueType.toDateBound(LocalDate.class, bound));
    }

    @Test
    public void missingValueIsRejected() {
        assertThrows(BusinessException.class, () -> AipFilterValueType.requireValue(null, "code"));
        assertThrows(BusinessException.class, () -> AipFilterValueType.requireValue("", "code"));
        assertEquals("ABC", AipFilterValueType.requireValue("ABC", "code"));
    }
}
