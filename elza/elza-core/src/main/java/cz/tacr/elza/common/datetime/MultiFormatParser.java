package cz.tacr.elza.common.datetime;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class MultiFormatParser {
    List<DateTimeFormatter> formatters = new ArrayList<>();

    public MultiFormatParser appendFormat(DateTimeFormatter formatter) {
        formatters.add(formatter);
        return this;
    }

    public LocalDate parseDate(String srcValue, LocalDate defaultValue) {
        for(DateTimeFormatter formatter: formatters) {
            try {
                LocalDate result = LocalDate.parse(srcValue, formatter);
                if(result!=null) {
                    return result; 
                }
            } catch (DateTimeParseException e) {
                // ignore this exception and jump to next converter
            }
        }
        return defaultValue;
    }
}
