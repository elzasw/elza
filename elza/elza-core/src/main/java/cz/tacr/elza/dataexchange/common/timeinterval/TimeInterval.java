package cz.tacr.elza.dataexchange.common.timeinterval;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.dataexchange.common.CalendarTypeConvertor;

/**
 * Time interval representation with Elza format. Bridge class between API and internal services.
 */
public class TimeInterval {

    private final Boundary lBoundary;

    private final Boundary uBoundary;

    private final CalendarType calendarType;

    private final boolean lbEst;

    private final boolean ubEst;

    /**
     * True if lower and upper boundary are equal.
     */
    private final boolean emptyInterval;

    /**
     * True if format e.g. "YM" (without dash).
     */
    private Boolean singleFormat;

    private BoundaryFormat lbFormat;

    private BoundaryFormat ubFormat;

    private TimeInterval(Boundary lBoundary, Boundary uBoundary, CalendarType calendarType, boolean lbEst, boolean ubEst) {
        this.lBoundary = lBoundary;
        this.uBoundary = uBoundary;
        this.calendarType = calendarType;
        this.lbEst = lbEst;
        this.ubEst = ubEst;

        int compareResult = lBoundary.compareTo(uBoundary);
        if (compareResult > 0) {
            throw new IllegalArgumentException(
                    "Lower bound is after the time of upper bound, from:" + getFormattedFrom() + ", to:" + getFormattedTo());
        }
        this.emptyInterval = compareResult == 0;
    }

    /**
     * @return Begin of interval.
     */
    public LocalDateTime getFrom() {
        return lBoundary.getLocalDateTime();
    }

    /**
     * @return Begin of interval as ISO-8601 without an offset.
     */
    public String getFormattedFrom() {
        return formatISO86O1(getFrom());
    }

    /**
     * @return End of interval.
     */
    public LocalDateTime getTo() {
        return uBoundary.getLocalDateTime();
    }

    /**
     * @return End of interval as ISO-8601 without an offset.
     */
    public String getFormattedTo() {
        return formatISO86O1(getTo());
    }

    /**
     * @return Interval format.
     */
    public String getFormat() {
        if (singleFormat == null) {
            singleFormat = emptyInterval;
            lbFormat = lBoundary.getDefinedFormat();
            ubFormat = uBoundary.getDefinedFormat();
        }
        if (singleFormat) {
            return lbFormat.getValue();
        }
        return lbFormat.getValue() + '-' + ubFormat.getValue();
    }

    public CalendarType getCalendarType() {
        return calendarType;
    }

    /**
     * @return If begin of interval is estimated.
     */
    public boolean isFromEst() {
        return lbEst;
    }

    /**
     * @return If end of interval is estimated.
     */
    public boolean isToEst() {
        return ubEst;
    }

    /**
     * Apply format on time interval.
     *
     * @throws IllegalArgumentException When format is not acceptable.
     */
    public void applyFormat(String format) {
        String[] parts = StringUtils.split(format, '-');
        if (parts == null) {
            return;
        }
        if (parts.length <= 0 || parts.length > 2) {
            throw new IllegalArgumentException("Invalid format of time interval, value:" + format);
        }
        BoundaryFormat lbf = BoundaryFormat.fromValue(parts[0]);
        BoundaryFormat ubf = null;
        if (parts.length == 1) {
            if (!isSingleFormatValid(lbf)) {
                throw new IllegalArgumentException("Single format is not acceptable for time interval, value:"
                        + format +
                        ", lower boundary: " + lBoundary.getLocalDateTime() +
                        ", upper boundary: " + uBoundary.getLocalDateTime());
            }
            singleFormat = true;
        } else {
            if (!lbf.isValid(lBoundary)) {
                throw new IllegalArgumentException("Lower boundary is incorrect:" + lbf.getValue() + ", date: "
                        + lBoundary.getLocalDateTime());
            }
            ubf = BoundaryFormat.fromValue(parts[1]);
            if (!ubf.isValid(uBoundary)) {
                throw new IllegalArgumentException("Upper boundary is incorrect:" + ubf.getValue() + ", date: "
                        + uBoundary.getLocalDateTime());
            }
            singleFormat = false;
        }
        this.lbFormat = lbf;
        this.ubFormat = ubf;
    }

    private boolean isSingleFormatValid(BoundaryFormat format) {
        if (!format.isValid(lBoundary) || !format.isValid(uBoundary)) {
            return false;
        }
        if (emptyInterval) {
            return true; // e.g. "Y" for 1990 - 1990
        }
        if (lBoundary.compareTo(uBoundary, format.getCalendarFieldPrecision()) == 0) {
            return true; // e.g. "Y" for 1990-1-1 - 1990-12-31
        }
        if (format == BoundaryFormat.CENTURY) { // e.g. "C" for 1901-2000
            int begin = lBoundary.getCalendarValue(Calendar.YEAR);
            if (begin + 99 == uBoundary.getCalendarValue(Calendar.YEAR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats dateTime as ISO-8601 with pattern "YYYY-MM-DDThh:mm:ss". Occurrence of milliseconds
     * is removed.
     */
    public static String formatISO86O1(LocalDateTime dateTime) {
        StringBuilder sb = new StringBuilder(19);
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.formatTo(dateTime, sb);
        switch (sb.length()) {
            case 16: // add zero seconds
                sb.append(":00");
                break;
            case 23: // remove milliseconds
                sb.setLength(19);
                break;
            case 19: // praise the sun
                break;
            default:
                throw new DateTimeException("Invalid ISO-86O1 format length:" + sb.length());
        }
        return sb.toString();
    }

    public static TimeInterval create(cz.tacr.elza.schema.v2.TimeInterval source) {
        Boundary lBoundary = Boundary.create(source.getF(), true);
        Boundary uBoundary = Boundary.create(source.getTo(), false);

        CalendarType ct = CalendarTypeConvertor.convert(source.getCt());

        boolean lbEst = source.isFe() == null ? false : source.isFe();
        boolean ubEst = source.isToe() == null ? false : source.isToe();

        TimeInterval ti = new TimeInterval(lBoundary, uBoundary, ct, lbEst, ubEst);
        ti.applyFormat(source.getFmt());
        return ti;
    }
}
