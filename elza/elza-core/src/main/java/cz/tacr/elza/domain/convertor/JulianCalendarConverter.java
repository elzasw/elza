package cz.tacr.elza.domain.convertor;

import java.time.LocalDateTime;

/**
 * Juliánský kalendář.
 *
 * @since 06.04.2016
 */
public class JulianCalendarConverter implements ICalendarConverter {

    @Override
    public LocalDateTime fromCalendar(final LocalDateTime julianDate) {
        long day = fromJulianDayDiff(julianDate);
        LocalDateTime gregorianDate = julianDate.plusDays(day);
        return gregorianDate;
    }

    @Override
    public LocalDateTime toCalendar(final LocalDateTime normalizedDate) {
        long day = toJulianDayDiff(normalizedDate);
        LocalDateTime julianDate = normalizedDate.minusDays(day);
        return julianDate;
    }

    /**
     * Vypočtení rozdílu dní mezi juliánským a gregoriánským kalendářem při J->G
     *
     * @param date juliánský datum
     * @return počet dní
     */
    private long fromJulianDayDiff(final LocalDateTime date) {
        LocalDateTime refDate = LocalDateTime.of(date.getYear(), 3, 12, 0, 0);
        int yearX = date.getYear() / 100 * 100;
        int y100 = (yearX - 3) / 100;
        int y400 = yearX / 400;
        long tmpDayDiff = y100 - y400 - 1L;

        LocalDateTime dateTime = refDate.minusDays(tmpDayDiff - 1);
        if (date.isBefore(dateTime)) {
            tmpDayDiff -= 1;
        }

        return tmpDayDiff;
    }

    /**
     * Vypočtení rozdílu dní mezi juliánským a gregoriánským kalendářem při G->J
     *
     * @param date gregoriánský datum
     * @return počet dní
     */
    private long toJulianDayDiff(final LocalDateTime date) {
        LocalDateTime refDate = LocalDateTime.of(date.getYear(), 3, 1, 0, 0);
        int yearX = date.getYear() / 100 * 100;
        int y100 = (yearX - 3) / 100;
        int y400 = yearX / 400;
        long tmpDayDiff = y100 - y400 - 1L;

        LocalDateTime dateTime = refDate.plusDays(tmpDayDiff - 1);
        if (date.isBefore(dateTime)) {
            tmpDayDiff -= 1;
        }

        return tmpDayDiff;
    }

}
