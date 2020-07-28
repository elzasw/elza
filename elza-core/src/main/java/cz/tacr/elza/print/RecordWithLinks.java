package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemType;

/**
 * Record with valid links to other entities
 */
public class RecordWithLinks extends Record {

    private final List<Node> nodes = new ArrayList<>();

    private RecordWithLinks(Record srcRecord) {
        super(srcRecord);
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Return collection of sorted values
     *
     * @param itemTypeCode Type of item
     * @return
     */
    public Collection<String> getSortedValues(String itemTypeCode) {
        SortedSet<String> result = null;
        // List<Item> items = new ArrayList<>();
        // prepare collection of items
        for (Node node : nodes) {
            for (Item item : node.getItems()) {
                ItemType itemType = item.getType();
                if (itemType.getCode().equals(itemTypeCode)) {
                    // prepare result set if do not exists
                    if (result == null) {
                        result = createSortedSet(itemType);
                    }
                    result.add(item.getSerializedValue());
                }
            }
        }
        return result;
    }

    /**
     * Prepare set with comparator for given ItemType
     *
     * @param itemType
     * @return
     */
    private SortedSet<String> createSortedSet(ItemType itemType) {
        Comparator<String> c;
        if (itemType.getDataType().equals(DataType.INT)) {
            // comparator for ints
            c = ((arg0, arg1) -> Integer.compare(Integer.valueOf(arg0), Integer.valueOf(arg1)));
        } else {
            // comparator for strings
            c = ((a, b) -> a.compareTo(b));
        }
        return new TreeSet<>(c);
    }

    /**
     * Return sorted list of values
     *
     * @param itemType Type of description item
     * @param separator
     * @return
     */
    public String getSortedValuesOf(String itemTypeCode, String separator) {
        Collection<String> sortedValues = getSortedValues(itemTypeCode);
        if (CollectionUtils.isEmpty(sortedValues)) {
            return "";
        }

        // return values as string
        return String.join(separator, sortedValues);
    }

    /**
     * Return value of ApRecord
     *
     * @param srcRecord
     * @return
     */
    public static RecordWithLinks newInstance(Record srcRecord) {
        RecordWithLinks record = new RecordWithLinks(srcRecord);
        return record;
    }
}
