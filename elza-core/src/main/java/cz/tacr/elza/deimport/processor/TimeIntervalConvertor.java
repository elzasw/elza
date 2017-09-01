package cz.tacr.elza.deimport.processor;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.data.util.Pair;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.schema.v2.TimeInterval;

public class TimeIntervalConvertor {

    private final DatatypeFactory df;

    public TimeIntervalConvertor(DatatypeFactory df) {
        this.df = df;
    }

    /**
     * Converts <code>TimeInterval</code> to <code>Calendar</code> interval with proper format.
     * @return conversion result
     * @throws IllegalArgumentException When any value of <code>TimeInterval</code> is not valid.
     */
    public TimeIntervalConversionResult convert(TimeInterval interval) {
        Boundary from = new Boundary(df.newXMLGregorianCalendar(interval.getF()), true);
        Boundary to = new Boundary(df.newXMLGregorianCalendar(interval.getTo()), false);
        return convert(from, to, interval);
    }

    private TimeIntervalConversionResult convert(Boundary from, Boundary to, TimeInterval interval) {
        Pair<String, String> formatInterval = convertFormat(from, to, interval.getFmt());
        return new TimeIntervalConversionResult(
                from.getCalendar(),
                to.getCalendar(),
                formatInterval.getFirst(),
                formatInterval.getSecond(),
                (interval.isFe() == null ? false : interval.isFe()),
                (interval.isToe() == null ? false : interval.isToe()),
                convertCalendarType(interval.getCt()));
    }

    private static CalendarType convertCalendarType(String value) {
        CalendarType ct = null;
        if (value == null) {
            ct = CalendarType.GREGORIAN;
        } else if (value.length() == 1) {
            switch (value.charAt(0)) {
                case 'G':
                case 'g':
                    ct = CalendarType.GREGORIAN;
                    break;
                case 'J':
                case 'j':
                    ct = CalendarType.JULIAN;
                    break;
            }
        }
        if (ct == null) {
            throw new IllegalArgumentException("Time interval has invalid calendar type, code:" + value);
        }
        return ct;
    }

    /**
     * When format is null then new is created by significant fields otherwise current format is checked.
     * @return Begin and end Elza format pattern for interval.
     *
     * @see #checkFormat(Boundary, String)
     */
    private static Pair<String, String> convertFormat(Boundary from, Boundary to, String format) {
        String fFormat = null;
        String tFormat = null;
        if (format != null) {
            String[] parts = StringUtils.split(format, '-');
            switch (parts.length) {
                case 1:
                    fFormat = tFormat = parts[0];
                    break;
                case 2:
                    fFormat = parts[0];
                    tFormat = parts[1];
                    break;
                default:
                    throw new IllegalArgumentException("Invalid format of time interval:" + format);
            }
            checkFormat(from, fFormat);
            checkFormat(to, tFormat);
        } else {
            fFormat = from.getFormat();
            tFormat = to.getFormat();
        }
        return Pair.of(fFormat, tFormat);
    }

    /**
     * Checks if significant field does not have higher precision than format.
     * @param boundary interval boundary
     * @param format Elza format pattern
     */
    private static void checkFormat(Boundary boundary, String format) {
        if (!boundary.isValidFormat(format)) {
            throw new IllegalArgumentException("Format hides part of date or time, current format:" + format
                    + ", expected format:" + boundary.getFormat());
        }
    }

    public static class TimeIntervalConversionResult {

        private final static FastDateFormat ISO_8601_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

        private final Calendar from;

        private final Calendar to;

        private final String fromFormat;

        private final String toFormat;

        private final boolean fromEst;

        private final boolean toEst;

        private final CalendarType calendarType;

        public TimeIntervalConversionResult(Calendar from, Calendar to, String fromFormat, String toFormat,
                                            boolean fromEst, boolean toEst, CalendarType calendarType) {
            this.from = from;
            this.to = to;
            this.fromFormat = fromFormat;
            this.toFormat = toFormat;
            this.fromEst = fromEst;
            this.toEst = toEst;
            this.calendarType = calendarType;
        }

        /**
         * @return Begin of interval.
         */
        public Calendar getFrom() {
            return from;
        }

        /**
         * @return Formats begin of interval as ISO8601 string without timezone.
         */
        public String getFormattedFrom() {
            return ISO_8601_FORMAT.format(from);
        }

        /**
         * @return End of interval.
         */
        public Calendar getTo() {
            return to;
        }

