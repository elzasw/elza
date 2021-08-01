package cz.tacr.elza.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;

public class UnitDateConvertorTest {

    private static final String YEAR_INTERVAL = UnitDateConvertor.YEAR + "-" + UnitDateConvertor.YEAR;

    private static final String YEAR_MONTH_INTERVAL = UnitDateConvertor.YEAR_MONTH + "-" + UnitDateConvertor.YEAR_MONTH;

    private static final String DATE_INTERVAL = UnitDateConvertor.DATE + "-" + UnitDateConvertor.DATE;

    private static final String DATE_TIME_INTERVAL = UnitDateConvertor.DATE_TIME + "-" + UnitDateConvertor.DATE_TIME;

    @Test
    public void convertToUnitDateTest() {
        ArrDataUnitdate unitDate;
        String sourceDate;

        sourceDate = "1st";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), UnitDateConvertor.CENTURY);
        assertEquals(unitDate.getValueFrom(), "0001-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0100-12-31T23:59:59");

        sourceDate = "1980-1990";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "1990-12-31T23:59:59");

        sourceDate = "1.1980-2.1990";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_MONTH_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "1990-02-28T23:59:59");

        sourceDate = "1.1.1980-2.2.1990";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "1990-02-02T23:59:59");

        sourceDate = "1.1.1980 10:05-2.2.1990 12:25";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_TIME_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "1980-01-01T10:05:00");
        assertEquals(unitDate.getValueTo(), "1990-02-02T12:25:59");

        sourceDate = "-2";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), UnitDateConvertor.YEAR);
        assertEquals(unitDate.getValueFrom(), "-0002-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0002-12-31T23:59:59");

        sourceDate = "[-2]";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), UnitDateConvertor.YEAR);
        assertTrue(unitDate.getValueFromEstimated());
        assertTrue(unitDate.getValueToEstimated());
        assertEquals(unitDate.getValueFrom(), "-0002-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0002-12-31T23:59:59");

        sourceDate = "-20--2";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0020-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0002-12-31T23:59:59");

        sourceDate = "-1.10";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), UnitDateConvertor.YEAR_MONTH);
        assertEquals(unitDate.getValueFrom(), "-0010-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0010-01-31T23:59:59");

        sourceDate = "-1.10--2.8";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), YEAR_MONTH_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0010-01-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0008-02-29T23:59:59");

        sourceDate = "-1.2.20";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), UnitDateConvertor.DATE);
        assertEquals(unitDate.getValueFrom(), "-0020-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0020-02-01T23:59:59");

        sourceDate = "-1.2.20--2.5.18";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0020-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "-0018-05-02T23:59:59");

        sourceDate = "-1.2.20-2.5.18";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        assertEquals(unitDate.getFormat(), DATE_INTERVAL);
        assertEquals(unitDate.getValueFrom(), "-0020-02-01T00:00:00");
        assertEquals(unitDate.getValueTo(), "0018-05-02T23:59:59");
    }

    @Test
    public void convertToStringTest() {
        ArrDataUnitdate unitDate;
        String sourceDate;
        String result;

        sourceDate = "1st";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, "1. st.");

        sourceDate = "[1900]-[1910]";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, "1900/1910");

        sourceDate = "1.1.1980 10:05-2.2.1990 12:25";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, "1.1.1980 10:05:00-2.2.1990 12:25:59");

        sourceDate = "[-2]";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, sourceDate);

        sourceDate = "-1.10--2.8";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, sourceDate);

        sourceDate = "-1.2.20--2.5.18";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, sourceDate);

        sourceDate = "-1.2.20-2.5.18";
        unitDate = UnitDateConvertor.convertToUnitDate(sourceDate, new ArrDataUnitdate());
        result = UnitDateConvertor.convertToString(unitDate);
        assertEquals(result, sourceDate);
    }
}
