package cz.tacr.elza.print;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemRecordRef;

public class RecordIterator implements Iterator<Record> {

    private NodeIterator nodeIterator;

    private Set<Integer> processedApIds = new HashSet<>();

    private LinkedList<Record> prepared = new LinkedList<>();

    public RecordIterator(final Output output,
                          final NodeIterator nodeIterator) {
        this.nodeIterator = nodeIterator;

        // fetch initial items
        processItems(output.getItems());
        if (prepared.size() == 0) {
            fetchNextRecords();
        }
    }

    @Override
    public boolean hasNext() {
        return (prepared.size() > 0);
    }

    @Override
    public Record next() {        
        if(prepared.size()==0) {
            throw new IllegalStateException("No more items");
        }
        Record n = prepared.pop();
        if (prepared.size() == 0) {
            // try to fetch next batch
            fetchNextRecords();
        }
        return n;
    }

    private void processItems(List<Item> items) {
        if (items == null) {
            return;
        }
        for (Item item : items) {
            if (item.getType().getDataType().equals(DataType.RECORD_REF) && (item instanceof ItemRecordRef)) {
                ItemRecordRef irr = (ItemRecordRef) item;
                Record record = irr.getRecord();
                if (!processedApIds.contains(record.getId())) {
                    processedApIds.add(record.getId());
                    prepared.add(record);
                }
            }
        }
    }
    private void fetchNextRecords() {
        while (nodeIterator.hasNext() && prepared.size() == 0) {
            Node node = nodeIterator.next();
            List<Item> items = node.getItems();
            processItems(items);
        }
    }

}
