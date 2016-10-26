package cz.tacr.elza.xmlimport.v1.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.domain.ParUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.service.exception.InvalidDataException;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;

/**
 * Pomocné meotdy pro xml import.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 29. 1. 2016
 */
public final class XmlImportUtils {

  private final static SimpleDateFormat FORMATTER_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private XmlImportUtils() {

    }

    public static String trimStringValue(final String text, final int length, final boolean stopOnError) throws InvalidDataException {
        if (StringUtils.isEmpty(text)) {
            return text;
        }

        if (text.length() > length) {
            if (stopOnError) {
                throw new InvalidDataException("Hodnota " + text + " je delší než povolená délka " + length);
            }

            return text.substring(0, length);
        }

        return text;
    }

    public static ParUnitdate convertComplexDateToUnitdate(final ComplexDate complexDate) throws InvalidDataException {
        if (complexDate == null) {
            return null;
        }

        XmlImportUtils.checkComplexDate(complexDate);

        Date specificDate = complexDate.getSpecificDate();
        Date specificDateFrom = complexDate.getSpecificDateFrom();
        Date specificDateTo = complexDate.getSpecificDateTo();
        String textDate = complexDate.getTextDate();


        ParUnitdate unitdate = new ParUnitdate();
        if (StringUtils.isNotBlank(textDate)) {
            try {
                UnitDateConvertor.convertToUnitDate(textDate, unitdate);
            } catch (Exception e) {
                unitdate.setTextDate(textDate);
            }
        } else if (specificDate != null) {
            unitdate.setValueFrom(FORMATTER_DATE_TIME.format(specificDateFrom));
            unitdate.formatAppend(UnitDateConvertor.DATE_TIME);
            unitdate.formatAppend(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER);
        } else {
            if (specificDateFrom != null) {
                unitdate.setValueFrom(FORMATTER_DATE_TIME.format(specificDateFrom));
                unitdate.formatAppend(UnitDateConvertor.DATE_TIME);
            }

            unitdate.formatAppend(UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER);

            if (specificDateTo != null) {
                unitdate.setValueTo(FORMATTER_DATE_TIME.format(specificDateTo));
                unitdate.formatAppend(UnitDateConvertor.DATE_TIME);
            }
        }

        return unitdate;
    }

    /**
     * Kontrola vyplnění správné kombinace atributů.
     *
     * @throws InvalidDataException pokud nenjsou správně vyplněny atributy
     */
    public static void checkComplexDate(final ComplexDate complexDate) throws InvalidDataException {
        if (!(onlyDate(complexDate) || onlyInterval(complexDate) || onlyTextDate(complexDate))) {
            throw new InvalidDataException("Nesprávně vyplněný datum. Musí být vyplněn jen konkrétní datum nebo jen interval nebo jen textové datum.");
        }
    }

    private static boolean onlyTextDate(final ComplexDate complexDate) {
        Date specificDate = complexDate.getSpecificDate();
        Date specificDateFrom = complexDate.getSpecificDateFrom();
        Date specificDateTo = complexDate.getSpecificDateTo();
        String textDate = complexDate.getTextDate();

        if (StringUtils.isNotBlank(textDate) && specificDate == null && specificDateFrom == null && specificDateTo == null) {
            return true;
        }

        return false;
    }

    private static boolean onlyInterval(final ComplexDate complexDate) {
        Date specificDate = complexDate.getSpecificDate();
        Date specificDateFrom = complexDate.getSpecificDateFrom();
        Date specificDateTo = complexDate.getSpecificDateTo();
        String textDate = complexDate.getTextDate();

        if ((specificDateFrom != null || specificDateTo != null) && StringUtils.isBlank(textDate) && specificDate == null) {
            return true;
        }

        return false;
    }

    private static boolean onlyDate(final ComplexDate complexDate) {
        Date specificDate = complexDate.getSpecificDate();
        Date specificDateFrom = complexDate.getSpecificDateFrom();
        Date specificDateTo = complexDate.getSpecificDateTo();
        String textDate = complexDate.getTextDate();

        if (specificDate != null && StringUtils.isBlank(textDate) && specificDateFrom == null && specificDateTo == null) {
            return true;
        }

        return false;
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

        complexDate.setSpecificDateFrom(XmlImportUtils.stringToDate(parUnitdate.getValueFrom()));
        complexDate.setSpecificDateTo(XmlImportUtils.stringToDate(parUnitdate.getValueTo()));
        complexDate.setTextDate(parUnitdate.getTextDate());

        return complexDate;
    }
}
