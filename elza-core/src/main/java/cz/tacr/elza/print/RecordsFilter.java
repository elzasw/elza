package cz.tacr.elza.print;

import java.util.HashSet;
import java.util.Set;

import cz.tacr.elza.print.item.Item;

public class RecordsFilter {

    Set<String> recordTypes = new HashSet<>();

    Set<String> itemTypes = new HashSet<>();

    public RecordsFilter addType(String type) {
        recordTypes.add(type);
        return this;
    }

    public RecordsFilter addItemType(String itemType) {
        itemTypes.add(itemType);
        return this;
    }

    @Override
    public int hashCode() {
        return recordTypes.size() * 31 + itemTypes.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RecordsFilter)) {
            return false;
        }
        RecordsFilter rf = (RecordsFilter) obj;
        if (!rf.itemTypes.equals(itemTypes)) {
            return false;
        }
        if (!rf.recordTypes.equals(recordTypes)) {
            return false;
        }
        return true;
    }

    public boolean match(Record rec, Item item) {
        // match record type
        if (this.recordTypes.size() > 0) {
            RecordType type = rec.getType();
            while (type != null) {
                if (recordTypes.contains(type.getCode())) {
                    break;
                }
                type = type.getParentType();
            }
            if (type == null) {
                // type not match
                return false;
            }
        }

        // match item type
        if (this.itemTypes.size() > 0) {
            String itemTypeCode = item.getType().getCode();
            if (!itemTypes.contains(itemTypeCode)) {
                return false;
            }
        }

        return true;
    }
}
