package cz.tacr.elza.domain.convertor;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ParUnitdate;

import org.apache.commons.lang.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    public static final String EXP_CENTURY = "(\\d+)\\.st\\.";

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
    public static final String FORMAT_DATE_TIME = "d.M.u H:m";

    /**
     * Zkratka datumu s časem
     */
    public static final String DATE_TIME = "DT";

    /**
     * Formát roku s měsícem
     */
    public static final String FORMAT_YEAR_MONTH = "u/M";

    /**
     * Zkratka roku s měsícem
     */
    public static final String YEAR_MONTH = "YM";

    /**
     * Oddělovač pro interval
     */
    public static final String INTERVAL_DELIMITER = "-";

    private static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(FORMAT_DATE);
    private static final DateTimeFormatter FORMATTER_DATE_TIME = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME);
    private static final DateTimeFormatter FORMATTER_YEAR_MONTH = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH);
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Provede konverzi textového vstupu a doplní intervaly do objektu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     * @return doplněný objekt
     */
    public static <T extends IUnitdate<ArrCalendarType>> T convertToUnitDate(final String input, final T unitdate) {

        unitdate.setFormat("");

        try {
            if (isInterval(input)) {
                parseInterval(input, unitdate);

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
                Token token = parseToken(input, unitdate);
                unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.opt);
                unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                unitdate.setValueToEstimated(token.opt);
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
            throw new IllegalStateException("Vstupní řetězec není validní");
        }

        return unitdate;
    }

    /**
     * Převede {@link ParUnitdate} na string.
     *
     * @param unitdate datum
     * @return string
     */
    public static String convertParUnitDateToString(final ParUnitdate unitdate) {
        String textDate;
        if (StringUtils.isEmpty(unitdate.getTextDate())) {
            try {
                textDate = UnitDateConvertor.convertToString(unitdate);
            } catch (Exception e) {
                textDate = unitdate.getTextDate();
            }
        } else {
            textDate = unitdate.getTextDate();
        }
        return textDate;
    }

    /**
     * Provede konverzi formátu do textové podoby.
     */
    public static String convertToString(final IUnitdate unitdate) {

        String format = unitdate.getFormat();

        if (isInterval(format)) {
            format = convertInterval(format, unitdate);
        } else {
            format = convertToken(format, unitdate, true);
        }

        return format;
    }

    /**
     * Konverze intervalu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @return výsledný řetězec
     */
    private static String convertInterval(String format, IUnitdate unitdate) {

        String[] data = format.split(INTERVAL_DELIMITER);

        String ret;

        switch (data.length) {
            case 1:
                ret = convertToken(data[0], unitdate, true);
                ret += INTERVAL_DELIMITER;
                break;
            case 2:
                ret = convertToken(data[0], unitdate, true);
                ret += INTERVAL_DELIMITER;
                ret += convertToken(data[1], unitdate, false);
                break;
            default:
                throw new IllegalStateException();
        }

        return ret;
    }

    /**
     * Přidání odhadu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String addEstimate(String format, IUnitdate unitdate, boolean first) {
        if (first) {
            if (unitdate.getValueFromEstimated()) {
                format = "(" + format + ")";
            }
        } else {
            if (unitdate.getValueToEstimated()) {
                format = "(" + format + ")";
            }
        }
        return format;
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final IUnitdate unitdate, boolean first) {

        if (format.equals("")) {
            return format;
        }

        String ret;

        switch (format) {
            case CENTURY:
                ret = convertCentury(format, unitdate, first);
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
                throw new IllegalStateException();
        }

        ret = addEstimate(ret, unitdate, first);

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
    private static String convertDateTime(String format, IUnitdate unitdate, boolean first) {
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
    private static String convertDate(String format, IUnitdate unitdate, boolean first) {
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
    private static String convertYearMonth(String format, IUnitdate unitdate, boolean first) {
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
    private static String convertYear(String format, IUnitdate unitdate, boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + YEAR + ")", "" + date.getYear());
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + YEAR + ")", "" + date.getYear());
            }
        }
        return format;
    }

    /**
     * Konverze stolení.
     *
     * @param format   vstupní formát
     * @param unitdate doplňovaný objekt
     * @param first    zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    private static String convertCentury(final String format, final IUnitdate unitdate, boolean first) {
        if (first) {
            if (unitdate.getValueFrom() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueFrom());
                return format.replaceFirst("(" + CENTURY + ")", (date.getYear() / 100 + 1) + ".st.");
            }
        } else {
            if (unitdate.getValueTo() != null) {
                LocalDateTime date = LocalDateTime.parse(unitdate.getValueTo());
                return format.replaceFirst("(" + CENTURY + ")", (date.getYear() / 100) + ".st.");
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

        String[] data = input.split(INTERVAL_DELIMITER);

        Token token;

        switch (data.length) {
            case 1:
                token = parseToken(data[0], unitdate);
                unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.opt);
                unitdate.setValueTo(null);
                unitdate.setValueToEstimated(false);
                unitdate.formatAppend(INTERVAL_DELIMITER);
                break;
            case 2:
                token = parseToken(data[0], unitdate);
                unitdate.setValueFrom(token.dateFrom == null ? null : FORMATTER_ISO.format(token.dateFrom));
                unitdate.setValueFromEstimated(token.opt);
                unitdate.formatAppend(INTERVAL_DELIMITER);
                token = parseToken(data[1], unitdate);
                unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                unitdate.setValueToEstimated(token.opt);
                break;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Parsování tokenu.
     *
     * @param tokenString výraz
     * @param unitdate    doplňovaný objekt
     * @return výsledný token
     */
    private static Token parseToken(final String tokenString, final IUnitdate unitdate) {
        if (tokenString.equals("")) {
            return new Token();
        }

        Token token;

        if (tokenString.charAt(0) == '(' && tokenString.charAt(tokenString.length() - 1) == ')') {
            String tokenStringTrim = tokenString.substring(1, tokenString.length() - 1);
            token = parseExpression(tokenStringTrim, unitdate);
            token.opt = true;
        } else {
            token = parseExpression(tokenString, unitdate);
            token.opt = false;
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
        } else if (tryParseDate(FORMATTER_DATE_TIME, expression)) {
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
    private static Token parseYearMonth(String yearMonthString, IUnitdate unitdate) {
        unitdate.formatAppend(YEAR_MONTH);

        Token token = new Token();
        try {
            YearMonth yearMonth = YearMonth.parse(yearMonthString, FORMATTER_YEAR_MONTH);
            LocalDateTime date = LocalDateTime.from(yearMonth.atDay(1).atStartOfDay());
            token.dateFrom = date;
            date = date.plusMonths(1);
            token.dateTo = date.minusSeconds(1);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
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
            token.dateTo = date.plusSeconds(59);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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

        } catch (NumberFormatException | DateTimeParseException e) {
            e.printStackTrace();
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
    private static boolean tryParseDate(DateTimeFormatter formatter, String s) {
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
    private static boolean isInterval(String input) {
        return input.contains(INTERVAL_DELIMITER);
    }

    /**
     * Pomocná třída pro reprezentaci jednoho výrazu.
     */
    private static class Token {

        public LocalDateTime dateFrom = null;

        public LocalDateTime dateTo = null;

        public boolean opt = false;
    }

}
