package cz.tacr.elza.drools.model;



import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;


public class DrlUtils {

    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static boolean greaterThan(String firstDate, String secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());
        ArrDataUnitdate second = UnitDateConvertor.convertToUnitDate(secondDate, new ArrDataUnitdate());

        LocalDateTime firstTo = LocalDateTime.parse(first.getValueTo(), FORMATTER_ISO);
        LocalDateTime secondTo = LocalDateTime.parse(second.getValueFrom(), FORMATTER_ISO);
        return firstTo.isAfter(secondTo);
    }

    public static boolean greaterThanOrEqualTo(String firstDate, String secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());
        ArrDataUnitdate second = UnitDateConvertor.convertToUnitDate(secondDate, new ArrDataUnitdate());

        LocalDateTime firstTo = LocalDateTime.parse(first.getValueTo(), FORMATTER_ISO);
        LocalDateTime secondTo = LocalDateTime.parse(second.getValueFrom(), FORMATTER_ISO);
        return firstTo.isAfter(secondTo) || firstTo.isEqual(secondTo);
    }

    public static boolean lessThan(String firstDate, String secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());
        ArrDataUnitdate second = UnitDateConvertor.convertToUnitDate(secondDate, new ArrDataUnitdate());

        LocalDateTime firstFrom = LocalDateTime.parse(first.getValueFrom(), FORMATTER_ISO);
        LocalDateTime secondFrom = LocalDateTime.parse(second.getValueTo(), FORMATTER_ISO);
        return firstFrom.isBefore(secondFrom);
    }

    public static boolean lessThanOrEqualTo(String firstDate, String secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());
        ArrDataUnitdate second = UnitDateConvertor.convertToUnitDate(secondDate, new ArrDataUnitdate());

        LocalDateTime firstFrom = LocalDateTime.parse(first.getValueFrom(), FORMATTER_ISO);
        LocalDateTime secondFrom = LocalDateTime.parse(second.getValueTo(), FORMATTER_ISO);
        return firstFrom.isBefore(secondFrom) || firstFrom.isEqual(secondFrom);
    }

    public static boolean greaterThan(String firstDate, LocalDateTime secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());

        LocalDateTime firstTo = LocalDateTime.parse(first.getValueTo(), FORMATTER_ISO);
        return firstTo.isAfter(secondDate);
    }

    public static boolean greaterThanOrEqualTo(String firstDate, LocalDateTime secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());

        LocalDateTime firstTo = LocalDateTime.parse(first.getValueTo(), FORMATTER_ISO);
        return firstTo.isAfter(secondDate) || firstTo.isEqual(secondDate);
    }

    public static boolean lessThan(String firstDate, LocalDateTime secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());

        LocalDateTime firstFrom = LocalDateTime.parse(first.getValueFrom(), FORMATTER_ISO);
        return firstFrom.isBefore(secondDate);
    }

    public static boolean lessThanOrEqualTo(String firstDate, LocalDateTime secondDate) {
        ArrDataUnitdate first = UnitDateConvertor.convertToUnitDate(firstDate, new ArrDataUnitdate());

        LocalDateTime firstFrom = LocalDateTime.parse(first.getValueFrom(), FORMATTER_ISO);
        return firstFrom.isBefore(secondDate) || firstFrom.isEqual(secondDate);
    }

}
