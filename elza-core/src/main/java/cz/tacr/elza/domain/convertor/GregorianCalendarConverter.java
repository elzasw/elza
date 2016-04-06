package cz.tacr.elza.domain.convertor;

import java.time.LocalDateTime;

/**
 * Gregoriánský kalendář - je referenční/normalizační.
 *
 * @author Martin Šlapa
 * @since 06.04.2016
 */
public class GregorianCalendarConverter implements ICalendarConverter {

    @Override
    public LocalDateTime fromCalendar(final LocalDateTime gregorianDate) {
        return gregorianDate;
    }

    @Override
    public LocalDateTime toCalendar(final LocalDateTime normalizedDate) {
        return normalizedDate;
    }

}
