package cz.tacr.elza.domain.converter;

import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.CENTURY;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE_TIME;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DEFAULT_INTERVAL_DELIMITER;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.FORMAT_DELIMITER;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR_MONTH;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.domain.converter.UnitDateLexicon.Language;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Conversion between the text of a unit date and its stored form (ISO from/to, format, estimates).
 *
 * <p>Parsing is language-independent: every form of every language in {@link UnitDateLexicon} is
 * accepted whatever the UI language, plus the negative year and the ISO full date. Rendering follows
 * the language asked for; the overloads without a language render in the default language of the
 * lexicon (the form stored in indexes and titles). The corpus {@code unitdate/cases.json} fixes the
 * behaviour of both this class and the client parser.
 *
 * <p>Stored form: {@code valueFrom}/{@code valueTo} as ISO local date-times, a year BC as the ISO
 * year {@code 1 - year} (1 BC is year 0), the format as the codes of {@link UnitDateConverterConsts}
 * joined by "-" for an interval.
 *
 * @since 6.11.2015
 */
public class UnitDateConverter {

    /**
     * Delimiter of an interval whose both ends are estimated
     */
    public static final String ESTIMATE_INTERVAL_DELIMITER = "/";

    /**
     * Two delimiters in a row: the second year of the interval is negative
     */
    public static final String SECOND_YEAR_IS_NEGATIVE = "--";

    /**
     * Sign of a year BC in the ISO form.
     */
    public static final String BC_ISO = "-";

    /**
     * Enum of format types
     */
    public enum FormatType {
    	C,
    	Y,
    	YM,
    	D,
    	DT
    }

    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final UnitDateLexicon LEXICON = UnitDateLexicon.get();

    private static final List<Form> FORMS = buildForms(LEXICON);

    enum TokenType {
    	SINGLE,
    	FROM,
    	TO
    }

    /**
     * Parsed date part
     */
    private static class Token {

    	private final String format;

    	private final boolean estimate;

		public Token(final String format, final boolean estimate) {
    		this.format = format;
    		this.estimate = estimate;
    	}

		public String getFormat() {
			return format;
		}

        public LocalDateTime dateFrom = null;

        public LocalDateTime dateTo = null;
    }

    /**
     * What a text form denotes and the meaning of its groups.
     */
    private enum Kind {
        /** group: year */
        YEAR,
        /** groups: month (number or name), year */
        YEAR_MONTH,
        /** groups: day, month (number or name), year */
        DATE,
        /** groups: day, month (number or name), year, hour, minute, second (optional) */
        DATE_TIME,
        /** groups: year, month, day */
        ISO_DATE,
        /** groups: year, month, day, hour, minute, second (optional) */
        ISO_DATE_TIME,
        /** group: century */
        CENTURY
    }

    /**
     * One text form of a date part: its pattern, what it denotes, whether it is a form BC and the
     * language whose month names it uses.
     */
    private static final class Form {

        private final Pattern pattern;

        private final Kind kind;

        private final boolean bc;

        private final Language language;

        Form(final String regex, final Kind kind, final boolean bc, final Language language) {
            this.pattern = Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            this.kind = kind;
            this.bc = bc;
            this.language = language;
        }

