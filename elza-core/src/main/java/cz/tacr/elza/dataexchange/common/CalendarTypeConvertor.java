package cz.tacr.elza.dataexchange.common;

import cz.tacr.elza.core.data.CalendarType;

/**
 * Calendar type converter for data-exchange.
 */
public class CalendarTypeConvertor {

    private CalendarTypeConvertor() {
    }

    public static String convert(CalendarType value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case GREGORIAN:
                return null; // default
            case JULIAN:
                return "J";
        }
        throw new IllegalArgumentException("Uknown calendar type:" + value);
    }

    public static CalendarType convert(String value) {
        if (value == null) {
            return CalendarType.GREGORIAN; // default
        } else if (value.length() == 1) {
            switch (value.charAt(0)) {
                case 'G':
                case 'g':
                    return CalendarType.GREGORIAN;
                case 'J':
                case 'j':
                    return CalendarType.JULIAN;
            }
        }
        throw new IllegalArgumentException("Uknown calendar type:" + value);
    }
}
