package cz.tacr.elza.domain.convertor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.*;

/**
 * Konvertor pro sprváné zobrazování UnitDate podle formátu.
 *
 * @author Martin Šlapa
 * @since 6.11.2015
 */
public class UnitDateConvertor {

    /**
     * Výraz pro detekci stolení
     */
    public static final String EXP_CENTURY = "(\\d+)((st)|(\\.[ ]?st\\.))";

    /**
     * Šablona pro století
     */
    public static final String CENTURY_TEMPLATE = "%d. st.";

    /**
     * Výraz pro rok
     */
    public static final String EXP_YEAR = "(-?\\d{1,4})";

    /**
     * Formát datumu
     */
    public static final String FORMAT_DATE = "d.M.u";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME = "d.M.u H:mm:ss";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME_WITHOUT_SEC = "d.M.u H:mm";

    /**
     * Formát roku s měsícem
     */
    public static final String FORMAT_YEAR_MONTH = "M.u";

    /**
     * Šablona pro interval
     */
    public static final String DEFAULT_INTERVAL_DELIMITER_TEMPLATE = "%s-%s";

    /**
     * Oddělovač pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER = "/";

    /**
     * Šablona pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER_TEMPLATE = "%s/%s";

    /**
     * Když druhý rok v intervalu je negativní
     */
    public static final String SECOND_YEAR_IS_NEGATIVE = "--";

    /**
     * Suffix př. n. l.
     */
    public static final String PR_N_L = " př. n. l.";

