package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractBatchLoader;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Reads supplied levels from node cache and pass them to {@link SectionOutputStream}.
 */
public class LevelInfoLoader extends AbstractBatchLoader<ArrLevel, LevelInfoImpl> {

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

            LevelInfoImpl levelInfo = createLevelInfo(level.getNodeId(), parentNodeId, cachedNode);
            entry.setResult(levelInfo);
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

    private static LevelInfoImpl createLevelInfo(Integer nodeId, Integer parentNodeId, CachedNode cachedNode) {
        Validate.notNull(nodeId);
        Validate.notNull(cachedNode);

        LevelInfoImpl levelInfo = new LevelInfoImpl(nodeId, parentNodeId);
        levelInfo.setNodeUuid(cachedNode.getUuid());
        levelInfo.setNodeAps(cachedNode.getNodeRegisters());
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems != null) {
        // sort items by item type and position
            descItems.stream().sorted((item1, item2) -> compareItems(item1, item2))
                    .forEachOrdered(levelInfo::addItem);
        }

        return levelInfo;
    }

    /**
     * Compare/Order items
     * 
     * @param item1
     * @param item2
     * @return
     */
    private static int compareItems(ArrDescItem item1, ArrDescItem item2) {
        RulItemType itemType1 = item1.getItemType();
        RulItemType itemType2 = item2.getItemType();
        int cmp = itemType1.getViewOrder().compareTo(itemType2.getViewOrder());
        if (cmp == 0) {
            if (itemType1.getUseSpecification() && itemType2.getUseSpecification()) {
                RulItemSpec itemSpec1 = item1.getItemSpec();
                RulItemSpec itemSpec2 = item2.getItemSpec();
                cmp = itemSpec1.getViewOrder().compareTo(itemSpec2.getViewOrder());
                if (cmp == 0) {
                    cmp = item1.getPosition().compareTo(item2.getPosition());
                }
            } else {
                cmp = item1.getPosition().compareTo(item2.getPosition());
            }
        }
        return cmp;
    }
}
