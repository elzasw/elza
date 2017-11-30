package cz.tacr.elza.dataexchange.common.timeinterval;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.utils.XmlUtils;

class Boundary implements Comparable<Boundary> {

    /**
     * Definition of calendar fields values and order for XML gregorian calendar.
     */
    private static final XmlCalendarField[] XML_CALENDAR_FIELDS = {
        new XmlCalendarField(Calendar.ERA) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getYear() < 0 ? GregorianCalendar.BC : GregorianCalendar.AD;
            }
        }, new XmlCalendarField(Calendar.YEAR) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                if (cal.getEon() != null) {
                    throw new IllegalArgumentException("Cannot create boundary with EON years");
                }
                return Math.abs(cal.getYear());
            }
        }, new XmlCalendarField(Calendar.MONTH) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getMonth() != FIELD_UNDEFINED ? cal.getMonth() - 1 : FIELD_UNDEFINED;
            }
        }, new XmlCalendarField(Calendar.DAY_OF_MONTH) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getDay();
            }
        }, new XmlCalendarField(Calendar.HOUR_OF_DAY) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getHour();
            }
        }, new XmlCalendarField(Calendar.MINUTE) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getMinute();
            }
        }, new XmlCalendarField(Calendar.SECOND) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                return cal.getSecond();
            }
        }, new XmlCalendarField(Calendar.MILLISECOND) {
            @Override
            public int getValue(XMLGregorianCalendar cal) {
                if (cal.getFractionalSecond() != null) {
                    throw new IllegalArgumentException("Cannot create boundary with precision of fractional seconds");
                }
                return FIELD_UNDEFINED;
            }
        }
    };

    /**
     * Represent all dates as gregorian, see {@link GregorianCalendar#setGregorianChange(Date)}.
     */
    private static final Date GREGORIAN_CHANGE_MIN = new Date(Long.MIN_VALUE);

    /**
     * Shortcut for {@link DatatypeConstants#FIELD_UNDEFINED}.
     */
    private static final int FIELD_UNDEFINED = DatatypeConstants.FIELD_UNDEFINED;

    private final Calendar calendar;

    private final int lastNonDefaultField;

    private final int lastDefinedField;

    private final boolean lowerBoundary;

    private Boundary(Calendar calendar, int lastNonDefaultField, int lastDefinedField, boolean lowerBoundary) {
        this.calendar = calendar;
        this.lastNonDefaultField = lastNonDefaultField;
        this.lastDefinedField = lastDefinedField;
        this.lowerBoundary = lowerBoundary;
    }

    public int getLastNonDefaultField() {
        return lastNonDefaultField;
    }

    public int getLastDefinedField() {
        return lastDefinedField;
    }

    public boolean isLowerBoundary() {
        return lowerBoundary;
    }

    public int getCalendarValue(int field) {
        return calendar.get(field);
    }

    /**
     * Creates format by defined calendar fields of boundary format.
     */
    public BoundaryFormat getDefinedFormat() {
        for (BoundaryFormat bf : BoundaryFormat.values()) {
            for (int cf : bf.getCalendarFields()) {
                if (cf == lastDefinedField) {
                    return bf;
                }
            }
        }
        throw new IllegalStateException("Uknown calendar field:" + lastDefinedField);
    }

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneOffset.UTC);
    }

    /**
     * Compares this boundary with the specified boundary. Returns a negative integer, zero, or a
     * positive integer as this object is less than, equal to, or greater than the specified
     * boundary.<br>
     * <br>
     * Defined fields are compared in sequence of boundary format calendar fields. Precision of
     * comparison is limited by last field.
     */
    public int compareTo(Boundary o, int lastField) {
        if (this == o) {
            return 0;
        }
        int definedLimit = Math.min(lastDefinedField, o.lastDefinedField);
        for (BoundaryFormat bf : BoundaryFormat.values()) {
            for (int cf : bf.getCalendarFields()) {
                if (cf > lastField) {
                    return 0;
                }
                if (cf > definedLimit) {
                    // defined limit reached -> compare last defined field
                    if (lastDefinedField != o.lastDefinedField) {
                        return lastDefinedField > o.lastDefinedField ? 1 : -1;
                    }
                    return 0;
                }
                int tv = calendar.get(cf);
                int ov = o.calendar.get(cf);
                if (tv != ov) {
                    return tv > ov ? 1 : -1;
                }
            }
        }
        return 0; // all format fields and equal
    }

    /**
     * Compares this boundary with the specified boundary. Returns a negative integer, zero, or a
     * positive integer as this object is less than, equal to, or greater than the specified
     * boundary.<br>
     * <br>
     * Defined fields are compared in sequence of boundary format calendar fields.
     */
    @Override
    public int compareTo(Boundary o) {
        return compareTo(o, Integer.MAX_VALUE);
    }

    /**
     * Creates interval boundary from XML dateTime.
     */
    public static Boundary create(String xmlDateTime, boolean lowerBoundary) {
        XMLGregorianCalendar xmlgc = XmlUtils.DATATYPE_FACTORY.newXMLGregorianCalendar(xmlDateTime);
        Boundary boundary = create(xmlgc, lowerBoundary);
        return boundary;
    }

    /**
     * Creates interval boundary from XML gregorian calendar.
     */
    public static Boundary create(XMLGregorianCalendar xmlgc, boolean lowerBoundary) {
        if (xmlgc.getTimezone() != FIELD_UNDEFINED) {
            throw new IllegalArgumentException("Cannot create boundary with timezone");
        }

        int lastNonDefaultField = FIELD_UNDEFINED;
        int lastDefinedField = FIELD_UNDEFINED;

        GregorianCalendar gc = new GregorianCalendar(XmlUtils.UTC_TIMEZONE);
        gc.setGregorianChange(GREGORIAN_CHANGE_MIN);

        for (XmlCalendarField cf : XML_CALENDAR_FIELDS) {
            int value = cf.getValue(xmlgc);
            int defValue = lowerBoundary ? gc.getActualMinimum(cf.field) : gc.getActualMaximum(cf.field);

            if (value == FIELD_UNDEFINED) {
                value = defValue;
            } else {
                lastDefinedField = cf.field;
                if (defValue != value) {
                    lastNonDefaultField = cf.field;
                }
            }
            gc.set(cf.field, value);
        }

        Validate.isTrue(lastNonDefaultField <= lastDefinedField);
        Validate.isTrue(lastNonDefaultField != FIELD_UNDEFINED);

        Boundary boundary = new Boundary(gc, lastNonDefaultField, lastDefinedField, lowerBoundary);
        return boundary;
    }

    private abstract static class XmlCalendarField {

        public final int field;

        public XmlCalendarField(int field) {
            this.field = field;
        }

        public abstract int getValue(XMLGregorianCalendar cal);
    }
}
