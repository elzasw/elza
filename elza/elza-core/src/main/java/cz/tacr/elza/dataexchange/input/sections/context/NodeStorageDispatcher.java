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
public class NodeStorageDispatcher {

    private List<NodeDepthBatch> depthStack = new ArrayList<>();

    private final StorageManager storageManager;

    private final int batchSize;

    public NodeStorageDispatcher(StorageManager storageManager, int batchSize) {
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
        return depthStack.size() - 1;
    }

    /**
     * Dispatch all entity types.
     */
    public void dispatchAll() {
        dispatch(getDepth(), NodeDepthBatch::storeAll);
    }

    public void addNode(ArrNodeWrapper node, int depth) {
        NodeDepthBatch batch = getBatch(depth);
        if (batch.addNode(node)) {
            batch.storeNodes();
        }
    }

    public void addLevel(ArrLevelWrapper level, int depth) {
        NodeDepthBatch batch = getBatch(depth);
        if (batch.addLevel(level)) {
            dispatch(depth, NodeDepthBatch::storeNodes);
            batch.storeLevels(true);
        }
    }

    public void addDescItem(ArrDescItemWrapper descItem, int depth) {
        NodeDepthBatch batch = getBatch(depth);
        if (batch.addDescItem(descItem)) {
            batch.storeDescItems(true);
        }
    }

    public void addData(ArrDataWrapper data, int depth) {
        NodeDepthBatch batch = getBatch(depth);
        if (batch.addData(data)) {
            batch.storeData(data.getEntity().getType());
        }
    }

    private void dispatch(int depthLimit, Consumer<NodeDepthBatch> storeAction) {
        for (int i = 0; i <= depthLimit; i++) {
            NodeDepthBatch batch = depthStack.get(i);
            storeAction.accept(batch);
        }
    }

    private NodeDepthBatch getBatch(int depth) {
        int currDepth = getDepth();
        if (currDepth < depth) {
            Validate.isTrue(currDepth + 1 == depth);
            NodeDepthBatch batch = new NodeDepthBatch(storageManager, batchSize);
            depthStack.add(batch);
            return batch;
        }
        return depthStack.get(depth);
    }

    private static class NodeDepthBatch {

        private final StorageManager storageManager;

        private final int batchSize;

        private final List<ArrNodeWrapper> nodes;

        private final List<ArrLevelWrapper> levels;

        private final List<ArrDescItemWrapper> descItems;

        private final MultiValuedMap<DataType, ArrDataWrapper> dataTypeMap;

        public NodeDepthBatch(StorageManager storageManager, int batchSize) {
            this.storageManager = storageManager;
            this.batchSize = batchSize;

            this.nodes = new ArrayList<>(batchSize);
            this.levels = new ArrayList<>(batchSize);
            this.descItems = new ArrayList<>(batchSize);
            this.dataTypeMap = new ArrayListValuedHashMap<>(batchSize);
        }

        /**
         * @return True when node batch is full.
         */
        public boolean addNode(ArrNodeWrapper node) {
            nodes.add(node);
            return nodes.size() >= batchSize;
        }

        /**
         * @return True when level batch is full.
         */
        public boolean addLevel(ArrLevelWrapper level) {
            levels.add(level);
            return levels.size() >= batchSize;
        }

        /**
         * @return True when DescItem batch is full.
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
            storeLevels(false);
            storeData();
            storeDescItems(false);
        }

        public void storeNodes() {
            if (nodes.isEmpty()) {
                return;
            }
            storageManager.storeGeneric(nodes);
            nodes.clear();
        }

        public void storeLevels(boolean storeReferenced) {
            if (levels.isEmpty()) {
                return;
            }
            if (storeReferenced) {
                storeNodes();
            }
            storageManager.storeGeneric(levels);
            levels.clear();
        }

        public void storeDescItems(boolean storeReferenced) {
            if (descItems.isEmpty()) {
                return;
            }
            if (storeReferenced) {
                storeNodes();
                storeData();
            }
            storageManager.storeGeneric(descItems);
            descItems.clear();
        }

        public void storeData(DataType dataType) {
            Collection<ArrDataWrapper> group = dataTypeMap.get(dataType);
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
    }
}
