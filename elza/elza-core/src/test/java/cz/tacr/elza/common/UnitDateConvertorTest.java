package cz.tacr.elza.common;

import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.CENTURY;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.DATE_TIME;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR;
import static cz.tacr.elza.domain.converter.UnitDateConverterConsts.YEAR_MONTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;

public class UnitDateConvertorTest {

    private static final String YEAR_INTERVAL = YEAR + "-" + YEAR;

    private static final String YEAR_MONTH_INTERVAL = YEAR_MONTH + "-" + YEAR_MONTH;

    private static final String DATE_INTERVAL = DATE + "-" + DATE;

    private static final String DATE_TIME_INTERVAL = DATE_TIME + "-" + DATE_TIME;

    @Test
    public void convertToUnitDateTest() {
        ArrDataUnitdate unitDate;
        String sourceDate;

        sourceDate = "-1st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), CENTURY);
        assertEquals(unitDate.getValueFrom(), "-0099-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0000-12-31T23:59:59");

        sourceDate = "-1.st.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), CENTURY);
        assertEquals(unitDate.getValueFrom(), "-0099-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0000-12-31T23:59:59");

        sourceDate = "1. st. př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), CENTURY);
        assertEquals(unitDate.getValueFrom(), "-0099-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0000-12-31T23:59:59");

        sourceDate = "1st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), CENTURY);
        assertEquals(unitDate.getValueFrom(), "0001-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0100-12-31T23:59:59");

        sourceDate = "1.st.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), CENTURY);
        assertEquals(unitDate.getValueFrom(), "0001-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0100-12-31T23:59:59");

        sourceDate = "1980-1990";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "1990-12-31T23:59:59");

        sourceDate = "1.1980-2.1990";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_MONTH_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "1990-02-28T23:59:59");

        sourceDate = "1.1.1980-2.2.1990";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(DATE_INTERVAL, unitDate.getFormat());
        assertEquals("1980-01-01T00:00:00", unitDate.getValueFrom());
        assertEquals("1990-02-02T23:59:59", unitDate.getValueTo());

        sourceDate = "1.1.1980 10:05";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(DATE_TIME_INTERVAL, unitDate.getFormat());
        assertEquals("1980-01-01T10:05:00", unitDate.getValueFrom());
        assertEquals("1980-01-01T10:05:59", unitDate.getValueTo());

        sourceDate = "1.3.325 10:05 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(DATE_TIME_INTERVAL, unitDate.getFormat());
        assertEquals("-0324-03-01T10:05:00", unitDate.getValueFrom());
        assertEquals("-0324-03-01T10:05:59", unitDate.getValueTo());

        sourceDate = "1.3.325 10:05:47 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(DATE_TIME, unitDate.getFormat());
        assertEquals("-0324-03-01T10:05:47", unitDate.getValueFrom());
        assertEquals("-0324-03-01T10:05:47", unitDate.getValueTo());

        sourceDate = "1.1.1980 10:05-2.2.1990 12:25";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(DATE_TIME_INTERVAL, unitDate.getFormat());
        assertEquals("1980-01-01T10:05:00", unitDate.getValueFrom());
        assertEquals("1990-02-02T12:25:59", unitDate.getValueTo());

        sourceDate = "-1";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(YEAR, unitDate.getFormat());
        assertEquals("0000-01-01T00:00:00", unitDate.getValueFrom());
        assertEquals("0000-12-31T23:59:59", unitDate.getValueTo());

        sourceDate = "-3";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(YEAR, unitDate.getFormat());
        assertEquals("-0002-01-01T00:00:00", unitDate.getValueFrom());
        assertEquals("-0002-12-31T23:59:59", unitDate.getValueTo());

        sourceDate = "[-3]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(YEAR, unitDate.getFormat());
        assertTrue(unitDate.getValueFromEstimated());
        assertTrue(unitDate.getValueToEstimated());
        assertEquals(unitDate.getValueFrom(), "-0002-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0002-12-31T23:59:59");
        
        sourceDate = "31 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR);
        assertEquals(unitDate.getValueFrom(), "-0030-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0030-12-31T23:59:59");

        sourceDate = "-20--2";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0019-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0001-12-31T23:59:59");

        sourceDate = "-1.10";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_MONTH);
        assertEquals(unitDate.getValueFrom(), "-0009-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0009-01-31T23:59:59");

        sourceDate = "-1.10--2.8";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_MONTH_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0009-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0007-02-28T23:59:59");
        
        sourceDate = "1.8.31 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE);
        assertEquals(unitDate.getValueFrom(), "-0030-08-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0030-08-01T23:59:59");

        sourceDate = "-1.2.20";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE);
        assertEquals(unitDate.getValueFrom(), "-0019-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0019-02-01T23:59:59");

        sourceDate = "-1.2.20--2.5.18";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0019-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0017-05-02T23:59:59");

        sourceDate = "-1.2.20-2.5.18";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0019-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0018-05-02T23:59:59");
    }

    @Test
    public void convertToStringTest() {
        ArrDataUnitdate unitDate;
        String sourceDate;
        String result;
        
        sourceDate = "-1st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1. st. př. n. l.", result);

        sourceDate = "1st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1. st.", result);

        sourceDate = "(1st)";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("[1. st.]", result);

        sourceDate = "17st-20st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("17. st.-20. st.", result);

        sourceDate = "17st/20st";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("17. st./20. st.", result);

        sourceDate = "[1900]-[1910]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1900/1910", result);

        sourceDate = "1.1.1980 10:05-2.2.1990 12:25";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1.1.1980 10:05:00-2.2.1990 12:25:59", result);

        sourceDate = "[1 př. n. l.]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("[1 př. n. l.]", result);

        sourceDate = "[2 př. n. l.]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("[2 př. n. l.]", result);

        sourceDate = "[-2]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("[2 př. n. l.]", result);

        sourceDate = "-1.10--2.8";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1.10 př. n. l.-2.8 př. n. l.", result);

        sourceDate = "1.10 př. n. l.-2.8 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1.10 př. n. l.-2.8 př. n. l.", result);

        sourceDate = "-1.2.20--2.5.18";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1.2.20 př. n. l.-2.5.18 př. n. l.", result);

        sourceDate = "-1.2.20-2.5.18";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("1.2.20 př. n. l.-2.5.18", result);

        sourceDate = "[-10.5.2 12:25:00]";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("[10.5.2 12:25:00 př. n. l.]", result);

        sourceDate = "3.1.10 př.n.l.-6.2.8 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("3.1.10 př. n. l.-6.2.8 př. n. l.", result);

        sourceDate = "3.1.10 10:38:41 př.n.l.-6.2.8 15:21:43 př. n. l.";
        unitDate = UnitDateConverter.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConverter.convertToString(unitDate);
        assertEquals("3.1.10 10:38:41 př. n. l.-6.2.8 15:21:43 př. n. l.", result);
    }
}
