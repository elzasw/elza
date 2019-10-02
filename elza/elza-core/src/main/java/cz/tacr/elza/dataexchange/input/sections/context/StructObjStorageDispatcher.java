package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;

public class StructObjStorageDispatcher {

    private final StorageManager storageManager;

    private final int batchSize;

    private final List<ArrStructObjectWrapper> objectQueue;

    private final MultiValuedMap<DataType, ArrDataWrapper> dataQueue;

    private final List<ArrStructItemWrapper> itemQueue;

    private final List<Pair<ArrStructItemWrapper, ArrDataStructureRefWrapper>> structObjRefs = new LinkedList<>();

    public StructObjStorageDispatcher(StorageManager storageManager, int batchSize) {
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
        dispatchStructObjRefs(structObject.getImportId());
    }

    public void addStructItem(ArrStructItemWrapper structItem) {
        itemQueue.add(structItem);
        if (itemQueue.size() >= batchSize) {
            storeItems(true);
        }
    }

    public void addStructItem(ArrStructItemWrapper structItem, ArrDataWrapper data) {
        DataType dt = data.getEntity().getType();
        addData(data, dt);
        addStructItem(structItem);
    }

    /**
     * Method is used when referenced structured object is not processed yet.
     */
    public void addStructObjRef(ArrStructItemWrapper structItem, ArrDataStructureRefWrapper data) {
        structObjRefs.add(Pair.of(structItem, data));
    }

    /**
     * Dispatch all entities. All referenced structured objects must be processed.
     */
    public void dispatchAll() {
        storeObjects();
        // dispatch all delayed items, referenced objects should be processed at this point
        structObjRefs.forEach(pair -> addStructItem(pair.getLeft(), pair.getRight()));
        structObjRefs.clear();
        // store remaining items
        storeData();
        storeItems(false);
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

    private void dispatchStructObjRefs(String structObjImportId) {
        Iterator<Pair<ArrStructItemWrapper, ArrDataStructureRefWrapper>> it = structObjRefs.iterator();
        // dispatch all items referencing given import id of structured object
        while (it.hasNext()) {
            Pair<ArrStructItemWrapper, ArrDataStructureRefWrapper> pair = it.next();
            String refImportId = pair.getRight().getStructObjImportId();
            if (refImportId.equals(structObjImportId)) {
                // add delayed item & data
                addStructItem(pair.getLeft(), pair.getRight());
                it.remove();
            }
        }
    }

    private void storeObjects() {
        if (objectQueue.isEmpty()) {
            return;
        }
        storageManager.storeGeneric(objectQueue);
        objectQueue.clear();
    }

    public void storeData(DataType dataType) {
        Collection<ArrDataWrapper> group = dataQueue.get(dataType);
        if (group.isEmpty()) {
            return;
        }
        storageManager.storeGeneric(group);
        group.clear();
    }

    public void storeData() {
        for (DataType dt : DataType.values()) {
            storeData(dt);
        }
    }

    private void storeItems(boolean storeReferenced) {
        if (itemQueue.isEmpty()) {
            return;
        }
        if (storeReferenced) {
            storeObjects();
            storeData();
        }
        storageManager.storeGeneric(itemQueue);
        itemQueue.clear();
    }
}
