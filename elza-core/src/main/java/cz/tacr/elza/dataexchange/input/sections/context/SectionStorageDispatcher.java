package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;

/**
 * Stores import objects as batch per level depth.
 */
public class SectionStorageDispatcher {

    private List<SectionBatch> batchDepthStack = new ArrayList<>();

    private final StorageManager storageManager;

    private final int batchSize;

    public SectionStorageDispatcher(StorageManager storageManager, int batchSize) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getDepth() {
        return batchDepthStack.size() - 1;
    }

    /**
     * Dispatch all entity types.
     */
    public void dispatchAll() {
        dispatch(getDepth(), SectionBatch::storeAll);
    }

    public void addNode(ArrNodeWrapper node, int depth) {
        SectionBatch batch = getBatch(depth);
        if (batch.addNode(node)) {
            batch.storeNodes();
        }
    }

    public void addNodeRegister(ArrNodeRegisterWrapper nodeRegister, int depth) {
        SectionBatch batch = getBatch(depth);
        if (batch.addNodeRegister(nodeRegister)) {
            batch.storeNodeRegistry();
        }
    }

    public void addLevel(ArrLevelWrapper level, int depth) {
        SectionBatch batch = getBatch(depth);
        if (batch.addLevel(level)) {
            dispatch(depth, SectionBatch::storeNodes);
            batch.storeLevels();
        }
    }

    public void addDescItem(ArrDescItemWrapper descItem, int depth) {
        SectionBatch batch = getBatch(depth);
        if (batch.addDescItem(descItem)) {
            batch.storeDescItems();
        }
    }

    public void addData(ArrDataWrapper data, int depth) {
        SectionBatch batch = getBatch(depth);
        if (batch.addData(data)) {
            batch.storeData(data.getEntity().getType());
        }
    }

    private void dispatch(int depthLimit, Consumer<SectionBatch> storeAction) {
        for (int i = 0; i <= depthLimit; i++) {
            SectionBatch batch = batchDepthStack.get(i);
            storeAction.accept(batch);
        }
    }

    private SectionBatch getBatch(int depth) {
        int currDepth = getDepth();
        if (currDepth < depth) {
            Validate.isTrue(currDepth + 1 == depth);
            SectionBatch batch = new SectionBatch(storageManager, batchSize);
            batchDepthStack.add(batch);
            return batch;
        }
        return batchDepthStack.get(depth);
    }

    private static class SectionBatch {

        private final StorageManager storageManager;

        private final int batchSize;

        private final List<ArrNodeWrapper> nodes;

        private final List<ArrNodeRegisterWrapper> nodeRegistery;

        private final List<ArrLevelWrapper> levels;

        private final List<ArrDescItemWrapper> descItems;

        private final MultiValuedMap<DataType, ArrDataWrapper> dataTypeMap;

        public SectionBatch(StorageManager storageManager, int batchSize) {
            this.storageManager = storageManager;
            this.batchSize = batchSize;

            this.nodes = new ArrayList<>(batchSize);
            this.levels = new ArrayList<>(batchSize);
            this.descItems = new ArrayList<>(batchSize);
            this.dataTypeMap = new ArrayListValuedHashMap<>(batchSize);
            this.nodeRegistery = new ArrayList<>(batchSize);
        }

        /**
         * @return True when node batch is full.
         */
        public boolean addNode(ArrNodeWrapper node) {
            nodes.add(node);
            return nodes.size() >= batchSize;
        }

        /**
         * @return True when node register batch is full.
         */
        public boolean addNodeRegister(ArrNodeRegisterWrapper nodeRegister) {
            nodeRegistery.add(nodeRegister);
            return nodeRegistery.size() >= batchSize;
        }

        /**
         * @return True when level batch is full.
         */
        public boolean addLevel(ArrLevelWrapper level) {
            levels.add(level);
            return levels.size() >= batchSize;
        }

        /**
         * @return True when desc. item batch is full.
         */
        public boolean addDescItem(ArrDescItemWrapper descItem) {
            descItems.add(descItem);
            return descItems.size() >= batchSize;
        }

        /**
         * @return True data batch is full.
         */
        public boolean addData(ArrDataWrapper data) {
            DataType dt = data.getEntity().getType();
            Collection<ArrDataWrapper> group = this.dataTypeMap.get(dt);
            group.add(data);
            if (group.size() >= batchSize) {
                return true;
            }
            return false;
        }

        public void storeAll() {
            storeNodes();
            storeNodeRegistry();
            storeLevels();
            storeData();
            storeDescItems();
        }

        public void storeNodes() {
            if (nodes.isEmpty()) {
                return;
            }
            storageManager.saveSectionNodes(nodes);
            nodes.clear();
        }

        public void storeNodeRegistry() {
            if (nodeRegistery.isEmpty()) {
                return;
            }
            storeNodes();
            storageManager.saveSectionNodeRegistry(nodeRegistery);
            nodeRegistery.clear();
        }

        public void storeLevels() {
            if (levels.isEmpty()) {
                return;
            }
            storeNodes();
            storageManager.saveSectionLevels(levels);
            levels.clear();
        }

        public void storeDescItems() {
            if (descItems.isEmpty()) {
                return;
            }
            storeNodes();
            storeData();
            storageManager.saveSectionDescItems(descItems);
            descItems.clear();
        }

        public void storeData(DataType dataType) {
            Collection<ArrDataWrapper> group = dataTypeMap.get(dataType);
            if (group.size() > 0) {
                storageManager.saveSectionData(group);
                group.clear();
            }
        }

        public void storeData() {
            for (DataType dt : DataType.values()) {
                storeData(dt);
            }
        }
    }
}
