package cz.tacr.elza.domain.convertor;

import java.time.LocalDateTime;

/**
 * Rozhraní pro převod kalendáře na normalizační a zpět.
 *
 * @author Martin Šlapa
 * @since 06.04.2016
 */
public interface ICalendarConverter {


    /**
     * Převedení juliánského datumu na normalizačního.
     *
     * @param julianDate juliánský datum
     * @return normalizačního datum
     */
    LocalDateTime fromCalendar(final LocalDateTime julianDate);

    /**
     * Převedení normalizačního datumu na juliánského.
     *
     * @param normalizedDate normalizačního datum
     * @return juliánský datum
     */
    LocalDateTime toCalendar(final LocalDateTime normalizedDate);

}
