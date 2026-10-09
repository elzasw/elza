package cz.tacr.elza.packageimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Ordering of specifications by their view-after anchors.
 */
class AnchoredOrderTest {

    /** Item code and its optional anchor, written as "CODE" or "CODE>ANCHOR". */
    private static List<String> order(String... items) {
        return order(new ArrayList<>(), items);
    }

    private static List<String> order(List<String> unresolved, String... items) {
        List<String> result = AnchoredOrder.apply(Arrays.asList(items), AnchoredOrderTest::code,
                                                  AnchoredOrderTest::anchor, i -> unresolved.add(code(i)));
        return result.stream().map(AnchoredOrderTest::code).toList();
    }

    private static String code(String item) {
        int i = item.indexOf('>');
        return i < 0 ? item : item.substring(0, i);
    }

    private static String anchor(String item) {
        int i = item.indexOf('>');
        return i < 0 ? null : item.substring(i + 1);
    }

    @Test
    void withoutAnchorsTheOrderIsKept() {
        assertEquals(List.of("A", "B", "C"), order("A", "B", "C"));
    }

    @Test
    void anAnchoredItemFollowsItsAnchor() {
        assertEquals(List.of("A", "X", "B", "C"), order("A", "B", "C", "X>A"));
    }

    @Test
    void itemsAnchoredToTheSameItemKeepTheirMutualOrder() {
        assertEquals(List.of("A", "X", "Y", "B"), order("A", "B", "X>A", "Y>A"));
    }

    @Test
    void aChainIsResolvedWhateverTheInputOrder() {
        // Y follows X which follows A; Z follows A too and comes after the whole chain
        assertEquals(List.of("A", "X", "Y", "Z", "B"), order("A", "B", "Y>X", "X>A", "Z>A"));
    }

    @Test
    void anAnchorMayBeTheLastItem() {
        assertEquals(List.of("A", "B", "X"), order("A", "B", "X>B"));
    }

    @Test
    void anUnknownAnchorPutsTheItemLastAndReportsIt() {
        List<String> unresolved = new ArrayList<>();
        assertEquals(List.of("A", "B", "X"), order(unresolved, "X>MISSING", "A", "B"));
        assertEquals(List.of("X"), unresolved);
    }

    @Test
    void aCycleIsReportedInsteadOfLooping() {
        List<String> unresolved = new ArrayList<>();
        assertEquals(List.of("A", "X", "Y"), order(unresolved, "A", "X>Y", "Y>X"));
        assertEquals(List.of("X", "Y"), unresolved);
    }

    @Test
    void anAnchorIsNotMovedItself() {
        // the anchor keeps its own position; only the anchored item moves
        Map<String, Integer> pos = Map.of("A", 0, "B", 1, "C", 2);
        List<String> result = order("A", "B", "C", "X>B");
        assertEquals(pos.get("B"), result.indexOf("B"));
        assertEquals(2, result.indexOf("X"));
    }
}
