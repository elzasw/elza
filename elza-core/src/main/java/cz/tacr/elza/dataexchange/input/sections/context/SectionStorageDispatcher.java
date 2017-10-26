package cz.tacr.elza.dataexchange.input.sections.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;

/**
 * Stores imported node objects by tree depth.
 */
public class SectionStorageDispatcher {

    private final static int GROW_SIZE = 16;

    private final StorageManager storageManager;

    private final int batchSize;

    private int maxDepth = -1;

    private SectionBatch[] batchStack = new SectionBatch[GROW_SIZE];

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

    /**
     * Dispatch all entity types.
     */
    public void dispatchAll() {
        dispatch(maxDepth, SectionBatch::storeAll);
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
        DataType dt = batch.addData(data);
        if (dt != null) {
            batch.storeData(dt);
        }
    }

    private void dispatch(int depthLimit, Consumer<SectionBatch> storeAction) {
        for (int i = 0; i <= depthLimit; i++) {
            SectionBatch level = batchStack[i];
            storeAction.accept(level);
        }
    }

    private SectionBatch getBatch(int depth) {
        ensureCapacity(depth);
        SectionBatch level = batchStack[depth];
        if (level == null) {
            if (maxDepth + 1 != depth) {
                throw new IllegalStateException("Depth must increased by one");
            }
            batchStack[depth] = level = new SectionBatch(storageManager, batchSize);
            maxDepth = depth;
        }
        return level;
    }

    private void ensureCapacity(int depth) {
        int requiredLength = depth + 1;
        if (requiredLength > batchStack.length) {
            batchStack = Arrays.copyOf(batchStack, requiredLength + GROW_SIZE);
        }
    }

    private static class SectionBatch {

        private final StorageManager storageManager;

        private final int batchSize;

        private final List<ArrNodeWrapper> nodes;

        private final List<ArrNodeRegisterWrapper> nodeRegistery;

        private final List<ArrLevelWrapper> levels;

        private final List<ArrDescItemWrapper> descItems;

        private final MultiValuedMap<DataType, ArrDataWrapper> data;

        public SectionBatch(StorageManager storageManager, int batchSize) {
            this.storageManager = storageManager;
            this.batchSize = batchSize;

            this.nodes = new ArrayList<>(batchSize);
            this.levels = new ArrayList<>(batchSize);
            this.descItems = new ArrayList<>(batchSize);
            this.data = new ArrayListValuedHashMap<>(batchSize);
            this.nodeRegistery = new ArrayList<>(batchSize);
        }

        public boolean addNode(ArrNodeWrapper node) {
            nodes.add(node);
            return nodes.size() >= batchSize;
        }

        public boolean addNodeRegister(ArrNodeRegisterWrapper nodeRegister) {
            nodeRegistery.add(nodeRegister);
            return nodeRegistery.size() >= batchSize;
        }

        public boolean addLevel(ArrLevelWrapper level) {
            levels.add(level);
            return levels.size() >= batchSize;
        }

        public boolean addDescItem(ArrDescItemWrapper descItem) {
            descItems.add(descItem);
            return descItems.size() >= batchSize;
        }

        public DataType addData(ArrDataWrapper data) {
            DataType dt = data.getEntity().getType();
            Collection<ArrDataWrapper> group = this.data.get(dt);
            group.add(data);
            if (group.size() >= batchSize) {
                return dt;
            }
            return null;
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
            Collection<ArrDataWrapper> group = data.get(dataType);
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
