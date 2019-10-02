package cz.tacr.elza.dataexchange.common.timeinterval;

import java.util.Calendar;

import org.apache.commons.lang3.ArrayUtils;

enum BoundaryFormat {
    CENTURY("C", Calendar.YEAR) {
        @Override
        public int[] getCalendarFields() {
            return ArrayUtils.EMPTY_INT_ARRAY;
        }

        @Override
        public boolean isValid(Boundary boundary) {
            if (!super.isValid(boundary)) {
                return false;
            }
            int year = boundary.getCalendarValue(Calendar.YEAR);
            if (boundary.isLowerBoundary()) {
                year--; // 1901 - 2000
            }
            return year % 100 == 0;
        }
    },
    YEAR("Y", Calendar.YEAR),
    YEAR_MONTH("YM", Calendar.MONTH),
    DATE("D", Calendar.DATE),
    DATE_TIME("DT", Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND);

    private final String value;

    private final int[] calendarFields;

    private BoundaryFormat(String value, int... calendarFields) {
        this.value = value;
        this.calendarFields = calendarFields;
    }

    public String getValue() {
        return value;
    }

    public int[] getCalendarFields() {
        return calendarFields;
    }

    public int getCalendarFieldPrecision() {
        return calendarFields[calendarFields.length - 1];
    }

    /**
     * Returns true if this format is valid against the specified boundary.
     */
    public boolean isValid(Boundary boundary) {
        if (boundary.getLastDefinedField() < calendarFields[0]) {
            return false; // format after defined fields
        }
        if (boundary.getLastNonDefaultField() > getCalendarFieldPrecision()) {
            return false; // format hides non-default fields
        }
        return true;
    }

    public static BoundaryFormat fromValue(String value) {
        for (BoundaryFormat bf : BoundaryFormat.values()) {
            if (value.equals(bf.value)) {
                return bf;
            }
        }
        throw new IllegalArgumentException("Invalid format of time interval boundary, value:" + value);
    }
}