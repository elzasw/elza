package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.archivists.ead3.schema.Daterange;
import org.archivists.ead3.schema.Datesingle;
import org.archivists.ead3.schema.Fromdate;
import org.archivists.ead3.schema.Todate;
import org.archivists.ead3.schema.Unitdatestructured;
import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class EadUnitdateParserTest {

    @Test
    void yearInterval_asInTheSpecification() {
        ArrDataUnitdate d = parse("Y-Y", std("1734-01-01T00:00:00"), std("1776-12-31T23:59:59"));

        assertEquals("Y-Y", d.getFormat());
        assertEquals("1734-01-01T00:00:00", d.getValueFrom());
        assertEquals("1776-12-31T23:59:59", d.getValueTo());
        assertFalse(d.getValueFromEstimated());
        assertFalse(d.getValueToEstimated());
        assertEquals("1734-1776", UnitDateConverter.convertToString(d));
    }

    @Test
    void singleYear() {
        ArrDataUnitdate d = parse("Y", std("1958-01-01T00:00:00"), std("1958-12-31T23:59:59"));

        assertEquals("Y", d.getFormat());
        assertEquals("1958", UnitDateConverter.convertToString(d));
    }

    /** The package of the reported failure: a year written with the midnight of its last day. */
    @Test
    void endAtMidnightOfTheLastDay_isThatDay() {
        ArrDataUnitdate d = parse("Y", std("2017-01-01T00:00:00"), std("2017-12-31T00:00:00"));

        assertEquals("2017-12-31T23:59:59", d.getValueTo());
    }

    @Test
    void bareDates_standForTheWholeDay() {
        ArrDataUnitdate d = parse("D", std("2001-10-01"), std("2001-10-01"));

        assertEquals("2001-10-01T00:00:00", d.getValueFrom());
        assertEquals("2001-10-01T23:59:59", d.getValueTo());
        assertEquals("1.10.2001", UnitDateConverter.convertToString(d));
    }

    @Test
    void mixedFormats() {
        ArrDataUnitdate d = parse("YM-D", std("1920-05-01T00:00:00"), std("1921-02-14T23:59:59"));

        assertEquals("YM-D", d.getFormat());
        assertEquals("1920-05-01T00:00:00", d.getValueFrom());
        assertEquals("1921-02-14T23:59:59", d.getValueTo());
    }

    @Test
    void century() {
        ArrDataUnitdate d = parse("C", std("1801-01-01T00:00:00"), std("1900-12-31T23:59:59"));

        assertEquals("19. st.", UnitDateConverter.convertToString(d));
    }

    @Test
    void dateTime_keepsTheTime() {
        ArrDataUnitdate d = parse("DT-DT", std("1945-05-08T14:30:00"), std("1945-05-09T08:15:10"));

        assertEquals("1945-05-08T14:30:00", d.getValueFrom());
        assertEquals("1945-05-09T08:15:10", d.getValueTo());
    }

    @Test
    void lowerCaseFormat_isTolerated() {
        assertEquals("Y-Y", parse(" y-y ", std("1734-01-01T00:00:00"), std("1776-12-31T23:59:59")).getFormat());
    }

    @Test
    void estimate() {
        Fromdate from = new Fromdate();
        from.setNotbefore("1734-01-01T00:00:00");
        Todate to = new Todate();
        to.setNotafter("1776-12-31T23:59:59");

        ArrDataUnitdate d = parse(range("Y-Y", from, to));

        assertTrue(d.getValueFromEstimated());
        assertTrue(d.getValueToEstimated());
        assertEquals("1734/1776", UnitDateConverter.convertToString(d));
    }

    @Test
    void beforeChrist_astronomicalYear() {
        // year 0 is 1 BC
        ArrDataUnitdate d = parse("Y", std("-0043-01-01T00:00:00"), std("-0043-12-31T23:59:59"));

        assertEquals("-0043-01-01T00:00:00", d.getValueFrom());
        assertEquals("44 př. n. l.", UnitDateConverter.convertToString(d));
    }

    @Test
    void missingFormat_isRefused() {
        assertRefused(null, std("2017-01-01T00:00:00"), std("2017-12-31T23:59:59"), "altrender");
    }

    @Test
    void unknownFormat_isRefused() {
        assertRefused("YMD", std("2017-01-01T00:00:00"), std("2017-12-31T23:59:59"), "YMD");
        assertRefused("Y-Y-Y", std("2017-01-01T00:00:00"), std("2017-12-31T23:59:59"), "Y-Y-Y");
    }

    @Test
    void valueOffTheBoundaryOfItsFormat_isRefused() {
        assertRefused("Y", std("2017-03-05T00:00:00"), std("2017-12-31T23:59:59"), "2017-03-05T00:00:00");
        assertRefused("D", std("2017-03-05T12:00:00"), std("2017-03-05T23:59:59"), "2017-03-05T12:00:00");
        assertRefused("Y-Y", std("2017-01-01T00:00:00"), std("2018-06-30T23:59:59"), "2018-06-30T23:59:59");
    }

    @Test
    void singlePeriodSpanningMorePeriods_isRefused() {
        assertRefused("Y", std("1734-01-01T00:00:00"), std("1776-12-31T23:59:59"), "Y-Y");
    }

    @Test
    void reversedInterval_isRefused() {
        assertRefused("Y-Y", std("1776-01-01T00:00:00"), std("1734-12-31T23:59:59"), "pozdější");
    }

    @Test
    void valueThatIsNotAnIsoDate_isRefused() {
        assertRefused("D", std("1.10.2001"), std("2001-10-01T23:59:59"), "1.10.2001");
        assertRefused("D", std("2001-10-01T00:00:00Z"), std("2001-10-01T23:59:59"), "2001-10-01T00:00:00Z");
        assertRefused("D", std("2001-02-30T00:00:00"), std("2001-10-01T23:59:59"), "existující");
    }

    @Test
    void missingMachineReadableValue_isRefused() {
        assertRefused("Y", new Fromdate(), std("2017-12-31T23:59:59"), "standarddate");
    }

    @Test
    void standardDateAndEstimateTogether_isRefused() {
        Fromdate from = std("2017-01-01T00:00:00");
        from.setNotbefore("2017-01-01T00:00:00");
        assertRefused("Y", from, std("2017-12-31T23:59:59"), "notbefore");
    }

    @Test
    void misplacedEstimate_isRefused() {
        Fromdate from = new Fromdate();
        from.setNotafter("2017-01-01T00:00:00");
        assertRefused("Y", from, std("2017-12-31T23:59:59"), "notafter");
    }

    @Test
    void missingEnd_isRefused() {
        assertRefused("Y", std("2017-01-01T00:00:00"), null, "<todate>");
    }

    @Test
    void datesingle_isRefused() {
        Unitdatestructured uds = new Unitdatestructured();
        uds.setDatesingle(new Datesingle());

        EadContentException e = assertThrows(EadContentException.class, () -> EadUnitdateParser.parse(uds));
        assertTrue(e.getMessage().contains("<datesingle>"), e.getMessage());
    }

    @Test
    void otherCalendar_isRefused() {
        Unitdatestructured uds = unitdate(range("Y", std("2017-01-01T00:00:00"), toStd(std("2017-12-31T23:59:59"))));
        uds.setCalendar("julian");

        EadContentException e = assertThrows(EadContentException.class, () -> EadUnitdateParser.parse(uds));
        assertTrue(e.getMessage().contains("julian"), e.getMessage());
    }

    private static <T extends org.archivists.ead3.schema.MDatesingle> T std(T date, String value) {
        date.setStandarddate(value);
        return date;
    }

    private static Fromdate std(String value) {
        return std(new Fromdate(), value);
    }

    private static Todate toStd(Fromdate from) {
        if (from == null) {
            return null;
        }
        Todate to = new Todate();
        to.setStandarddate(from.getStandarddate());
        to.setNotbefore(from.getNotbefore());
        to.setNotafter(from.getNotafter());
        return to;
    }

    private static Daterange range(String format, Fromdate from, Todate to) {
        Daterange range = new Daterange();
        range.setAltrender(format);
        range.setFromdate(from);
        range.setTodate(to);
        return range;
    }

    private static Unitdatestructured unitdate(Daterange range) {
        Unitdatestructured uds = new Unitdatestructured();
        uds.setDaterange(range);
        return uds;
    }

    private static ArrDataUnitdate parse(Daterange range) {
        return EadUnitdateParser.parse(unitdate(range));
    }

    /** The end is given as a {@link Fromdate} only to keep the tests short; it is copied to a {@link Todate}. */
    private static ArrDataUnitdate parse(String format, Fromdate from, Fromdate to) {
        return parse(range(format, from, toStd(to)));
    }

    private static void assertRefused(String format, Fromdate from, Fromdate to, String expectedInMessage) {
        EadContentException e = assertThrows(EadContentException.class, () -> parse(format, from, to));
        assertTrue(e.getMessage().contains(expectedInMessage), e.getMessage());
    }
}
