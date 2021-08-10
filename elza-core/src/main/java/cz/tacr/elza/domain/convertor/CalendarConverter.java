package cz.tacr.elza.domain.convertor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
  
/**
 * Převody datumu na sekundy a naopak.
 *
 * @author Martin Šlapa
 * @since 06.04.2016
 */
public class CalendarConverter {

    public static final long UNIX_EPOCH_START_POS = 62135596800l;

    public static final long UNIX_EPOCH_START_NEG = 62167219200l;

    /**
     * Převede datum na počet sekund od roku 1.
     * <p>
     * Datum 1. 1. 1 00:00:01 odpovídá 1
     * Datum 1. 1. 1 00:00:00 odpovídá 0
     * Datum 31. 12. -1 23:59:59 odpovídá -1
     *
     * @param dateTime normalizovaný datum
     * @return počet sekund
     */
    public static long toSeconds(final LocalDateTime dateTime) {
        if (dateTime.getYear() == 0) {
            throw new IllegalArgumentException("Year 0 is not valid");
        }
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        long sec;
        if (dateTime.getYear() < 0) {
            sec = instant.getEpochSecond() + UNIX_EPOCH_START_NEG;
        } else {
            sec = instant.getEpochSecond() + UNIX_EPOCH_START_POS;
        }
        return sec;
    }

    /**
     * Převede sekundy na datum.
     *
     * @param seconds počet sekund
     * @return normalizovaný datum
     */
    public static LocalDateTime toDateTime(final long seconds) {
        long sec;
        if (seconds < 0) {
            sec = seconds - UNIX_EPOCH_START_NEG;
        } else {
            sec = seconds - UNIX_EPOCH_START_POS;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneOffset.UTC);
    }

}
