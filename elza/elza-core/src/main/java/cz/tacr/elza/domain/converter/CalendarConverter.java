package cz.tacr.elza.domain.converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
  
/**
 * Převody datumu na sekundy a naopak.
 *
 * @since 06.04.2016
 */
public class CalendarConverter {

    public static final long UNIX_EPOCH_START_POS = 62135596800l;

    /**
     * Převede datum na počet sekund od roku 1.
     * <p>
     * Datum 1. 1. 1 00:00:01 odpovídá 1
     * Datum 1. 1. 1 00:00:00 odpovídá 0
     * (astronomická datace, ISO-8601) Datum 31. 12. 0 23:59:59 odpovídá -1
     * (chronologická datace) Datum 31. 12. -1 23:59:59 odpovídá -1
     *
     * @param dateTime normalizovaný datum
     * @return počet sekund
     */
    public static long toSeconds(final LocalDateTime dateTime) {
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return instant.getEpochSecond() + UNIX_EPOCH_START_POS;
    }

    /**
     * Převede sekundy na datum.
     *
     * @param seconds počet sekund
     * @return normalizovaný datum
     */
    public static LocalDateTime toDateTime(final long seconds) {
        long sec = seconds - UNIX_EPOCH_START_POS;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneOffset.UTC);
    }

}
