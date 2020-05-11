package cz.tacr.elza.print;

import java.text.Collator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.ElzaLocale;

/**
 * Collection of records
 *
 */
public class FilteredRecords {
    /**
     * Code of applied filter
     */
    private final String filterType;

    /**
     * Hash map of records by record id
     */
    private final Map<Integer, RecordWithLinks> recordsMap = new HashMap<>();

    /**
     * Sorted collection of records
     */
    private Collection<RecordWithLinks> records;

    private ElzaLocale elzaLocale;

    FilteredRecords(ElzaLocale elzaLocale, String filterType) {
        this.filterType = Validate.notEmpty(filterType);
        this.elzaLocale = elzaLocale;
    }

    public String getFilterType() {
        return filterType;
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
        List<Record> recs = node.getRecords();

        for (Record rec : recs) {
            addRecord(rec, node);
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
            RecordType type = rec.getType();
            while (type != null) {
                if (filterType.equals(type.getCode())) {
                    rwl = RecordWithLinks.newInstance(rec);
                    recordsMap.put(recordId, rwl);
                    rwl.addNode(node);
                    break;
                }
                // get parent type
                type = type.getParentType();
            }
        } else {
            rwl.addNode(node);
        }
        return rwl;
    }

    /**
     * Final method when all nodes were added
     *
     * This method allows to sort items
     */
    public void nodesAdded() {
       /* Collator collator = elzaLocale.getCollator();
        records = recordsMap.values().stream()
                .sorted((v1, v2) -> collator.compare(v1.getPrefName().getFullName(), v2.getPrefName().getFullName()))
                .collect(Collectors.toList());*/
    }
}
