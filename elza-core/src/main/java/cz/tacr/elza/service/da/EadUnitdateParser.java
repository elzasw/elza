package cz.tacr.elza.service.da;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.archivists.ead3.schema.Daterange;
import org.archivists.ead3.schema.MDatesingle;
import org.archivists.ead3.schema.Unitdatestructured;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.domain.converter.UnitDateConverter.FormatType;
import cz.tacr.elza.domain.converter.UnitDateConverterConsts;

/**
 * Reads the date of origin of a unit of description written in EAD.
 *
 * The date is read as the profile of EAD for the National Archives describes it
 * (https://stands.nacr.cz/ead/current/prvky-popisu/datace.html), which is also what the EAD
 * export of ELZA writes:
 *
 * <pre>
 * &lt;unitdatestructured&gt;
 *   &lt;daterange altrender="Y-Y"&gt;
 *     &lt;fromdate standarddate="1734-01-01T00:00:00"&gt;1734&lt;/fromdate&gt;
 *     &lt;todate standarddate="1776-12-31T23:59:59"&gt;1776&lt;/todate&gt;
 *   &lt;/daterange&gt;
 * &lt;/unitdatestructured&gt;
 * </pre>
 *
 * The format in {@code altrender} is taken over as it is - it is the format of the unit date of
 * ELZA - and the machine readable values are taken from {@code standarddate}, or from
 * {@code notbefore} / {@code notafter} for an estimate. The text inside the elements is only the
 * readable form and is not interpreted.
 *
 * Only a little is tolerated: the format may be written in lower case, and a value may be a
 * bare date or, at the end of the interval, the midnight of the right day, because those can be
 * read in only one way. Anything that could be read in more than one way - a missing format, a
 * value that does not fall on the boundary of the period its format names, a single period
 * whose ends do not belong to it - is refused, so that the date shown in ELZA is the date the
 * author of the package wrote.
 */
final class EadUnitdateParser {

    private static final String ESTIMATE_FROM = "notbefore";
    private static final String ESTIMATE_TO = "notafter";
    private static final String STANDARD_DATE = "standarddate";

    private static final String GREGORIAN = "gregorian";

    /** Astronomical year (year 0 is 1 BC), month, day and optionally the time, without a zone. */
    private static final Pattern ISO_VALUE = Pattern.compile("-?\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2})?");

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    /** The form ELZA stores the values of a unit date in. */
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private EadUnitdateParser() {
    }

    /**
     * @return the unit date, normalized; its data type is left to the caller
     * @throws EadContentException when the date is not written the way it can be read unambiguously
     */
    static ArrDataUnitdate parse(Unitdatestructured unitdate) {
        String calendar = StringUtils.trimToNull(unitdate.getCalendar());
        if (calendar != null && !GREGORIAN.equalsIgnoreCase(calendar)) {
            throw new EadContentException("kalendář '" + calendar
                    + "' není podporován, datace se zapisuje v gregoriánském kalendáři");
        }
        Daterange range = unitdate.getDaterange();
        if (range == null) {
            throw new EadContentException("datace se zapisuje jako interval v elementu <daterange>"
                    + (unitdate.getDatesingle() != null || unitdate.getDateset() != null
                            ? ", elementy <datesingle> a <dateset> nejsou přípustné"
                            : ", element <unitdatestructured> jej neobsahuje"));
        }
        return parse(range);
    }

    private static ArrDataUnitdate parse(Daterange range) {
        String format = StringUtils.trimToNull(range.getAltrender());
        if (format == null) {
            throw new EadContentException("element <daterange> nemá povinný atribut altrender s formátem datace"
                    + " (C, Y, YM, D, DT, případně interval, např. Y-Y)");
        }
        FormatType[] formats = parseFormat(format);
        if (range.getFromdate() == null || range.getTodate() == null) {
            throw new EadContentException("element <daterange> musí obsahovat oba elementy <fromdate> i <todate>");
        }

        Boundary from = readBoundary(range.getFromdate(), true, formats[0]);
        Boundary to = readBoundary(range.getTodate(), false, formats[formats.length - 1]);

        if (from.value().isAfter(to.value())) {
            throw new EadContentException("začátek intervalu " + FORMATTER_ISO.format(from.value())
                    + " je pozdější než jeho konec " + FORMATTER_ISO.format(to.value()));
        }
        if (formats.length == 1) {
            // A single period: ELZA shows only its beginning, so the end has to close that
            // same period or the date shown would not be the date written.
            LocalDateTime endOfPeriod = alignTo(from.value(), formats[0], false);
            if (!endOfPeriod.equals(to.value())) {
                throw new EadContentException("formát '" + format + "' označuje jediné období, ale <fromdate> ("
                        + FORMATTER_ISO.format(from.value()) + ") a <todate> ("
                        + FORMATTER_ISO.format(to.value()) + ") do stejného období nepatří;"
                        + " interval se zapisuje formátem ve tvaru " + formats[0] + "-" + formats[0]);
            }
        }

        ArrDataUnitdate result = new ArrDataUnitdate();
        result.setFormat(formats.length == 1
                ? formats[0].name()
                : formats[0].name() + UnitDateConverterConsts.FORMAT_DELIMITER + formats[1].name());
        result.setValueFrom(FORMATTER_ISO.format(from.value()));
        result.setValueFromEstimated(from.estimated());
        result.setValueTo(FORMATTER_ISO.format(to.value()));
        result.setValueToEstimated(to.estimated());
        UnitDateConverter.normalize(result);
        return result;
    }

