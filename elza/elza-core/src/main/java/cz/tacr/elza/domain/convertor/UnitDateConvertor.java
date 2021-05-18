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
     * Zkratka století
     */
    public static final String CENTURY = "C";

    /**
     * Výraz pro rok
     */
    public static final String EXP_YEAR = "(\\d+)";

    /**
     * Zkratka roku
     */
    public static final String YEAR = "Y";

    /**
     * Formát datumu
     */
    public static final String FORMAT_DATE = "d.M.u";

    /**
     * Zkratka datumu
     */
    public static final String DATE = "D";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME = "d.M.u H:mm:ss";

    /**
     * Formát datumu s časem
     */
    public static final String FORMAT_DATE_TIME2 = "d.M.u H:mm";

    /**
     * Zkratka datumu s časem
     */
    public static final String DATE_TIME = "DT";

    /**
     * Formát roku s měsícem
     */
    public static final String FORMAT_YEAR_MONTH = "M.u";

    /**
     * Zkratka roku s měsícem
     */
    public static final String YEAR_MONTH = "YM";

    /**
     * Oddělovač pro interval
     */
    public static final String DEFAULT_INTERVAL_DELIMITER = "-";

    /**
     * Oddělovač pro interval, který vyjadřuje odhad
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER = "/";

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
    private static final DateTimeFormatter FORMATTER_DATE_TIME2 = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME2);
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
                    from = LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }

                LocalDateTime to = null;
                if (unitdate.getValueTo() != null) {
                    to = LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }

                if (from != null && to != null && from.isAfter(to)) {
                    throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
                }

            } else {
                Token token = parseToken(normalizedInput, unitdate);
                unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.estimate);
                unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                unitdate.setValueToEstimated(token.estimate);
            }

            if (unitdate.getValueFrom() != null) {
                String valueFrom = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                        LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueFrom.length() != 19) {
                    throw new IllegalArgumentException("Neplatná délka ISO datumů");
                }
            }

            if (unitdate.getValueTo() != null) {
                String valueTo = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                if (valueTo.length() != 19) {
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
     * Provede konverzi formátu do textové podoby.
     */
    public static String convertToString(final IUnitdate unitdate) {

        String format = unitdate.getFormat();

		String formatted;
        if (isInterval(format)) {
			formatted = convertInterval(format, unitdate);
        } else {
			formatted = convertToken(format, unitdate, true, true);
        }

		return formatted;
	}

	/**
	 * Begin of interval to string
	 *
	 * @param unitdate
	 * @return
	 */
	public static String beginToString(final IUnitdate unitdate, final boolean allowEstimate) {
		String format = unitdate.getFormat();
		if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[0];
		}
        String formatted = convertToken(format, unitdate, true, allowEstimate);
		return formatted;
    }

	/**
	 * End of interval to string
	 *
	 * @param unitdate
	 * @return
	 */
	public static String endToString(final IUnitdate unitdate, final boolean allowEstimate) {
		String format = unitdate.getFormat();
		if (isInterval(format)) {
			String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
			format = data[1];
		}
        String formatted = convertToken(format, unitdate, false, allowEstimate);
		return formatted;
	}

	/**
	 * Konverze intervalu.
	 *
	 * @param format
	 *            vstupní formát
	 * @param unitdate
	 *            doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval(final String format, final IUnitdate unitdate) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        String ret;

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getValueFromEstimated()) && BooleanUtils.isTrue(unitdate.getValueToEstimated());

        ret = convertToken(data[0], unitdate, true, !bothEstimate);
        if (bothEstimate) {
            ret += ESTIMATE_INTERVAL_DELIMITER;
        } else {
            ret += DEFAULT_INTERVAL_DELIMITER;
        }
        ret += convertToken(data[1], unitdate, false, !bothEstimate);

        return ret;
    }

    /**
     * Přidání odhadu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @param allow    povolit odhad?
     * @return výsledný řetězec
     */
    private static String addEstimate(String format, final IUnitdate unitdate, final boolean first, final boolean allow) {
        if (first) {
            if (BooleanUtils.isTrue(unitdate.getValueFromEstimated()) && allow) {
                format = "[" + format + "]";
            }
        } else {
            if (BooleanUtils.isTrue(unitdate.getValueToEstimated()) && allow) {
                format = "[" + format + "]";
            }
        }
        return format;
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format
     *            vstupní formát
     * @param unitdate
     *            doplňovaný objekt
     * @param first
     *            zda-li se jedná o první datum
     * @param allowEstimate
     *            povolit odhad?
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final IUnitdate unitdate, final boolean first,
                                       final boolean allowEstimate) {

        if (format.equals("")) {
            return format;
        }

        String ret;
        boolean canAddEstimate = true;
        switch (format) {
            case CENTURY:
                ret = convertCentury(format, unitdate, first);
                canAddEstimate = false;
                break;
            case YEAR:
                ret = convertYear(format, unitdate, first);
                break;
            case YEAR_MONTH:
                ret = convertYearMonth(format, unitdate, first);
                break;
            case DATE:
                ret = convertDate(format, unitdate, first);
                break;
            case DATE_TIME:
                ret = convertDateTime(format, unitdate, first);
                break;
            default:
                throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (canAddEstimate) {
            ret = addEstimate(ret, unitdate, first, allowEstimate);
        }

        return ret;
    }

    /**
     * Konverze datumu s časem.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertDateTime(final String format, final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + DATE_TIME + ")", "" + FORMATTER_DATE_TIME.format(date));
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + DATE_TIME + ")", "" + FORMATTER_DATE_TIME.format(date));
            }
        }
        return format;
    }

    /**
     * Konverze datumu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertDate(final String format, final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + DATE + ")", "" + FORMATTER_DATE.format(date));
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + DATE + ")", "" + FORMATTER_DATE.format(date));
            }
        }
        return format;
    }

    /**
     * Konverze roku s měsícem.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertYearMonth(final String format, final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + YEAR_MONTH + ")", "" + FORMATTER_YEAR_MONTH.format(date));
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + YEAR_MONTH + ")", "" + FORMATTER_YEAR_MONTH.format(date));
            }
        }
        return format;
    }

    /**
     * Konverze roku.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertYear(final String format, final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom().trim());
                return format.replaceFirst("(" + YEAR + ")", "" + date.getYear());
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo().trim());
                return format.replaceFirst("(" + YEAR + ")", "" + date.getYear());
            }
        }
        return format;
    }

    /**
     * Konverze roku.
     *
     * @param unitdate doplňovaný objekt
     * @param first zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    public static String convertYear(final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return date.getYear() + (unitdate.getValueFrom().startsWith(BC_ISO) ? PR_N_L : "");
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return date.getYear() + (unitdate.getValueTo().startsWith(BC_ISO) ? PR_N_L : "");
            }
        }
        return unitdate.getFormat();
    }

    /**
     * Konverze stolení.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertCentury(final String format, final IUnitdate unitdate, final boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + CENTURY + ")", (date.getYear() / 100 + 1) + ". st.");
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + CENTURY + ")", (date.getYear() / 100) + ". st.");
            }
        }
        return format;
    }

    /**
     * Parsování intervalu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     */
    private static void parseInterval(final String input, final IUnitdate unitdate) {

        String[] data = input.split(DEFAULT_INTERVAL_DELIMITER + "|" + ESTIMATE_INTERVAL_DELIMITER);

        Token token;

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + input);
        }

        boolean estimateBoth = input.contains(ESTIMATE_INTERVAL_DELIMITER);

        token = parseToken(data[0], unitdate);
        unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
        unitdate.setValueFromEstimated(token.estimate || estimateBoth);
        unitdate.formatAppend(DEFAULT_INTERVAL_DELIMITER);
        token = parseToken(data[1], unitdate);
        unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
        unitdate.setValueToEstimated(token.estimate || estimateBoth);
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
        } else if (tryParseDate(FORMATTER_DATE, expression)) {
            return parseDate(expression, unitdate);
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
     * Detekce, zda-li se jedná o interval
     *
     * @param input vstupní řetězec
     * @return true - jedná se o interval
     */
    private static boolean isInterval(final String input) {
        return input.contains(DEFAULT_INTERVAL_DELIMITER) || input.contains(ESTIMATE_INTERVAL_DELIMITER);
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
