package cz.tacr.elza.schema.support;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    public OffsetDateTime unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        if (value == null) {
            return null;
        }
        // xs:dateTime permits an optional timezone offset. If present, keep it;
        // otherwise assume system-default zone so we still produce an
        // OffsetDateTime that round-trips through Jackson / JPA without losing
        // the wall-clock value.
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
    }

    public String marshal(OffsetDateTime value) {
        return value != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value) : null;
    }
}
