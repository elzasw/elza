package cz.tacr.elza.schema.support;

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class LocalTimeAdapter extends XmlAdapter<String, LocalTime> {

    public LocalTime unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        return value != null ? LocalTime.parse(value) : null;
    }

    public String marshal(LocalTime value) {
        return value != null ? DateTimeFormatter.ISO_LOCAL_TIME.format(value) : null;
    }
}
