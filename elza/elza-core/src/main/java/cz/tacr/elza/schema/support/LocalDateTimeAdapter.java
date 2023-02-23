package cz.tacr.elza.schema.support;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    public LocalDateTime unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        return value != null ? LocalDateTime.parse(value) : null;
    }

    public String marshal(LocalDateTime value) {
        return value != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value) : null;
    }
}
