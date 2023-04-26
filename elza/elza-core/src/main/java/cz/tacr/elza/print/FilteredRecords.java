package cz.tacr.elza.print;

import java.text.Collator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;

/**
 * Collection of records
 *
 */
public class FilteredRecords {
    /**
     * Code of applied filter
     */
    private final RecordsFilter filter;

    /**
     * Hash map of records by record id
     */
    private final Map<Integer, RecordWithLinks> recordsMap = new HashMap<>();

    /**
     * Sorted collection of records
     */
    private Collection<RecordWithLinks> records;

    private ElzaLocale elzaLocale;

    FilteredRecords(final ElzaLocale elzaLocale, final RecordsFilter filter) {
        this.filter = filter;
        this.elzaLocale = elzaLocale;
    }

    public RecordsFilter getFilter() {
        return filter;
    }

    public Collection<RecordWithLinks> getRecords() {
        return records;
    }

    /**
     * Add to the list of records
     *
     * @param node
     */
    public void addNode(Node node) {
        List<Item> items = node.getItems();
        if (items != null) {
            for (Item item : items) {
                if (item instanceof ItemRecordRef) {
                    Record r = item.getValue(Record.class);
                    if (filter.match(r, item)) {
                        addRecord(r, node);
                    }
                }
            }
        }
    }

    /**
     * Add single result
     *
     * @param rec
     * @param node
     * @return
     */
    private RecordWithLinks addRecord(Record rec, Node node) {
        int recordId = rec.getId();
        // check if record exists
        RecordWithLinks rwl = recordsMap.get(recordId);
        if (rwl == null) {
            // check if allowed record type
            rwl = RecordWithLinks.newInstance(rec);
            recordsMap.put(recordId, rwl);
        }
        rwl.addNode(node);

        return rwl;
    }

    /**
     * Final method when all nodes were added
     *
     * This method allows to sort items
     */
    public void nodesAdded() {
        Collator collator = elzaLocale.getCollator();
        records = recordsMap.values().stream()
                .sorted((v1, v2) -> collator.compare(v1.getPreferredPart().getValue(), v2.getPreferredPart().getValue()))
                .collect(Collectors.toList());
    }
}
