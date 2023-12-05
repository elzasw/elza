package cz.tacr.elza.schema.support;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    public LocalDate unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        return value != null ? LocalDate.parse(value) : null;
    }

    public String marshal(LocalDate value) {
        return value != null ? DateTimeFormatter.ISO_LOCAL_DATE.format(value) : null;
    }
}