    /**
     * Záporná reprezentace v ISO formátu.
     */
    public static final String BC_ISO = "-";

    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(FORMAT_DATE);
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
    private static final DateTimeFormatter FORMATTER_DATE_TIME2 = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME_WITHOUT_SEC);
    private static final DateTimeFormatter FORMATTER_YEAR_MONTH = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH);
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Provede konverzi textového vstupu a doplní intervaly do objektu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     * @return doplněný objekt
     */
    public static <T extends IUnitdate> T convertToUnitDate(final String input, final T unitdate) {

        unitdate.setFormat("");

        String normalizedInput = normalizeInput(input);

        try {
            if (isInterval(normalizedInput)) {
                parseInterval(normalizedInput, unitdate);

                LocalDateTime from = null;
                if (unitdate.getValueFrom() != null) {
                    from = LocalDateTime.parse(unitdate.getValueFrom(), FORMATTER_ISO);
                }

                LocalDateTime to = null;
                if (unitdate.getValueTo() != null) {
                    to = LocalDateTime.parse(unitdate.getValueTo(), FORMATTER_ISO);
                }

                if (from != null && to != null && from.isAfter(to)) {
                    throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
                }

            } else {
                Token token = parseToken(moveMinusToYearDate(normalizedInput), unitdate);
                unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.estimate);
                unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                unitdate.setValueToEstimated(token.estimate);
            }

            if (unitdate.getValueFrom() != null) {
                String valueFrom = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                        LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueFrom.length() != 19 && valueFrom.length() != 20) {
                    throw new IllegalArgumentException("Neplatná délka ISO datumů");
                }
            }

            if (unitdate.getValueTo() != null) {
                String valueTo = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueTo.length() != 19 && valueTo.length() != 20) {
                    throw new IllegalArgumentException("Neplatná délka ISO datumů");
                }
            }

        } catch (Exception e) {
            unitdate.setFormat("");
            throw new SystemException("Vstupní řetězec není validní", BaseCode.PROPERTY_IS_INVALID)
                    .set("property", "format")
                    .set("value", input);
        }

        return unitdate;
    }

    /**
     * Normalizace závorek (na hranaté) a odstranění bílých, přebytečných znaků.
     *
     * @param input text k normalizaci
     * @return normalizovaný text
     */
    private static String normalizeInput(final String input) {
        return input.replace("(", "[").replace(")", "]").trim();
    }

    /**
     * Detekce, zda-li se jedná o interval
     * Interval existuje, pokud je nalezen oddělovač '/' nebo '-', ale vylučujeme situace:
     * Intervaly:
     *      1900-1912
     *      1900/1912
     *      -7-2
     *      -7/-2
     *      -7--2
     *      -1.3.7--14.10.2
     *      [-10--8]
     * Samostatne:
     *      -12.3.44
     *      -18
     *      [-20]
     *
     * @param input vstupní řetězec
     * @return true - jedná se o interval
     */
    private static boolean isInterval(final String input) {
        if (input.contains(ESTIMATE_INTERVAL_DELIMITER)) {
            return true; // 1900/1902
        }
        String dateString = input;
        if (input.startsWith("-")) {
            dateString = dateString.substring(1); // vyloučit -8
        } else if (input.startsWith("[-")) {
            dateString = dateString.substring(2); // vyloučit [-8]
        }

        return dateString.contains(DEFAULT_INTERVAL_DELIMITER);
    }

    /**
     * Parsování intervalu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     */
    private static void parseInterval(final String input, final IUnitdate unitdate) {
        Token token;
        String[] data = splitInterval(input);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + input);
        }

        boolean estimateBoth = input.contains(ESTIMATE_INTERVAL_DELIMITER);

        token = parseToken(moveMinusToYearDate(data[0]), unitdate);
        unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
        unitdate.setValueFromEstimated(token.estimate || estimateBoth);
        unitdate.formatAppend(DEFAULT_INTERVAL_DELIMITER);
        token = parseToken(moveMinusToYearDate(data[1]), unitdate);
        unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
        unitdate.setValueToEstimated(token.estimate || estimateBoth);
    }

    /**
     * Rozdělení řetězce s datovým intervalem na dva řádky
     * 
     * @param input
     * @return
     */
    private static String[] splitInterval(final String input) {
        String delimiter = SECOND_YEAR_IS_NEGATIVE;

        // vzorek: datum/datum
        if (input.contains(ESTIMATE_INTERVAL_DELIMITER)) {
            return input.split(ESTIMATE_INTERVAL_DELIMITER);
        }
        // vzorek: datum-datum
        if (!input.contains(SECOND_YEAR_IS_NEGATIVE)) {
            if (!input.startsWith("-")) {
                return input.split(DEFAULT_INTERVAL_DELIMITER);
            }
            // vzorek: -datum-datum
            delimiter = DEFAULT_INTERVAL_DELIMITER;
        }

        // vzorek: [-]datum--datum
        int position = input.indexOf(delimiter, 1);
        String[] parts = {input.substring(0, position), input.substring(position + 1)};

        return parts;
    }

    /**
     * Provede konverzi formátu do textové podoby.
     * 
     * @param unitdate
     * @return String
     */
    public static String convertToString(final IUnitdate unitdate) {

        String format = unitdate.getFormat();

        if (isInterval(format)) {
            return convertInterval(format, unitdate);
        }
        return convertToken(format, unitdate.getValueFrom(), unitdate.getValueFromEstimated());
    }

    /**
	 * Begin of interval to string
	 *
	 * @param unitdate
	 * @param allowEstimate
     * @return String
	 */
	public static String beginToString(final IUnitdate unitdate, final boolean allowEstimate) {

	    String format = unitdate.getFormat();

	    if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[0];
		}
	    return convertToken(format, unitdate.getValueFrom(), allowEstimate && unitdate.getValueFromEstimated());
    }

	/**
	 * End of interval to string
	 *
	 * @param unitdate
	 * @return String
	 */
	public static String endToString(final IUnitdate unitdate, final boolean allowEstimate) {

	    String format = unitdate.getFormat();

		if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[1];
		}
		return convertToken(format, unitdate.getValueTo(), allowEstimate && unitdate.getValueToEstimated());
	}

	/**
	 * Konverze intervalu.
	 *
	 * @param format   vstupní formát
	 * @param unitdate doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval(final String format, final IUnitdate unitdate) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getValueFromEstimated()) && BooleanUtils.isTrue(unitdate.getValueToEstimated());

        String template = bothEstimate? ESTIMATE_INTERVAL_DELIMITER_TEMPLATE : DEFAULT_INTERVAL_DELIMITER_TEMPLATE;  
        String dateFrom = convertToken(data[0], unitdate.getValueFrom(), !bothEstimate && unitdate.getValueFromEstimated());
        String dateTo = convertToken(data[1], unitdate.getValueTo(), !bothEstimate && unitdate.getValueToEstimated());

        return String.format(template, dateFrom, dateTo);
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format        vstupní formát
     * @param unitdate      doplňovaný objekt
     * @param first         zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final String value, final boolean estimated) {

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
                result = moveMinusToDayDate(FORMATTER_YEAR_MONTH.format(date));
                break;
            case DATE:
                result = moveMinusToDayDate(FORMATTER_DATE.format(date));
                break;
            case DATE_TIME:
                result = moveMinusToDayDate(FORMATTER_DATE_TIME.format(date));
                break;
            default:
                throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (addEstimate) {
            result = String.format(ESTIMATED_TEMPLATE, result);
        }

        return result;
    }

    /**
     * Konverze roku.
     *
     * @param unitdate doplňovaný objekt
     * @param first zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    public static String convertYear(final IUnitdate unitdate, final boolean first) {
        LocalDateTime date = getLocalDateTimeFromUnitDate(unitdate, first);
        if (date != null) {
            return date.getYear() + (unitdate.getValueFrom().startsWith(BC_ISO) ? PR_N_L : "");
        }
        return unitdate.getFormat();
    }

    /**
     * Získání LocalDateTime z objektu IUnitdate.
     * 
     * @param unitdate
     * @param first
     * @return LocalDateTime
     */
    private static LocalDateTime getLocalDateTimeFromUnitDate(final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                return LocalDateTime.parse(unitdate.getValueFrom());
            }
        } else {
            if (unitdate.getValueTo() != null) {
                return LocalDateTime.parse(unitdate.getValueTo());
            }
        }
        return null;
    }

    /**
     * Parsování tokenu.
     *
     * @param tokenString výraz
     * @param unitdate    doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseToken(final String tokenString, final IUnitdate unitdate) {
        if (StringUtils.isEmpty(tokenString)) {
            throw new IllegalArgumentException("Nemůže existovat prázdný interval");
        }

        Token token;

        if (tokenString.charAt(0) == '[' && tokenString.charAt(tokenString.length() - 1) == ']') {
            String tokenStringTrim = tokenString.substring(1, tokenString.length() - 1);
            token = parseExpression(tokenStringTrim, unitdate);
            token.estimate = true;
        } else {
            token = parseExpression(tokenString, unitdate);
        }

        return token;
    }

    /**
     * Parsování výrazu.
     *
     * @param expression výraz
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseExpression(final String expression, final IUnitdate unitdate) {

        if (expression.matches(EXP_CENTURY)) {
            return parseCentury(expression, unitdate);
        } else if (expression.matches(EXP_YEAR)) {
            return parseYear(expression, unitdate);
        } else if (tryParseDate(FORMATTER_YEAR_MONTH, expression)) {
            return parseYearMonth(expression, unitdate);
        } else if (tryParseDate(FORMATTER_DATE_TIME, expression) || tryParseDate(FORMATTER_DATE_TIME2, expression) ) {
            return parseDateTime(expression, unitdate);
        } else if (tryParseDate(FORMATTER_DATE, moveMinusToYearDate(expression))) {
            return parseDate(moveMinusToYearDate(expression), unitdate);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Parsování roku s měsícem.
     *
     * @param yearMonthString rok s měsícem
     * @param unitdate        doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseYearMonth(final String yearMonthString, final IUnitdate unitdate) {
        unitdate.formatAppend(YEAR_MONTH);

        Token token = new Token();
        try {
            YearMonth yearMonth = YearMonth.parse(yearMonthString, FORMATTER_YEAR_MONTH);
            LocalDateTime date = LocalDateTime.from(yearMonth.atDay(1).atStartOfDay());
            token.dateFrom = date;
            date = date.plusMonths(1);
            token.dateTo = date.minusSeconds(1);
        } catch (DateTimeParseException e) {
            throw new SystemException("Failed to parse", BaseCode.PROPERTY_IS_INVALID)
                    .set("value", yearMonthString);
        }

        return token;
    }

    /**
     * Parsování datumu s časem.
     *
     * @param dateString datum s časem
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseDateTime(final String dateString, final IUnitdate unitdate) {
        unitdate.formatAppend(DATE_TIME);

        Token token = new Token();
        try {
            LocalDateTime date = LocalDateTime.parse(dateString, FORMATTER_DATE_TIME);
            token.dateFrom = date;
            token.dateTo = date;
        } catch (DateTimeParseException e) {
            LocalDateTime date = LocalDateTime.parse(dateString, FORMATTER_DATE_TIME2);
            token.dateFrom = date;
            token.dateTo = date.plusSeconds(59);
        }

        return token;
    }

    /**
     * Parsování datumu.
     *
     * @param dateString datum
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseDate(final String dateString, final IUnitdate unitdate) {
        unitdate.formatAppend(DATE);

        Token token = new Token();
        try {
            LocalDateTime date = LocalDateTime.from(LocalDate.parse(dateString, FORMATTER_DATE).atStartOfDay());
            token.dateFrom = date;
            date = date.plusDays(1);
            date = date.minusSeconds(1);
            token.dateTo = date;
        } catch (DateTimeParseException e) {
            throw new SystemException("Failed to parse", BaseCode.PROPERTY_IS_INVALID)
                    .set("value", dateString);
        }

        return token;
    }

    /**
     * Parsování roku.
     *
     * @param yearString rok
     * @param unitdate   doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseYear(final String yearString, final IUnitdate unitdate) {
        unitdate.formatAppend(YEAR);
        Token token = new Token();
        try {
            Integer year = Integer.parseInt(yearString);
            token.dateFrom = LocalDateTime.of(year, 1, 1, 0, 0);
            token.dateTo = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new SystemException("Failed to parse", BaseCode.PROPERTY_IS_INVALID)
                    .set("value", yearString);
        }

        return token;
    }

    /**
     * Parsování stolení.
     *
     * @param centuryString stolení
     * @param unitdate      doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseCentury(final String centuryString, final IUnitdate unitdate) {
        unitdate.formatAppend(CENTURY);
        Token token = new Token();
        try {

            Pattern pattern = Pattern.compile(EXP_CENTURY);
            Matcher matcher = pattern.matcher(centuryString);

            Integer c;

            if (matcher.find()) {
                c = Integer.parseInt(matcher.group(1));
            } else {
                throw new IllegalStateException();
            }

            token.dateFrom = LocalDateTime.of((c - 1) * 100 + 1, 1, 1, 0, 0);
            token.dateTo = LocalDateTime.of(c * 100, 12, 31, 23, 59, 59);
            token.estimate = true;

        } catch (NumberFormatException | DateTimeParseException e) {
            throw new SystemException("Failed to parse", BaseCode.PROPERTY_IS_INVALID)
                    .set("value", centuryString);
        }

        return token;
    }

    /**
     * Testování, zda-li odpovídá řetězec formátu
     *
     * @param formatter formát
     * @param s         řetězec
     * @return true - lze parsovat
     */
    private static boolean tryParseDate(final DateTimeFormatter formatter, final String s) {
        try {
            formatter.withResolverStyle(ResolverStyle.STRICT);
            formatter.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Přesunutí znaménka minus z začátku data na rok 
     * 
     * @param s datum, např -1.2.1024, -2.1024
     * @return String, např 1.2.-1024, 2.-1024
     */
    private static String moveMinusToYearDate(final String s) {
        if (!s.startsWith("-")) {
            return s;
        }
        String[] parts = s.split("\\.");
        switch (parts.length) {
        // jen rok
        case 1:
            return s;
        // měsíc a rok
        case 2:
            return String.format("%s.-%s", parts[0].substring(1), parts[1]);
        // den, měsíc a rok
        case 3:
            return String.format("%s.%s.-%s", parts[0].substring(1), parts[1], parts[2]);  
        default:
            throw new IllegalStateException("Chyba formátu data: " + s);
        }
    }

    /**
     * Přesunutí znaménka mínus z roku na začátek data 
     * 
     * @param s datum, např 1.2.-1024
     * @return String, např -1.2.1024
     */
    private static String moveMinusToDayDate(final String s) {
        String[] parts = s.split("\\.");
        switch (parts.length) {
        // jen rok
        case 1:
            return s;
        // měsíc a rok
        case 2:
            if (!parts[1].startsWith("-")) {
                return s;
            }
            return String.format("-%s.%s", parts[0], parts[1].substring(1));
        // den, měsíc a rok
        case 3:
            if (!parts[2].startsWith("-")) {
                return s;
            }
            return String.format("-%s.%s.%s", parts[0], parts[1], parts[2].substring(1));  
        default:
            throw new IllegalStateException("Chyba formátu data: " + s);
        }
    }

    public static <T extends IUnitdate> T convertIsoToUnitDate(final String input, final T unitDate) {
        if (tryParseDate(FORMATTER_ISO, input)) {
            unitDate.setValueFrom(input);
            unitDate.setValueFromEstimated(false);
            unitDate.setValueTo(input);
            unitDate.setValueToEstimated(true);
        } else {
            int isoLength = 19;
            if (input.startsWith(BC_ISO)) {
                isoLength++;
            }
            String from = input.substring(0, isoLength);
            String to = input.substring(isoLength + 1);

            if (!tryParseDate(FORMATTER_ISO, from) && !tryParseDate(FORMATTER_ISO, to)) {
                throw new IllegalStateException("Neplatný interval: " + input);
            }

            unitDate.setValueFrom(from);
            unitDate.setValueFromEstimated(false);
            unitDate.setValueTo(to);
            unitDate.setValueToEstimated(false);
        }

        return unitDate;
    }

    /**
     * Pomocná třída pro reprezentaci jednoho výrazu.
     */
    private static class Token {

        public LocalDateTime dateFrom = null;

        public LocalDateTime dateTo = null;

        public boolean estimate = false;
    }

}
