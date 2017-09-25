package cz.tacr.elza.deimport;

import cz.tacr.elza.core.data.CalendarType;

public class CalendarTypeConvertor {

    private CalendarTypeConvertor() {
    }

    public static CalendarType convert(String value) {
        if (value == null) {
            return CalendarType.GREGORIAN;
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
