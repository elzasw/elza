package cz.tacr.elza.print.item.convertors;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cz.tacr.elza.api.IUnitdate;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.*;

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
        return convertDate(format, unitdate.getValueFrom(), unitdate.getValueFromEstimated());
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
        String dateFrom = convertDate(fmt[0], unitdate.getValueFrom(), unitdate.getValueFromEstimated());
        String dateTo = convertDate(fmt[1], unitdate.getValueTo(), unitdate.getValueToEstimated());

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
    public static String convertDate(final String format, final String value, final boolean estimated) {

        String result;
        boolean addEstimate = estimated;

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Chyba při analýze datum: " + value, e);
        }
        switch (format) {
        case CENTURY:
            result = String.format(CENTURY_TEMPLATE, date.getYear() / 100 + 1);
            addEstimate = false;
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

        if (addEstimate) {
            result = String.format(ESTIMATED_TEMPLATE, result);
        }

        return result;
    }
}
