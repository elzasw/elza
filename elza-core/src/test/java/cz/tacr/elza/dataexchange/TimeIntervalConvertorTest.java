package cz.tacr.elza.dataexchange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cz.tacr.elza.dataexchange.common.timeinterval.TimeInterval;

public class TimeIntervalConvertorTest {

    @Test
    public void testNullFormat() {
        cz.tacr.elza.schema.v2.TimeInterval interval = null;
        TimeInterval result;

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("2015");
        interval.setTo("2015-06");
        result = TimeInterval.create(interval);
        assertEquals("Y-YM", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1990-06-16");
        interval.setTo("1995-06-16T16:50:45");
        result = TimeInterval.create(interval);
        assertEquals("D-DT", result.getFormat());
    }

    @Test
    public void testSignificantFields() {
        cz.tacr.elza.schema.v2.TimeInterval interval = null;
        TimeInterval result = null;

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("2011");
        interval.setTo("2011");
        result = TimeInterval.create(interval);
        assertEquals("Y", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("2011");
        interval.setTo("2012");
        result = TimeInterval.create(interval);
        assertEquals("Y-Y", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T00:00:00");
        interval.setTo("1846-10-03T15:12:00");
        result = TimeInterval.create(interval);
        assertEquals("DT-DT", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T15:00:00");
        interval.setTo("1846-10-03T15:00:00");
        result = TimeInterval.create(interval);
        assertEquals("DT", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1990-01");
        interval.setTo("1990-01");
        result = TimeInterval.create(interval);
        assertEquals("YM", result.getFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatFalselyClaimsEquality() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T15:00:00");
        interval.setTo("1846-10-03T15:01:00");
        interval.setFmt("DT");
        TimeInterval.create(interval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerBoundAfterUpperBound() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T15:01:00");
        interval.setTo("1846-10-03T15:00:00");
        TimeInterval.create(interval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatHidePrecision() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T15:12:00");
        interval.setTo("1846-10-03T15:12:00");
        interval.setFmt("D");
        TimeInterval.create(interval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCustomFormat() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1990");
        interval.setTo("1995-06-16");
        interval.setFmt("");
        TimeInterval.create(interval);
    }

    @Test
    public void testValidCustomFormat() {
        cz.tacr.elza.schema.v2.TimeInterval interval = null;
        TimeInterval result = null;

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1990");
        interval.setTo("1995-06");
        interval.setFmt("Y-YM");
        result = TimeInterval.create(interval);
        assertEquals("Y-YM", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1857-06-16");
        interval.setTo("1873-11-23T23:56:01");
        interval.setFmt("D-DT");
        result = TimeInterval.create(interval);
        assertEquals("D-DT", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T00:00:00");
        interval.setTo("1846-10-03T15:12:00");
        interval.setFmt("D-DT");
        result = TimeInterval.create(interval);
        assertEquals("D-DT", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1783-01-01T00:00:00");
        interval.setTo("1783-12-31T23:59:59");
        interval.setFmt("Y");
        result = TimeInterval.create(interval);
        assertEquals("Y", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1846-10-03T15:00:00");
        interval.setTo("1846-10-03T15:00:00");
        interval.setFmt("DT");
        result = TimeInterval.create(interval);
        assertEquals("DT", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1737-01-07T00:00:00");
        interval.setTo("1737-12-31T23:59:59");
        interval.setFmt("D-D");
        result = TimeInterval.create(interval);
        assertEquals("D-D", result.getFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDate() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1990-1-1"); // 1990-01-01
        interval.setTo("1995-06-16");
        TimeInterval.create(interval);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDate() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF(null);
        interval.setTo("1995-06-16");
        TimeInterval.create(interval);
    }

    @Test
    public void testDefaultFieldValues() {
        cz.tacr.elza.schema.v2.TimeInterval interval = null;
        TimeInterval result = null;

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1856");
        interval.setTo("1857-06");
        result = TimeInterval.create(interval);
        assertEquals("1856-01-01T00:00:00", result.getFormattedFrom());
        assertEquals("1857-06-30T23:59:59", result.getFormattedTo());
        assertEquals("Y-YM", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1857-06-16");
        interval.setTo("1873-11-23T23:56:01");
        result = TimeInterval.create(interval);
        assertEquals("1857-06-16T00:00:00", result.getFormattedFrom());
        assertEquals("1873-11-23T23:56:01", result.getFormattedTo());
        assertEquals("D-DT", result.getFormat());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCenturyInvalidFormat() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1701");
        interval.setTo("1900");
        interval.setFmt("C");
        TimeInterval.create(interval);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCenturyInvalidValue() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1811");
        interval.setTo("1900");
        interval.setFmt("C");
        TimeInterval.create(interval);
    }

    @Test
    public void testCenturyFormat() {
        cz.tacr.elza.schema.v2.TimeInterval interval = null;
        TimeInterval result = null;

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1701");
        interval.setTo("1900");
        interval.setFmt("C-C");
        result = TimeInterval.create(interval);
        assertEquals("C-C", result.getFormat());

        interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1801");
        interval.setTo("1900");
        interval.setFmt("C");
        result = TimeInterval.create(interval);
        assertEquals("C", result.getFormat());
    }

    @Test
    public void testTimeWithMilliseconds() {
        String exMsg = null;
        try {
            cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
            interval.setF("1873-11-23T23:56:01.010");
            interval.setTo("1873-11-23T23:56:01.010");
            interval.setFmt("DT");
            TimeInterval.create(interval);
        } catch (IllegalArgumentException e) {
            exMsg = e.getMessage();
        }
        assertEquals("Fractional seconds cannot be converted", exMsg);
    }

    @Test
    public void testTimeWithTimeoffset() {
        String exMsg = null;
        try {
            cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
            interval.setF("1873-11-23T23:56:01+01:00");
            interval.setTo("1873-11-23T23:56:01+01:00");
            interval.setFmt("DT");
            TimeInterval.create(interval);
        } catch (IllegalArgumentException e) {
            exMsg = e.getMessage();
        }
        assertEquals("Cannot create boundary with timezone", exMsg);
    }

    @Test
    public void testOldGregorianDate() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1178-01-01T00:00:00");
        interval.setTo("1178-12-31T23:59:59");
        TimeInterval result = TimeInterval.create(interval);
        assertEquals("1178-01-01T00:00:00", result.getFormattedFrom());
        assertEquals("1178-12-31T23:59:59", result.getFormattedTo());
    }

    @Test
    public void testJulianDate() {
        cz.tacr.elza.schema.v2.TimeInterval interval = new cz.tacr.elza.schema.v2.TimeInterval();
        interval.setF("1178-01-01T00:00:00");
        interval.setTo("1178-12-31T23:59:59");
        interval.setCt("J");
        TimeInterval result = TimeInterval.create(interval);
        assertEquals("1178-01-01T00:00:00", result.getFormattedFrom());
        assertEquals("1178-12-31T23:59:59", result.getFormattedTo());
    }
}