    private static FormatType[] parseFormat(String format) {
        String[] parts = format.split(UnitDateConverterConsts.FORMAT_DELIMITER, -1);
        if (parts.length > 2) {
            throw invalidFormat(format);
        }
        FormatType[] result = new FormatType[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = FormatType.valueOf(parts[i].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw invalidFormat(format);
            }
        }
        return result;
    }

    private static EadContentException invalidFormat(String format) {
        return new EadContentException("formát datace '" + format + "' v atributu altrender není platný,"
                + " přípustné jsou hodnoty C, Y, YM, D, DT nebo jejich interval, např. Y-Y");
    }

    /** One end of the interval. */
    private record Boundary(LocalDateTime value, boolean estimated) {
    }

    /**
     * The beginning is given by {@code standarddate}, or {@code notbefore} when estimated; the end
     * by {@code standarddate}, or {@code notafter} when estimated. The other estimate attribute
     * and both attributes at once are refused - which of the values was meant cannot be told.
     */
    private static Boundary readBoundary(MDatesingle date, boolean from, FormatType format) {
        String element = from ? "<fromdate>" : "<todate>";
        String estimateAttr = from ? ESTIMATE_FROM : ESTIMATE_TO;
        String misplacedAttr = from ? ESTIMATE_TO : ESTIMATE_FROM;

        String standard = StringUtils.trimToNull(date.getStandarddate());
        String estimate = StringUtils.trimToNull(from ? date.getNotbefore() : date.getNotafter());
        String misplaced = StringUtils.trimToNull(from ? date.getNotafter() : date.getNotbefore());

        if (misplaced != null) {
            throw new EadContentException("atribut " + misplacedAttr + " u elementu " + element
                    + " není přípustný, odhad se u něj zapisuje do atributu " + estimateAttr);
        }
        if (standard != null && estimate != null) {
            throw new EadContentException("element " + element + " uvádí současně atributy " + STANDARD_DATE
                    + " i " + estimateAttr + ", použije se jen jeden z nich");
        }
        if (standard == null && estimate == null) {
            throw new EadContentException("element " + element + " nemá strojovou podobu data v atributu "
                    + STANDARD_DATE + " (u odhadu " + estimateAttr + ")");
        }
        String attr = standard != null ? STANDARD_DATE : estimateAttr;
        String raw = standard != null ? standard : estimate;
        LocalDateTime value = parseValue(raw, from, element, attr);

        LocalDateTime aligned = alignTo(value, format, from);
        if (!aligned.equals(value)) {
            // The end of a period written as the midnight of its last day is still that day.
            boolean midnightOfLastDay = !from && format != FormatType.DT
                    && value.toLocalTime().equals(LocalTime.MIDNIGHT)
                    && value.toLocalDate().equals(aligned.toLocalDate());
            if (!midnightOfLastDay) {
                throw new EadContentException("hodnota '" + raw + "' atributu " + attr + " u elementu " + element
                        + " neodpovídá formátu " + format + ", " + (from ? "začátek" : "konec")
                        + " období by byl " + FORMATTER_ISO.format(aligned));
            }
        }
        return new Boundary(aligned, estimate != null);
    }

    /**
     * A bare date stands for the whole day: its beginning at the start of the interval, its end
     * at the end of it.
     */
    private static LocalDateTime parseValue(String raw, boolean from, String element, String attr) {
        Matcher matcher = ISO_VALUE.matcher(raw);
        if (!matcher.matches()) {
            throw new EadContentException("hodnota '" + raw + "' atributu " + attr + " u elementu " + element
                    + " není datum ve tvaru RRRR-MM-DDThh:mm:ss");
        }
        try {
            if (matcher.group(1) != null) {
                return LocalDateTime.parse(raw, FORMATTER_ISO);
            }
            LocalDateTime startOfDay = LocalDateTime.parse(raw + "T00:00:00", FORMATTER_ISO);
            return from ? startOfDay : startOfDay.with(END_OF_DAY);
        } catch (DateTimeParseException e) {
            throw new EadContentException("hodnota '" + raw + "' atributu " + attr + " u elementu " + element
                    + " není existující datum", e);
        }
    }

    /** The beginning or the end of the period of the given format the value falls in. */
    private static LocalDateTime alignTo(LocalDateTime value, FormatType format, boolean from) {
        ArrDataUnitdate probe = new ArrDataUnitdate();
        String iso = FORMATTER_ISO.format(value);
        if (from) {
            probe.setValueFrom(iso);
        } else {
            probe.setValueTo(iso);
        }
        return UnitDateConverter.getLocalDateTimeFromUnitDate(probe, from, format);
    }
}
