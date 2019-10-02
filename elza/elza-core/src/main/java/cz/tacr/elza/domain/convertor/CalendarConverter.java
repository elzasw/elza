package cz.tacr.elza.domain.convertor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import cz.tacr.elza.core.data.CalendarType;

/**
 * Převody datumu na sekundy a naopak.
 *
 * @author Martin Šlapa
 * @since 06.04.2016
 */
public class CalendarConverter {

    public static final long UNIX_EPOCH_START_POS = 62135596800l;

    public static final long UNIX_EPOCH_START_NEG = 62167219200l;

    private static final ICalendarConverter gcc = new GregorianCalendarConverter();
    private static final ICalendarConverter jcc = new JulianCalendarConverter();

    /**
     * Převede datum na počet sekund (normalizovaně).
     *
     * @param type    typ kalendáře
     * @param seconds počet sekund
     * @return datum v daném kalendáři
     */
    public static LocalDateTime toDateTime(final CalendarType type, final long seconds) {
        ICalendarConverter converter = getCalendarConverter(type);
        LocalDateTime dateTimeNormalized = toDateTime(seconds);
        return converter.toCalendar(dateTimeNormalized);
    }

    /**
     * Převede sekundy na datum (normalizovaně).
     *
     * @param type     typ kalendáře
     * @param dateTime datum v daném kalendáři
     * @return počet sekund
     */
    public static long toSeconds(final CalendarType type, final LocalDateTime dateTime) {
        ICalendarConverter converter = getCalendarConverter(type);
        LocalDateTime dateTimeNormalized = converter.fromCalendar(dateTime);
        return toSeconds(dateTimeNormalized);
    }

    /**
     * Vybere konvertro podle kalendáře.
     *
     * @param type typ kalendáře
     * @return konvertor
     */
    private static ICalendarConverter getCalendarConverter(final CalendarType type) {
        ICalendarConverter converter;
        switch (type) {
        case GREGORIAN:
            converter = gcc;
            break;
        case JULIAN:
            converter = jcc;
            break;
        default:
            throw new IllegalStateException("Neimplementovaný typ kalendáře: " + type);
        }
        return converter;
    }

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
