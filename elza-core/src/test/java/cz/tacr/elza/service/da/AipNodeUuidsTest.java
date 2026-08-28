package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class AipNodeUuidsTest {

    private static final String A = "2fde031b-0cac-4e5a-b04a-8ad8da08bed4";
    private static final String B = "2e9d2e65-b2e9-4e5e-ad84-1730553f3be3";
    private static final String C = "506b5ba5-b9a4-4907-b0f2-f87e7db046e3";

    @Test
    void normalize_stripsTheAipPrefix() {
        assertEquals(A, AipNodeUuids.normalize("uuid-" + A));
        assertEquals(A, AipNodeUuids.normalize("UUID-" + A));
        assertEquals(A, AipNodeUuids.normalize("  uuid-" + A + "  "));
        assertEquals(A, AipNodeUuids.normalize(A));
    }

    @Test
    void normalize_rejectsWhatCannotBeAUuid() {
        assertNull(AipNodeUuids.normalize(null));
        assertNull(AipNodeUuids.normalize("   "));
        assertNull(AipNodeUuids.normalize("vs_1"));
        assertNull(AipNodeUuids.normalize("uuid-vs_1"));
        assertNull(AipNodeUuids.normalize(A + "-too-long"));
    }

    @Test
    void matchingOrder_isPackageThenLevelsThenRepresentations() {
        List<String> ordered = AipNodeUuids.inMatchingOrder("uuid-" + A, List.of("uuid-" + B), List.of("uuid-" + C));

        assertEquals(List.of(A, B, C), ordered);
    }

    @Test
    void matchingOrder_dropsUnusableValuesAndKeepsTheFirstPositionOfADuplicate() {
        List<String> ordered = AipNodeUuids.inMatchingOrder(
                null,
                List.of("uuid-" + B, "vs_1", "uuid-" + C),
                List.of("uuid-" + B));

        assertEquals(List.of(B, C), ordered);
    }

    @Test
    void matchingOrder_toleratesMissingParts() {
        assertEquals(List.of(), AipNodeUuids.inMatchingOrder(null, null, null));
        assertEquals(List.of(A), AipNodeUuids.inMatchingOrder(A, null, null));
    }
}