        /**
         * @return Formats end of interval as ISO8601 string without timezone.
         */
        public String getFormattedTo() {
            return ISO_8601_FORMAT.format(to);
        }

        /**
         * @return Returns Elza format pattern for interval begin.
         */
        public String getFromFormat() {
            return fromFormat;
        }

        /**
         * @return Returns Elza format pattern for interval end.
         */
        public String getToFormat() {
            return toFormat;
        }

        /**
         * @return Returns Elza format pattern for interval.
         */
        public String getFormat() {
            if (fromFormat.equals(toFormat)) {
                return fromFormat;
            }
            return fromFormat + '-' + toFormat;
        }

        /**
         * @return If begin of interval is estimated.
         */
        public boolean isFromEst() {
            return fromEst;
        }

        /**
         * @return If end of interval is estimated.
         */
        public boolean isToEst() {
            return toEst;
        }

        public CalendarType getCalendarType() {
            return calendarType;
        }
    }

    private static class Boundary {

        public static final int[] FIELD_SEQUENCE = {
                Calendar.YEAR,
                Calendar.MONTH,
                Calendar.DAY_OF_MONTH,
                Calendar.HOUR_OF_DAY,
                Calendar.MINUTE,
                Calendar.SECOND,
                Calendar.MILLISECOND
        };

        private final Calendar calendar;

        private final int sigField;

        public Boundary(XMLGregorianCalendar xMLGregorianCalendar, boolean begin) {
            Pair<Calendar, Integer> calendarAndSigField = createCalendarAndFindSigField(xMLGregorianCalendar, begin);
            this.calendar = calendarAndSigField.getFirst();
            this.sigField = calendarAndSigField.getSecond();
        }

        public Calendar getCalendar() {
            return calendar;
        }

        /**
         * @param format Elza format pattern
         * @return True if significant field of boundary does not have higher precision than format.
         */
        public boolean isValidFormat(String format) {
            if (StringUtils.isEmpty(format)) {
                return false;
            }
            switch (format) {
                case "C":
                case "Y":
                    return sigField <= Calendar.YEAR;
                case "YM":
                    return sigField <= Calendar.MONTH;
                case "D":
                    return sigField <= Calendar.DAY_OF_MONTH;
                case "DT":
                    return sigField <= Calendar.MILLISECOND;
                default:
                    return false;
            }
        }

        /**
         * Returns Elza format pattern by significant field.
         */
        public String getFormat() {
            switch (sigField) {
                case Calendar.YEAR:
                    return "Y";
                case Calendar.MONTH:
                    return "YM";
                case Calendar.DAY_OF_MONTH:
                    return "D";
                case Calendar.HOUR_OF_DAY:
                case Calendar.MINUTE:
                case Calendar.SECOND:
                case Calendar.MILLISECOND:
                    return "DT";
                default:
                    throw new IllegalStateException();
            }
        }

        /**
         * Searches for defined field with highest precision (according to {@link #FIELD_SEQUENCE}).
         * Undefined fields are set to default value. Calendar is build during the process.
         */
        private static Pair<Calendar, Integer> createCalendarAndFindSigField(XMLGregorianCalendar xMLGregorianCalendar, boolean begin) {
            int[] dateFields = new int[FIELD_SEQUENCE.length];
            dateFields[0] = xMLGregorianCalendar.getYear();
            dateFields[1] = xMLGregorianCalendar.getMonth() - 1; // Calendar.MONTH from 0
            dateFields[2] = xMLGregorianCalendar.getDay();
            dateFields[3] = xMLGregorianCalendar.getHour();
            dateFields[4] = xMLGregorianCalendar.getMinute();
            dateFields[5] = xMLGregorianCalendar.getSecond();
            dateFields[6] = xMLGregorianCalendar.getMillisecond();

            Calendar calendar = GregorianCalendar.getInstance(); //TODO: time zone and locale
            int sigField = -1;

            for (int i = 0; i < FIELD_SEQUENCE.length; i++) {
                int field = FIELD_SEQUENCE[i];
                int srcValue = dateFields[i];

                int fieldValue = begin ? calendar.getActualMinimum(field) : calendar.getActualMaximum(field);
                if (srcValue != DatatypeConstants.FIELD_UNDEFINED && srcValue != fieldValue) {
                    fieldValue = srcValue;
                    sigField = field;
                }
                calendar.set(field, fieldValue);
            }
            if (sigField < 0) {
                throw new IllegalArgumentException("Invalid interval boundary:" + xMLGregorianCalendar);
            }
            return Pair.of(calendar, sigField);
        }
    }
}
