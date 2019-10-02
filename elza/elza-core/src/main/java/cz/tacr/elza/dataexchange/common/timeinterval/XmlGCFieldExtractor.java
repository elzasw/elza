package cz.tacr.elza.dataexchange.common.timeinterval;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Defines how extract {@link XMLGregorianCalendar} date as {@link Calendar} fields.
 */
public class XmlGCFieldExtractor {

    /**
     * Shortcut for {@link DatatypeConstants#FIELD_UNDEFINED}.
     */
    private static final int FIELD_UNDEFINED = DatatypeConstants.FIELD_UNDEFINED;

    public static final XmlGCField ERA_FIELD = new XmlGCField(Calendar.ERA) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getYear() < 0 ? GregorianCalendar.BC : GregorianCalendar.AD;
        }
    };

    public static final XmlGCField YEAR_FIELD = new XmlGCField(Calendar.YEAR) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            if (cal.getEon() != null) {
                throw new IllegalArgumentException("EON years cannot be converted");
            }
            return Math.abs(cal.getYear());
        }
    };

    public static final XmlGCField MONTH_FIELD = new XmlGCField(Calendar.MONTH) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getMonth() != FIELD_UNDEFINED ? cal.getMonth() - 1 : FIELD_UNDEFINED;
        }
    };

    public static final XmlGCField DAY_OF_MONTH_FIELD = new XmlGCField(Calendar.DAY_OF_MONTH) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getDay();
        }
    };

    public static final XmlGCField HOUR_FIELD = new XmlGCField(Calendar.HOUR_OF_DAY) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getHour();
        }
    };

    public static final XmlGCField MINUTE_FIELD = new XmlGCField(Calendar.MINUTE) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getMinute();
        }
    };

    public static final XmlGCField SECOND_FIELD = new XmlGCField(Calendar.SECOND) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            return cal.getSecond();
        }
    };

    public static final XmlGCField MILLISECOND_FIELD = new XmlGCField(Calendar.MILLISECOND) {
        @Override
        public int getValue(XMLGregorianCalendar cal) {
            if (cal.getFractionalSecond() != null) {
                throw new IllegalArgumentException("Fractional seconds cannot be converted");
            }
            return FIELD_UNDEFINED;
        }
    };

    /**
     * Returns {@link Calendar} fields representation ordered from the lowest precision.
     */
    public static XmlGCField[] getFieldsLowerPrecisionFirst() {
        return new XmlGCField[] {
                ERA_FIELD,
                YEAR_FIELD,
                MONTH_FIELD,
                DAY_OF_MONTH_FIELD,
                HOUR_FIELD,
                MINUTE_FIELD,
                SECOND_FIELD,
                MILLISECOND_FIELD
        };
    }

    /**
     * Represents {@link XMLGregorianCalendar} value as {@link Calendar} field.
     */
    public abstract static class XmlGCField {

        private final int field;

        private XmlGCField(int field) {
            this.field = field;
        }

        public int getField() {
            return field;
        }

        public abstract int getValue(XMLGregorianCalendar cal);
    }
}
