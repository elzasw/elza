package cz.tacr.elza.schema.support;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

/**
 * JAXB adapter for {@code xs:dateTime} backed by {@link OffsetDateTime}, used by
 * the cam-2019 (APIv1) binding.
 * <p>
 * Marshalling emits the wall clock in the JVM default zone, truncated to seconds
 * and without a zone suffix — i.e. {@code 2026-05-06T16:25:07}. APIv1 keeps this
 * legacy zone-less form for wire compatibility; clients re-interpret it in their
 * own implicit zone. (APIv2 uses {@link OffsetDateTimeWithZoneAdapter}, which
 * preserves the offset on the wire.)
 * <p>
 * Unmarshalling is lenient — both forms permitted by {@code xs:dateTime} are
 * accepted:
 * <ul>
 *   <li>with explicit offset (e.g. {@code 2026-05-06T16:25:07+02:00}, or
 *       {@code …Z}) – parsed as-is,</li>
 *   <li>without offset (e.g. {@code 2026-05-06T16:25:07}) – the JVM default zone
 *       is attached, matching the legacy assumption that zone-less timestamps
 *       reflect the writer's wall clock.</li>
 * </ul>
 */
public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime unmarshal(String value) {
        value = StringUtils.stripToNull(value);
        if (value == null) {
            return null;
        }
        if (hasOffset(value)) {
            return OffsetDateTime.parse(value);
        }
        return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @Override
    public String marshal(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        // Shift to the JVM default zone first (Hibernate's default storage policy
        // normalises to UTC; emitting the UTC wall clock zone-less would otherwise
        // mislead clients that assume local time), then drop sub-second precision
        // so the output is always {@code …:SS} not {@code …:SS.NNN}.
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                value.atZoneSameInstant(ZoneId.systemDefault())
                        .truncatedTo(ChronoUnit.SECONDS));
    }

    private static boolean hasOffset(String value) {
        int t = value.indexOf('T');
        if (t < 0) {
            return false;
        }
        // Search after the time portion for Z or +/- (avoid matching the date's '-' separators).
        for (int i = t + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'Z' || c == '+' || (c == '-' && i > t + 1)) {
                return true;
            }
        }
        return false;
    }
}
