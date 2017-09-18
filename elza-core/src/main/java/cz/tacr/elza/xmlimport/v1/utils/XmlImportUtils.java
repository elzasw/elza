package cz.tacr.elza.xmlimport.v1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;

/**
 * Pomocné meotdy pro xml import.
 */
public final class XmlImportUtils {

    private final static SimpleDateFormat FORMATTER_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private XmlImportUtils() {
    }

    public static String dateToString(final Date date) {
        if (date == null) {
            return null;
        }

        return FORMATTER_DATE_TIME.format(date);
    }

    public static Date stringToDate(final String stringDate) {
        if (StringUtils.isBlank(stringDate)) {
            return null;
        }

        try {
            return FORMATTER_DATE_TIME.parse(stringDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("3patný formát datumu " + stringDate);
        }
    }

    public static ComplexDate createComplexDate(final ParUnitdate parUnitdate) {
        if (parUnitdate == null) {
            return null;
        }

        ComplexDate complexDate = new ComplexDate();

        complexDate.setSpecificDateFrom(stringToDate(parUnitdate.getValueFrom()));
        complexDate.setSpecificDateTo(stringToDate(parUnitdate.getValueTo()));
        complexDate.setTextDate(parUnitdate.getTextDate());
        complexDate.setNote(parUnitdate.getNote());

        return complexDate;
    }
}
