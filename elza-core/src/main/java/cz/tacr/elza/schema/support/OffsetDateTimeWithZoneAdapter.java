package cz.tacr.elza.schema.support;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.StringUtils;

/**
 * JAXB adapter for {@code xs:dateTime} backed by {@link OffsetDateTime}, used by
 * the cam-2025 (APIv2) binding. Output is constrained to match the schema's
 * {@code DateTime} pattern: {@code YYYY-MM-DDTHH:MM:SS} followed by either
 * {@code Z} or a whole-minute offset {@code ±HH:MM}.
 *
 * <p>Two normalisations are applied on marshal:</p>
 * <ul>
 *   <li>The value is truncated to seconds — JVM-default precision is
 *       milli/micro, which would otherwise emit {@code .NNN} and fail the
 *       schema pattern.</li>
 *   <li>If the offset has a non-zero seconds component (only theoretically
 *       possible — no standard tz database zone has one), it is rounded toward
 *       zero to whole minutes so the wire form stays compliant.</li>
 * </ul>
 *
 * <p>Unmarshal is lenient — both forms permitted by {@code xs:dateTime} are
 * accepted (with or without offset). When no offset is present, the JVM default
 * zone is attached, matching the historical assumption.</p>
 */
public class OffsetDateTimeWithZoneAdapter extends XmlAdapter<String, OffsetDateTime> {

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
        OffsetDateTime normalized = normalizeOffsetToWholeMinutes(value).truncatedTo(ChronoUnit.SECONDS);
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(normalized);
    }

    /**
     * If {@code value}'s offset has a non-zero seconds component, shift to the
     * nearest whole-minute offset (rounding toward zero) preserving the same
     * instant. No-op for every offset produced by the standard tz database.
     */
    private static OffsetDateTime normalizeOffsetToWholeMinutes(OffsetDateTime value) {
        int totalSeconds = value.getOffset().getTotalSeconds();
        int subMinuteSeconds = totalSeconds % 60;
        if (subMinuteSeconds == 0) {
            return value;
        }
        ZoneOffset wholeMinuteOffset = ZoneOffset.ofTotalSeconds(totalSeconds - subMinuteSeconds);
        return value.withOffsetSameInstant(wholeMinuteOffset);
    }

    private static boolean hasOffset(String value) {
        int t = value.indexOf('T');
        if (t < 0) {
            return false;
        }
        for (int i = t + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'Z' || c == '+' || (c == '-' && i > t + 1)) {
                return true;
            }
        }
        return false;
    }
}