        Token parse(final Matcher m, final boolean estimate, final boolean negative, final TokenType type) {
            if (bc && negative) {
                throw new SystemException("Double negative not supported", BaseCode.PROPERTY_IS_INVALID);
            }
            boolean beforeChrist = bc || negative;
            switch (kind) {
            case YEAR:
                return yearToken(isoYear(Integer.parseInt(m.group(1)), beforeChrist), estimate);
            case YEAR_MONTH:
                return yearMonthToken(isoYear(Integer.parseInt(m.group(2)), beforeChrist), language.month(m.group(1)),
                                      estimate);
            case DATE:
                return dateToken(LocalDate.of(isoYear(Integer.parseInt(m.group(3)), beforeChrist),
                                              language.month(m.group(2)), Integer.parseInt(m.group(1))),
                                 estimate);
            case ISO_DATE:
                return dateToken(LocalDate.of(isoYear(Integer.parseInt(m.group(1)), beforeChrist),
                                              Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))),
                                 estimate);
            case DATE_TIME:
                return dateTimeToken(LocalDate.of(isoYear(Integer.parseInt(m.group(3)), beforeChrist),
                                                  language.month(m.group(2)), Integer.parseInt(m.group(1))),
                                     time(m.group(4), m.group(5), m.group(6)), m.group(6) == null, estimate, type);
            case ISO_DATE_TIME:
                return dateTimeToken(LocalDate.of(isoYear(Integer.parseInt(m.group(1)), beforeChrist),
                                                  Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))),
                                     time(m.group(4), m.group(5), m.group(6)), m.group(6) == null, estimate, type);
            case CENTURY:
                int century = Integer.parseInt(m.group(1));
                if (century == 0 && !beforeChrist) {
                    throw new IllegalArgumentException("Century 0 does not exist");
                }
                return centuryToken(beforeChrist ? -century + 1 : century, estimate);
            default:
                throw new IllegalStateException("Unexpected kind: " + kind);
            }
        }
    }

    /**
     * The forms in the order they are tried: the forms AD of every language, then the forms BC, as
     * the Czech-only parser tried them. Patterns match the whole token, case-insensitively.
     */
    private static List<Form> buildForms(final UnitDateLexicon lexicon) {
        UnitDateLexicon.Common common = lexicon.common();
        List<Form> forms = new ArrayList<>();
        forms.add(new Form(common.year, Kind.YEAR, false, null));
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.yearMonth, Kind.YEAR_MONTH, false, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.date + "\\s+" + common.time, Kind.DATE_TIME, false, language));
        }
        forms.add(new Form(common.isoDate + "\\s+" + common.time, Kind.ISO_DATE_TIME, false, null));
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.date, Kind.DATE, false, language));
        }
        forms.add(new Form(common.isoDate, Kind.ISO_DATE, false, null));
        for (Language language : lexicon.languages()) {
            forms.add(new Form("(\\d+)" + language.century, Kind.CENTURY, false, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form("(\\d+)" + language.century + language.bc, Kind.CENTURY, true, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form(common.bcYear + language.bc, Kind.YEAR, true, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.yearMonth + language.bc, Kind.YEAR_MONTH, true, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.date + language.bc, Kind.DATE, true, language));
        }
        for (Language language : lexicon.languages()) {
            forms.add(new Form(language.date + "\\s+" + common.time + language.bc, Kind.DATE_TIME, true, language));
        }
        return List.copyOf(forms);
    }

    /**
     * ISO year of a chronological year: 1 BC is the ISO year 0, 2 BC is -1.
     */
    private static int isoYear(final int year, final boolean beforeChrist) {
        return beforeChrist ? -year + 1 : year;
    }

    private static LocalTime time(final String hour, final String minute, final String second) {
        int h = Integer.parseInt(hour);
        int m = Integer.parseInt(minute);
        int s = second != null ? Integer.parseInt(second) : 0;
        if (h > 23 || m > 59 || s > 59) {
            throw new IllegalArgumentException("Invalid time: " + hour + ":" + minute + (second != null ? ":" + second : ""));
        }
        return LocalTime.of(h, m, s);
    }

    private static Token yearToken(final int isoYear, final boolean estimate) {
        Token token = new Token(YEAR, estimate);
        token.dateFrom = LocalDateTime.of(isoYear, 1, 1, 0, 0);
        token.dateTo = LocalDateTime.of(isoYear, 12, 31, 23, 59, 59);
        return token;
    }

    private static Token yearMonthToken(final int isoYear, final int month, final boolean estimate) {
        Token token = new Token(YEAR_MONTH, estimate);
        token.dateFrom = LocalDate.of(isoYear, month, 1).atStartOfDay();
        token.dateTo = token.dateFrom.plusMonths(1).minusSeconds(1);
        return token;
    }

    private static Token dateToken(final LocalDate date, final boolean estimate) {
        Token token = new Token(DATE, estimate);
        token.dateFrom = date.atStartOfDay();
        token.dateTo = token.dateFrom.plusDays(1).minusSeconds(1);
        return token;
    }

    /**
     * @param withoutSeconds
     *            the text has no seconds: the part spans the whole minute, and alone it is stored as
     *            an interval of two date-times
     */
    private static Token dateTimeToken(final LocalDate date, final LocalTime time, final boolean withoutSeconds,
                                       final boolean estimate, final TokenType type) {
        String format = DATE_TIME;
        if (withoutSeconds && type == TokenType.SINGLE) {
            format = DATE_TIME + FORMAT_DELIMITER + DATE_TIME;
        }
        Token token = new Token(format, estimate);
        token.dateFrom = LocalDateTime.of(date, time);
        token.dateTo = withoutSeconds ? token.dateFrom.plusSeconds(59) : token.dateFrom;
        return token;
    }

    private static Token centuryToken(final int century, final boolean estimate) {
        Token token = new Token(CENTURY, estimate);
        token.dateFrom = LocalDateTime.of((century - 1) * 100 + 1, 1, 1, 0, 0);
        token.dateTo = LocalDateTime.of(century * 100, 12, 31, 23, 59, 59);
        return token;
    }

    /**
     * Provede konverzi textového vstupu a doplní intervaly do objektu.
     *
     * @param input    textový vstup
     * @param unitdate doplňovaný objekt
     * @return doplněný objekt
     */
    public static <T extends IUnitdate> T convertToUnitDate(final String input, final T unitdate) {

        unitdate.setFormat("");

        try {
            String normalizedInput = normalizeInput(input);
            UnitDateLexicon.Common common = LEXICON.common();

            if (normalizedInput.contains(common.estimateIntervalDelimiter)) {
                // 1985/1990: both ends estimated
                String[] parts = normalizedInput.split(Pattern.quote(common.estimateIntervalDelimiter));
                if (parts.length != 2) {
                    throw new IllegalStateException("Neplatný interval: " + normalizedInput);
                }
                parseInterval(parts[0], parts[1], true, unitdate);
            } else if (normalizedInput.contains(common.spacedIntervalDelimiter)) {
                // 1968 - 1969, 1968-08-21 - 1969-01-01: the spaced delimiter may separate dates containing "-"
                int position = normalizedInput.indexOf(common.spacedIntervalDelimiter);
                parseInterval(normalizedInput.substring(0, position),
                              normalizedInput.substring(position + common.spacedIntervalDelimiter.length()), false,
                              unitdate);
            } else {
                try {
                    // a single date part, including the ISO date with "-" inside
                    Token token = parseToken(normalizedInput, TokenType.SINGLE);
                    unitdate.formatAppend(token.getFormat());
                    unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
                    unitdate.setValueFromEstimated(token.estimate);
                    unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
                    unitdate.setValueToEstimated(token.estimate);
                } catch (RuntimeException e) {
                    String[] parts = splitInterval(normalizedInput);
                    if (parts == null) {
                        throw e;
                    }
                    parseInterval(parts[0], parts[1], false, unitdate);
                }
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

            normalize(unitdate);

        } catch (Exception e) {
            unitdate.setFormat("");
            throw new SystemException("Vstupní řetězec není validní", e, BaseCode.PROPERTY_IS_INVALID)
                    .set("property", "format")
                    .set("value", input);
        }

        return unitdate;
    }

    /**
     * Vyplnění polí normalizeFrom a normalizeTo
     *
     * @param aeDataUnitdate
     */
    public static void normalize(IUnitdate aeDataUnitdate) {

        String valueFrom = aeDataUnitdate.getValueFrom();
        if (valueFrom == null) {
            aeDataUnitdate.setNormalizedFrom(Long.MIN_VALUE);
        } else {
            LocalDateTime fromDate = LocalDateTime.parse(valueFrom.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedFrom(CalendarConverter.toSeconds(fromDate));
        }

        String valueTo = aeDataUnitdate.getValueTo();
        if (valueTo == null) {
            aeDataUnitdate.setNormalizedTo(Long.MAX_VALUE);
        } else {
            LocalDateTime toDate = LocalDateTime.parse(valueTo.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            aeDataUnitdate.setNormalizedTo(CalendarConverter.toSeconds(toDate));
        }
    }

    /**
     * Brackets of an estimate to square brackets, aliases of the interval delimiter (en dash) to the
     * delimiter, surrounding white space removed.
     *
     * @param input text k normalizaci
     * @return normalizovaný text
     */
    private static String normalizeInput(final String input) {
        UnitDateLexicon.Common common = LEXICON.common();
        String result = input.trim();
        for (String open : common.estimateOpen) {
            result = result.replace(open, "[");
        }
        for (String close : common.estimateClose) {
            result = result.replace(close, "]");
        }
        for (String alias : common.intervalDelimiterAliases) {
            result = result.replace(alias, common.intervalDelimiter);
        }
        return result;
    }

    /**
     * Check whether supplied unitdate is interval
     * @param unitDate
     * @return
     */
    public static boolean isInterval(IUnitdate unitDate) {
    	String dateFormat = unitDate.getFormat();
    	if(dateFormat.contains(UnitDateConverterConsts.FORMAT_DELIMITER)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    private static boolean isInterval(final String format) {
        return format.contains(FORMAT_DELIMITER);
    }

    /**
     * Parsování intervalu.
     *
     * @param from
     *            text of the beginning
     * @param to
     *            text of the end
     * @param estimateBoth
     *            both ends are estimated (interval written with "/")
     * @param unitdate doplňovaný objekt
     */
    private static void parseInterval(final String from, final String to, final boolean estimateBoth,
                                      final IUnitdate unitdate) {
        Token token = parseToken(from, TokenType.FROM);
        unitdate.formatAppend(token.getFormat());
        unitdate.setValueFrom(FORMATTER_ISO.format(token.dateFrom));
        unitdate.setValueFromEstimated(token.estimate || estimateBoth);
        LocalDateTime dateFrom = token.dateFrom;

        unitdate.formatAppend(DEFAULT_INTERVAL_DELIMITER);

        token = parseToken(to, TokenType.TO);
        unitdate.formatAppend(token.getFormat());
        unitdate.setValueTo(FORMATTER_ISO.format(token.dateTo));
        unitdate.setValueToEstimated(token.estimate || estimateBoth);

        if (dateFrom.isAfter(token.dateTo)) {
            throw new IllegalArgumentException("Neplatný interval ISO datumů: od > do");
        }
    }

    /**
     * Splits an interval written with "-" and no spaces into its two parts. A leading "-" is the
     * sign of the first year, "--" the delimiter before a negative second year:
     * {@code 1900-1912}, {@code -7-2}, {@code -7--2}, {@code -1.3.7--14.10.2}.
     *
     * @return the two parts, null when the text contains no delimiter
     * @throws IllegalStateException
     *             more or fewer than two parts
     */
    private static String[] splitInterval(final String input) {
        if (!input.contains(DEFAULT_INTERVAL_DELIMITER)) {
            return null;
        }
        String delimiter = SECOND_YEAR_IS_NEGATIVE;
        if (!input.contains(SECOND_YEAR_IS_NEGATIVE)) {
            if (!input.startsWith(DEFAULT_INTERVAL_DELIMITER)) {
                String[] parts = input.split(DEFAULT_INTERVAL_DELIMITER);
                if (parts.length != 2) {
                    throw new IllegalStateException("Neplatný interval: " + input);
                }
                return parts;
            }
            // -datum-datum
            delimiter = DEFAULT_INTERVAL_DELIMITER;
        }
        // [-]datum--datum
        int position = input.indexOf(delimiter, 1);
        if (position < 0) {
            throw new IllegalStateException("Neplatný interval: " + input);
        }
        return new String[] { input.substring(0, position), input.substring(position + 1) };
    }

    /**
     * Text of a unit date in the default language of the lexicon - the form stored in indexes and
     * titles.
     *
     * @param unitdate
     * @return String
     */
    public static String convertToString(final IUnitdate unitdate) {
        return convertToString(unitdate, LEXICON.defaultLanguage());
    }

    /**
     * Text of a unit date in a language.
     *
     * @param languageTag
     *            BCP 47 tag of the language; null or unknown gives the default language
     */
    public static String convertToString(final IUnitdate unitdate, final String languageTag) {
        return convertToString(unitdate, LEXICON.language(languageTag));
    }

    private static String convertToString(final IUnitdate unitdate, final Language language) {
        String format = unitdate.getFormat();

        if (isInterval(format)) {
            return convertInterval(format, unitdate, language);
        }
        return convertToken(format, unitdate.getValueFrom(), unitdate.getValueFromEstimated(), language);
    }

    /**
	 * Begin of interval to string, in the default language
	 *
	 * @param unitdate
	 * @param allowEstimate
     * @return String
	 */
	public static String beginToString(final IUnitdate unitdate, final boolean allowEstimate) {
	    return beginToString(unitdate, allowEstimate, (String) null);
    }

    /**
     * Begin of interval to string, in a language
     */
    public static String beginToString(final IUnitdate unitdate, final boolean allowEstimate,
                                       final String languageTag) {
        String format = unitdate.getFormat();

        if (isInterval(format)) {
            String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
            format = data[0];
        }
        return convertToken(format, unitdate.getValueFrom(), allowEstimate && unitdate.getValueFromEstimated(),
                            LEXICON.language(languageTag));
    }

	/**
	 * End of interval to string, in the default language
	 *
	 * @param unitdate
	 * @return String
	 */
	public static String endToString(final IUnitdate unitdate, final boolean allowEstimate) {
	    return endToString(unitdate, allowEstimate, (String) null);
	}

    /**
     * End of interval to string, in a language
     */
    public static String endToString(final IUnitdate unitdate, final boolean allowEstimate,
                                     final String languageTag) {
        String format = unitdate.getFormat();

        if (isInterval(format)) {
            String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);
            format = data[1];
        }
        return convertToken(format, unitdate.getValueTo(), allowEstimate && unitdate.getValueToEstimated(),
                            LEXICON.language(languageTag));
    }

	/**
	 * Konverze intervalu.
	 *
	 * @param format   vstupní formát
	 * @param unitdate doplňovaný objekt
	 * @return výsledný řetězec
	 */
    private static String convertInterval(final String format, final IUnitdate unitdate, final Language language) {

        String[] data = format.split(DEFAULT_INTERVAL_DELIMITER);

        if (data.length != 2) {
            throw new IllegalStateException("Neplatný interval: " + format);
        }

        boolean bothEstimate = BooleanUtils.isTrue(unitdate.getValueFromEstimated()) && BooleanUtils.isTrue(unitdate.getValueToEstimated());

        String template = language.template(bothEstimate ? "estimateInterval" : "interval");
        String dateFrom = convertToken(data[0], unitdate.getValueFrom(),
                                       !bothEstimate && BooleanUtils.isTrue(unitdate.getValueFromEstimated()), language);
        String dateTo = convertToken(data[1], unitdate.getValueTo(),
                                     !bothEstimate && BooleanUtils.isTrue(unitdate.getValueToEstimated()), language);

        return render(template, Map.of("from", dateFrom, "to", dateTo));
    }

    /**
     * Konverze tokenu - výrazu.
     *
     * @param format        vstupní formát
     * @param srcValue		zdrojový řetězec
     * @param estimated     the part is an estimate
     * @param language      language of the text
     * @return výsledný řetězec
     */
    private static String convertToken(final String format, final String srcValue, final boolean estimated,
                                       final Language language) {

    	// remove extra whitespaces
    	String value = srcValue.trim();

        LocalDateTime date;
        try {
            date = LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Chyba při analýze datum: " + value, e);
        }

        int year = date.getYear();
        // ISO year 0 is 1 BC, -99 is 100 BC
        boolean bc = year <= 0;
        if (bc) {
            year = -(year - 1);
        }
        int century = (year + 99) / 100;

        Map<String, String> values = new HashMap<>();
        values.put("y", String.valueOf(year));
        values.put("c", String.valueOf(century));
        values.put("ord", ordinalSuffix(century));
        values.put("d", String.valueOf(date.getDayOfMonth()));
        values.put("M", String.valueOf(date.getMonthValue()));
        values.put("MMM", language.monthName(date.getMonthValue()));
        values.put("H", String.valueOf(date.getHour()));
        values.put("mm", String.format("%02d", date.getMinute()));
        values.put("ss", String.format("%02d", date.getSecond()));

        String result;
        switch (format) {
            case CENTURY:
                result = render(language.template(bc ? "centuryBc" : "century"), values);
                break;
            case YEAR:
                result = render(language.template(bc ? "yearBc" : "year"), values);
                break;
            case YEAR_MONTH:
                result = render(language.template("yearMonth"), values) + (bc ? language.template("bcSuffix") : "");
                break;
            case DATE:
                result = render(language.template("date"), values) + (bc ? language.template("bcSuffix") : "");
                break;
            case DATE_TIME:
                result = render(language.template("dateTime"), values) + (bc ? language.template("bcSuffix") : "");
                break;
            default:
                throw new IllegalStateException("Neexistující formát: " + format);
        }

        if (estimated) {
            result = render(language.template("estimate"), Map.of("x", result));
        }

        return result;
    }

    /**
     * Fills {@code {placeholder}}s of a template.
     */
    private static String render(final String template, final Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * English ordinal suffix: 1st, 2nd, 3rd, 4th, 11th, 12th, 13th, 21st.
     */
    static String ordinalSuffix(final int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return "th";
        }
        switch (n % 10) {
        case 1:
            return "st";
        case 2:
            return "nd";
        case 3:
            return "rd";
        default:
            return "th";
        }
    }

    /**
     * Year of one end of a unit date as text in the default language ("1968", "31 př. n. l.").
     *
     * @param unitdate doplňovaný objekt
     * @param first zda-li se jedná o první datum
     * @return výsledný řetězec
     */
    public static String convertYear(final IUnitdate unitdate, final boolean first) {
        LocalDateTime date = getLocalDateTimeFromUnitDate(unitdate, first);
        if (date != null) {
            Language language = LEXICON.defaultLanguage();
            int year = date.getYear();
            if (year <= 0) {
                return render(language.template("yearBc"), Map.of("y", String.valueOf(-(year - 1))));
            }
            return render(language.template("year"), Map.of("y", String.valueOf(year)));
        }
        return unitdate.getFormat();
    }

    /**
     * Získání LocalDateTime z objektu IUnitdate.
     *
     * @param unitdate
     * @param from
     * @return LocalDateTime
     */
    public static LocalDateTime getLocalDateTimeFromUnitDate(final IUnitdate unitdate, final boolean from) {
    	// Value from and to might contain additional spaces from db
    	// and have to be trimmed.
        if (from) {
            if (unitdate.getValueFrom() != null) {
                return LocalDateTime.parse(unitdate.getValueFrom().trim());
            }
        } else {
            if (unitdate.getValueTo() != null) {
                return LocalDateTime.parse(unitdate.getValueTo().trim());
            }
        }
        return null;
    }

	public static LocalDateTime getLocalDateTimeFromUnitDate(IUnitdate unitDate, boolean from,
			FormatType format) {
		LocalDateTime ldt = getLocalDateTimeFromUnitDate(unitDate, from);
		switch(format) {
		case C:
			boolean ad = ldt.getYear()>0;
			if(from) {
				// e.g. 1848 -> 1801, 1801 -> 1801, 1800 -> 1701
				//         0 -> -99, -1 -> -99, -100 -> -199
				int expYear = ad?((ldt.getYear()-1)/100*100+1):(ldt.getYear()/100*100-99);
				if( ldt.getYear()==expYear && ldt.getMonthValue()==1 && ldt.getDayOfMonth()==1&&ldt.getHour()==0&&ldt.getMinute()==0&&ldt.getSecond()==0) {
					return ldt;
				}
				return LocalDateTime.of(LocalDate.of(expYear, 1, 1), LocalTime.of(0, 0, 0));
			} else {
				// e.g. 1848 -> 1900, 1801 -> 1900, 1800 -> 1800
				//         0 -> 0, -99 -> 0, -100 -> -100
				int expYear = ad?((ldt.getYear()+99)/100*100):(ldt.getYear()/100*100);
				if(ldt.getYear()==expYear && ldt.getMonthValue()==12&&ldt.getDayOfMonth()==31&&ldt.getHour()==23&&ldt.getMinute()==59&&ldt.getSecond()==59) {
					return ldt;
				}
				return LocalDateTime.of(LocalDate.of(expYear, 12, 31), LocalTime.of(23, 59, 59));
			}
		case Y:
			if(from) {
				if(ldt.getMonthValue()==1 && ldt.getDayOfMonth()==1&&ldt.getHour()==0&&ldt.getMinute()==0&&ldt.getSecond()==0) {
					return ldt;
				}
				return LocalDateTime.of(LocalDate.of(ldt.getYear(), 1, 1), LocalTime.of(0, 0, 0));
			} else {
				if(ldt.getMonthValue()==12&&ldt.getDayOfMonth()==31&&ldt.getHour()==23&&ldt.getMinute()==59&&ldt.getSecond()==59) {
					return ldt;
				}
				return LocalDateTime.of(LocalDate.of(ldt.getYear(), 12, 31), LocalTime.of(23, 59, 59));
			}
		case YM:
			if(from) {
				if(ldt.getDayOfMonth()==1&&ldt.getHour()==0&&ldt.getMinute()==0&&ldt.getSecond()==0) {
					return ldt;
				}
				return LocalDateTime.of(LocalDate.of(ldt.getYear(), ldt.getMonthValue(), 1), LocalTime.of(0, 0, 0));
			} else {
				// end of month
				LocalDateTime ldtIncremented = ldt.plusDays(1);
				if(ldtIncremented.getDayOfMonth()==1&&ldt.getHour()==23&&ldt.getMinute()==59&&ldt.getSecond()==59) {
					return ldt;
				}
				int year = ldt.getYear();
				int month = ldt.getMonthValue()+1;
				if(month>12) {
					year++;
					month = 1;
				}
				ldtIncremented = LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.of(23, 59, 59));
				return ldtIncremented.minusDays(1);
			}
		case D:
			if(from) {
				if(ldt.getHour()==0&&ldt.getMinute()==0&&ldt.getSecond()==0) {
					return ldt;
				}
				return LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(0, 0, 0));
			} else {
				if(ldt.getHour()==23&&ldt.getMinute()==59&&ldt.getSecond()==59) {
					return ldt;
				}
				return LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(23, 59, 59));
			}
		case DT:
			// full date time -> nothing to change
			return ldt;
		default:
			throw new IllegalStateException("Unexpected format: "+format);
		}
	}


    /**
     * Parsování tokenu.
     *
     * @param tokenString výraz
     * @param type        position of the part in the text
     * @return výsledný token
     * @throws IllegalArgumentException
     *             no form matches
     */
    private static Token parseToken(final String tokenString, final TokenType type) {
        String token = tokenString == null ? null : tokenString.trim();
        if (StringUtils.isEmpty(token)) {
            throw new IllegalArgumentException("Nemůže existovat prázdný interval");
        }

        boolean estimate = false;
        if (token.charAt(0) == '[' && token.charAt(token.length() - 1) == ']') {
            token = token.substring(1, token.length() - 1);
            estimate = true;
        }
        boolean negative = false;
        if (token.startsWith(DEFAULT_INTERVAL_DELIMITER)) {
            token = token.substring(1);
            negative = true;
        }

        // try to parse date using available forms
        for (Form form : FORMS) {
            Matcher matcher = form.pattern.matcher(token);
            if (matcher.matches()) {
                return form.parse(matcher, estimate, negative, type);
            }
        }
        throw new IllegalArgumentException("Nepodporovaný výraz: " + tokenString);
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
        normalize(unitDate);

        return unitDate;
    }

    /**
     * Return format of given part
     * @param unitDate
     * @param from
     * @return
     */
	public static String getFormatOf(IUnitdate unitDate, boolean from) {
		String fullFormat = unitDate.getFormat();
		if(!isInterval(unitDate)) {
			return fullFormat;
		}
		String fullFormats[] = fullFormat.split(UnitDateConverterConsts.FORMAT_DELIMITER);
		return from? fullFormats[0]: fullFormats[1];
	}

	/**
	 * Select less accurate format from two
	 * @param firstFormat
	 * @param secondFormat
	 * @return
	 */
	public static FormatType getLessAccurateFormat(String firstFormat, String secondFormat) {
		FormatType first = FormatType.valueOf(firstFormat);
		FormatType second = FormatType.valueOf(secondFormat);

		return (first.compareTo(second)<0)?first:second;
	}

	/**
	 * Compare two unitdates
	 * @param firstDate
	 * @param firstFrom
	 * @param secondDate
	 * @param secondFrom
	 * @return Return -1 if first date is before second date. Return 0 if both dates are same.
	 * Return 1 if second date is before first date.
	 */
    public static int compare(IUnitdate firstDate, boolean firstFrom, IUnitdate secondDate, boolean secondFrom) {
    	// make both dates comparable by converting to same accuracy
    	String firstFormat = UnitDateConverter.getFormatOf(firstDate, firstFrom);
    	String secondFormat = UnitDateConverter.getFormatOf(secondDate, secondFrom);

    	FormatType format = UnitDateConverter.getLessAccurateFormat(firstFormat, secondFormat);

    	LocalDateTime first = UnitDateConverter.getLocalDateTimeFromUnitDate(firstDate, firstFrom, format);
    	LocalDateTime second = UnitDateConverter.getLocalDateTimeFromUnitDate(secondDate, secondFrom, format);

    	int result = first.compareTo(second);
    	if(result<0) {
    		return -1;
    	}
    	if(result>0) {
    		return 1;
    	}
    	return 0;
	}
}
