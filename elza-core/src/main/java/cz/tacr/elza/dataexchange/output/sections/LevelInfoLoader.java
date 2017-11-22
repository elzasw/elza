package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractBatchLoader;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Reads supplied levels from node cache and pass them to {@link SectionOutputStream}.
 */
public class LevelInfoLoader extends AbstractBatchLoader<ArrLevel, ExportLevelInfo> {

    private final NodeCacheService nodeCacheService;

    private boolean firstBatch = true;

    public LevelInfoLoader(int batchSize, NodeCacheService nodeCacheService) {
        super(batchSize);
        this.nodeCacheService = nodeCacheService;
    }

    @Override
    protected void processBatch(ArrayList<BatchEntry> entries) {
        List<Integer> nodeIds = getNodeIds(entries);
        Map<Integer, RestoredNode> cachedNodes = nodeCacheService.getNodes(nodeIds);

        for (int i = 0; i < entries.size(); i++) {
            BatchEntry entry = entries.get(i);

            ArrLevel level = entry.getRequest();
            // remove parent for first node (section root node)
            Integer parentNodeId = firstBatch && i == 0 ? null : level.getNodeIdParent();
            // cached node from prepared map
            RestoredNode cachedNode = cachedNodes.get(level.getNodeId());

            ExportLevelInfo levelInfo = createLevelInfo(level.getNodeId(), parentNodeId, cachedNode);
            entry.addResult(levelInfo);
        }

        firstBatch = false;
    }

    private static List<Integer> getNodeIds(List<BatchEntry> entries) {
        List<Integer> nodeIds = new ArrayList<>(entries.size());
        for (BatchEntry entry : entries) {
            nodeIds.add(entry.getRequest().getNodeId());
        }
        return nodeIds;
    }

    private static ExportLevelInfo createLevelInfo(Integer nodeId, Integer parentNodeId, CachedNode cachedNode) {
        Validate.notNull(nodeId);
        Validate.notNull(cachedNode);

        ExportLevelInfo levelInfo = new ExportLevelInfo(nodeId, parentNodeId);
        levelInfo.setNodeUuid(cachedNode.getUuid());
        levelInfo.setDescItems(cachedNode.getDescItems());
        levelInfo.setNodeAPs(cachedNode.getNodeRegisters());

        return levelInfo;
    }
}
