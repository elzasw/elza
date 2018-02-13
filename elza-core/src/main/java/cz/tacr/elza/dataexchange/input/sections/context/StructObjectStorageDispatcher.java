package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;

public class StructObjectStorageDispatcher {

    private final StorageManager storageManager;

    private final int batchSize;

    private final List<ArrStructObjectWrapper> objectQueue;

    private final MultiValuedMap<DataType, ArrDataWrapper> dataQueue;

    private final List<ArrStructItemWrapper> itemQueue;

    private final List<DelayedItemDataPair> delayedItemDataPairs = new ArrayList<>();

    public StructObjectStorageDispatcher(StorageManager storageManager, int batchSize) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.objectQueue = new ArrayList<>(batchSize);
        this.dataQueue = new ArrayListValuedHashMap<>(batchSize);
        this.itemQueue = new ArrayList<>(batchSize);
    }

    public void addStructObject(ArrStructObjectWrapper structObject) {
        objectQueue.add(structObject);
        if (objectQueue.size() >= batchSize) {
            storeObjects();
        }
        refreshDelayedItems(structObject.getImportId());
    }

    public void addStructItem(ArrStructItemWrapper structItem) {
        itemQueue.add(structItem);
        if (itemQueue.size() >= batchSize) {
            storeItems();
        }
    }

    public void addStructItem(ArrStructItemWrapper structItem, ArrDataWrapper data) {
        DataType dt = data.getEntity().getType();
        addData(data, dt);
        addStructItem(structItem);
    }

    public void addDelayedStructItem(ArrStructItemWrapper structItem, DelayedStructObjectRefWrapper delayedData) {
        delayedItemDataPairs.add(new DelayedItemDataPair(structItem, delayedData));
    }

    /**
     * Dispatch all entities. All referenced structured objects must be processed.
     */
    public void dispatchAll() {
        storeObjects();
        // dispatch all delayed items, referenced objects should be processed at this point
        delayedItemDataPairs.forEach(DelayedItemDataPair::dispatch);
        delayedItemDataPairs.clear();
        // store remaining items
        storeData();
        storeItems();
    }

    private void addData(ArrDataWrapper data, DataType dataType) {
        Validate.notNull(data);
        Validate.notNull(dataType);

        Collection<ArrDataWrapper> group = dataQueue.get(dataType);
        group.add(data);
        if (group.size() >= batchSize) {
            storeData(dataType);
        }
    }

    private void refreshDelayedItems(String importId) {
        Iterator<DelayedItemDataPair> it = delayedItemDataPairs.iterator();
        // find all items referencing structured object represented by import id
        while (it.hasNext()) {
            DelayedItemDataPair pair = it.next();
            if (pair.isDelayedImportId(importId)) {
                // dispatch delayed item & data
                pair.dispatch();
                it.remove();
            }
        }
    }

    private void storeObjects() {
        if (objectQueue.isEmpty()) {
            return;
        }
        storageManager.saveStructObjects(objectQueue);
        objectQueue.clear();
    }

    public void storeData(DataType dataType) {
        Collection<ArrDataWrapper> group = dataQueue.get(dataType);
        if (group.isEmpty()) {
            return;
        }
        storageManager.saveData(group);
        group.clear();
    }

    public void storeData() {
        for (DataType dt : DataType.values()) {
            storeData(dt);
        }
    }

    private void storeItems() {
        if (itemQueue.isEmpty()) {
            return;
        }
        storeObjects();
        storeData();
        storageManager.saveStructItems(itemQueue);
        itemQueue.clear();
    }

    private class DelayedItemDataPair {

        private final ArrStructItemWrapper structItem;

        private final DelayedStructObjectRefWrapper delayedData;

        public DelayedItemDataPair(ArrStructItemWrapper structItem, DelayedStructObjectRefWrapper delayedData) {
            this.structItem = structItem;
            this.delayedData = delayedData;
        }

        public boolean isDelayedImportId(String importId) {
            return delayedData.getRefStructObjectImportId().equals(importId);
        }

        public void dispatch() {
            addStructItem(structItem, delayedData);
        }
    }
}
