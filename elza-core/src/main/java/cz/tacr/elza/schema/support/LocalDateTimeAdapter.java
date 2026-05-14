package cz.tacr.elza.schema.support;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


/**
 * Adapter for CAM v1 {@code xs:dateTime}: typed as {@link LocalDateTime} since
 * CAM v1 doesn't carry timezone information. Unmarshalling tolerates both the
 * local form ({@code 2025-01-01T12:00:00}) and the offset form
 * ({@code 2025-01-01T12:00:00+02:00}) — in the latter case the offset is
 * folded into the wall-clock value and dropped, since this adapter targets a
 * zone-less type.
 */
public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    public LocalDateTime unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }

    public String marshal(LocalDateTime value) {
        return value != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value) : null;
    }
}
