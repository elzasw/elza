package cz.tacr.elza.print.item.convertors;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.CENTURY;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DATE;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DATE_TIME;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.ESTIMATED_TEMPLATE;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.YEAR;
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.YEAR_MONTH;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cz.tacr.elza.api.IUnitdate;

/**
 * Konvertor pro sprváné zobrazování UnitDate pro tiskový výstup.
 * 
 */
public class UnitDatePrintConvertor {

    /**
     * Formátovač pro datum
     */
    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern("d. M. u");
    
    /**
     * Formátovač pro datum s časem
     */
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern("d. M. u H:mm:ss");

    /**
     * Názvy měsíců
     */
    public static final String[] MONTHS = {"leden", "únor", "březen", "duben", "květen", "červen", 
            "červenec", "srpen", "září", "říjen", "listopad", "prosinec"};

    /**
     * Šablona pro století
     */
    public static final String CENTURY_TEMPLATE = "%d. století";

    /**
     * Formát rok-rok
     */
    public static final String FORMAT_YEAR_YEAR = "Y-Y";

    /**
     * Šablona pro meziroční interval
     */
    public static final String INTERVAL_YEAR_TEMPLATE = "%s–%s";

    /**
     * Šablona pro interval
     */
    public static final String INTERVAL_TEMPLATE = "%s – %s";

    enum DateType {
        // solo date
        SOLITARY,
        // lower part
        LOWER_BOUNDARY,
        // upper part
        UPPER_BOUNDARY
    };

    /**
     * Provede konverzi formátu pro tiskový výstup.
     * 
     * @param unitdate
     * @return String
     */
    public static String convertToPrint(final IUnitdate unitdate) {

        String format = unitdate.getFormat();

        if (isInterval(format)) {
            return convertInterval(format, unitdate);
        }
        return convertDate(format, unitdate.getValueFrom(), unitdate.getValueFromEstimated(), DateType.SOLITARY);
    }

    /**
     * Detekce, zda-li se jedná o interval
     * 
     * @param format
     * @return true, pokud ve formátu existuje "-"
     */
    private static boolean isInterval(String format) {
        return format.contains(DEFAULT_INTERVAL_DELIMITER);
    }

    /**
     * Konverze intervalu.

     * @param format
     * @param unitdate
     * @return String
     */
    private static String convertInterval(final String format, final IUnitdate unitdate) {

        String[] fmt = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (fmt.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        String template = format.equals(FORMAT_YEAR_YEAR)? INTERVAL_YEAR_TEMPLATE : INTERVAL_TEMPLATE;
        String dateFrom = convertDate(fmt[0], unitdate.getValueFrom(), unitdate.getValueFromEstimated(),
                                      DateType.LOWER_BOUNDARY);
        String dateTo = convertDate(fmt[1], unitdate.getValueTo(), unitdate.getValueToEstimated(),
                                    DateType.UPPER_BOUNDARY);

        return String.format(template, dateFrom, dateTo);
    }

    /**
     * Konverze datum.
     * 
     * @param format
     * @param value
     * @param estimated
     * @return String
     */
    public static String convertDate(final String format, final String value, 
                                     final boolean estimated, final DateType dateType) {

        String result;

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Chyba při analýze datum: " + value, e);
        }
        switch (format) {
        case CENTURY:
        {
            int iDateShift = (dateType == DateType.UPPER_BOUNDARY) ? 0 : 1;
            result = String.format(CENTURY_TEMPLATE, date.getYear() / 100 + iDateShift);
        }
            break;
        case YEAR:
            result = String.valueOf(date.getYear());
            break;
        case YEAR_MONTH:
            result = String.format("%s %d", MONTHS[date.getMonthValue() - 1], date.getYear());
            break;
        case DATE:
            result = FORMATTER_DATE.format(date);
            break;
        case DATE_TIME:
            result = FORMATTER_DATE_TIME.format(date);
            break;
        default:
            throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (estimated) {
            result = String.format(ESTIMATED_TEMPLATE, result);
        }

        return result;
    }
}
