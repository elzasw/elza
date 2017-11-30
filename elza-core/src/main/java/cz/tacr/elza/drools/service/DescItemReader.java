package cz.tacr.elza.drools.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

/**
 * Class to read description items for the given levels
 *
 *
 */
public class DescItemReader {
	/**
	 * Map of levels to be set
	 */
	Map<Level, ArrNode> items = new HashMap<>();

	private final DescItemRepository descItemRepository;

	private final DescItemFactory descItemFactory;

	private final NodeCacheService nodeCacheService;

	private final ArrFundVersion version;

	public DescItemReader(ArrFundVersion version, DescItemRepository descItemRepository,
						  DescItemFactory descItemFactory,
						  NodeCacheService nodeCacheService)
	{
		this.version = version;
		this.descItemRepository = descItemRepository;
		this.descItemFactory = descItemFactory;
		this.nodeCacheService = nodeCacheService;
	}

	/**
	 * Add node to the reader
	 * @param level
	 * @param node
	 */
	public void add(Level level, ArrNode node) {
		items.put(level, node);
	}

	/**
	 * Read description items for all levels in the reader
	 * @param version Version of the fund
	 */
	public void read() {

		Collection<ArrNode> nodes = items.values();
		Set<Level> levels = items.keySet();

		// handle empty key set
		if (levels.size() == 0) {
			return;
		}

        Map<Integer, List<ArrDescItem>> descItemsMap = null;
		Map<Integer, RestoredNode> cachedNodes = null;

        if (version.getLockChange() == null) {
            Collection<Integer> nodeIds = new ArrayList<>(nodes.size());
            for (ArrNode node : nodes) {
                nodeIds.add(node.getNodeId());
            }
            cachedNodes = nodeCacheService.getNodes(nodeIds);
        } else {
			List<ArrDescItem> descItems = descItemRepository.findByNodesAndDeleteChange(nodes, version.getLockChange());
            descItemsMap = ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());
        }

        for (Level level : levels) {
            List<ArrDescItem> levelDescItems;
            if (version.getLockChange() == null) {
                levelDescItems = cachedNodes.get(level.getNodeId()).getDescItems();
            } else {
                levelDescItems = descItemsMap.get(level.getNodeId());
            }
			List<DescItem> items = ModelFactory.createDescItems(levelDescItems, descItemFactory);
            level.setDescItems(items);
        }

	}
}
