package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;

class LevelBatchReader {

    private final ArrLevel[] levels;

    private final SectionOutputStream sectionOutputStream;

    private final NodeCacheService nodeCacheService;

    private boolean firstBatch = true;

    private int index = -1;

    public LevelBatchReader(int capacity, SectionOutputStream sectionOutputStream, NodeCacheService nodeCacheService) {
        this.levels = new ArrLevel[capacity];
        this.sectionOutputStream = sectionOutputStream;
        this.nodeCacheService = nodeCacheService;
    }

    public int getSize() {
        return index + 1;
    }

    public List<Integer> getNodeIds() {
        List<Integer> nodeIds = new ArrayList<>(getSize());
        for (int i = 0; i <= index; i++) {
            nodeIds.add(levels[i].getNodeId());
        }
        return nodeIds;
    }

    public void addLevel(ArrLevel level) {
        levels[++index] = level;
        if (getSize() == levels.length) {
            flush();
        }
    }

    public void flush() {
        int size = getSize();
        if (size == 0) {
            return;
        }
        readLevelBatch();

        Arrays.fill(levels, 0, size, null);
        firstBatch = false;
        index = -1;
    }

    private void readLevelBatch() {
        Map<Integer, CachedNode> cachedNodes = nodeCacheService.getNodes(getNodeIds());

        for (int i = 0; i <= index; i++) {
            ArrLevel level = levels[i];
            CachedNode cachedNode = cachedNodes.get(level.getNodeId());
            readLevel(level, cachedNode);
        }
    }

    private void readLevel(ArrLevel level, CachedNode cachedNode) {
        ArrNode node = level.getNode();

        Validate.notNull(cachedNode);
        Validate.notNull(node);

        // remove parent for first node (section root node)
        Integer parentNodeId = firstBatch && level == levels[0] ? null : level.getNodeIdParent();

        ExportLevelInfo info = new ExportLevelInfo(level.getNodeId(), parentNodeId);
        info.setNodeUuid(node.getUuid());
        info.setDescItems(cachedNode.getDescItems());
        info.setNodeAPs(cachedNode.getNodeRegisters());

        sectionOutputStream.addLevel(info);
    }
}
