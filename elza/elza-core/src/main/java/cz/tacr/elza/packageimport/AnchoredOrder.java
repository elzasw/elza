package cz.tacr.elza.packageimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Places items with an anchor ("view after") directly after the item they name.
 *
 * <p>Items without an anchor keep their order. An anchored item is placed after its anchor and
 * after every item already placed behind that anchor (directly or through a chain of anchors), so
 * several items anchored to the same item keep their mutual order. An item whose anchor is missing
 * from the list, or that is part of an anchor cycle, is appended at the end and reported.
 */
public final class AnchoredOrder {

    private AnchoredOrder() {
    }

    /**
     * @param items
     *            items in their default order
     * @param codeOf
     *            code of an item; codes are unique within the list
     * @param anchorOf
     *            code of the item to place this one after, or null
     * @param unresolved
     *            receives each item whose anchor could not be applied
     * @return new list in the final order
     */
    public static <T> List<T> apply(final List<T> items,
                                    final Function<T, String> codeOf,
                                    final Function<T, String> anchorOf,
                                    final Consumer<T> unresolved) {
        List<T> result = new ArrayList<>(items.size());
        List<T> pending = new ArrayList<>();
        Map<String, String> anchorByCode = new HashMap<>();
        for (T item : items) {
            String anchor = anchorOf.apply(item);
            if (anchor == null) {
                result.add(item);
            } else {
                pending.add(item);
                anchorByCode.put(codeOf.apply(item), anchor);
            }
        }

        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            for (Iterator<T> it = pending.iterator(); it.hasNext();) {
                T item = it.next();
                String anchor = anchorOf.apply(item);
                int anchorIndex = indexOf(result, anchor, codeOf);
                if (anchorIndex < 0) {
                    continue;
                }
                int pos = anchorIndex + 1;
                while (pos < result.size()
                        && isPlacedBehind(codeOf.apply(result.get(pos)), anchor, anchorByCode)) {
                    pos++;
                }
                result.add(pos, item);
                it.remove();
                progress = true;
            }
        }

        for (T item : pending) {
            unresolved.accept(item);
            result.add(item);
        }
        return result;
    }

    private static <T> int indexOf(final List<T> list, final String code, final Function<T, String> codeOf) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(codeOf.apply(list.get(i)), code)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * True when the item's chain of anchors reaches the given anchor.
     */
    private static boolean isPlacedBehind(final String code, final String anchor,
                                          final Map<String, String> anchorByCode) {
        Set<String> visited = new HashSet<>();
        String current = anchorByCode.get(code);
        while (current != null && visited.add(current)) {
            if (current.equals(anchor)) {
                return true;
            }
            current = anchorByCode.get(current);
        }
        return false;
    }
}
