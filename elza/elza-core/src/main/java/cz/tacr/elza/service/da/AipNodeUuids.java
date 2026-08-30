package cz.tacr.elza.service.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * UUIDs an AIP offers for matching against the nodes of the archival description, in the order
 * they are matched: the package first, then the levels of the logical structural map top down,
 * then the representations.
 *
 * A UUID is written with the {@value #UUID_PREFIX} prefix inside an AIP (an XML ID must not
 * start with a digit), while a node stores it bare - the prefix is stripped here.
 */
public final class AipNodeUuids {

    public static final String UUID_PREFIX = "uuid-";

    /** Length of a UUID in its canonical text form, the length a node stores. */
    private static final int UUID_LENGTH = 36;

    private AipNodeUuids() {
    }

    /**
     * @return the bare UUID, or null when the value cannot be one
     */
    public static String normalize(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String uuid = value.trim();
        if (StringUtils.startsWithIgnoreCase(uuid, UUID_PREFIX)) {
            uuid = uuid.substring(UUID_PREFIX.length());
        }
        return uuid.length() == UUID_LENGTH ? uuid : null;
    }

    /**
     * Builds the matching order out of the parts of an AIP; values that cannot be a UUID are
     * left out and duplicates keep their first position.
     */
    public static List<String> inMatchingOrder(String packageUuid,
                                               Collection<String> levelUuids,
                                               Collection<String> representationUuids) {
        List<String> ordered = new ArrayList<>();
        ordered.add(packageUuid);
        if (levelUuids != null) {
            ordered.addAll(levelUuids);
        }
        if (representationUuids != null) {
            ordered.addAll(representationUuids);
        }

        Set<String> result = new LinkedHashSet<>();
        for (String value : ordered) {
            String uuid = normalize(value);
            if (uuid != null) {
                result.add(uuid);
            }
        }
        return List.copyOf(result);
    }
}
