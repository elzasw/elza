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
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.service.DescriptionItemService;
import cz.tacr.elza.service.StructObjValueService;
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

	private final StructuredItemRepository structItemRepos;

    private final DescriptionItemService descItemService;

    private final StructObjValueService structObjService;

	public DescItemReader(ArrFundVersion version, DescItemRepository descItemRepository,
						  DescItemFactory descItemFactory,
						  NodeCacheService nodeCacheService,
						  final StructuredItemRepository structItemRepos,
                          DescriptionItemService descItemService,
                          StructObjValueService structObjService)
	{
		this.version = version;
		this.descItemRepository = descItemRepository;
		this.descItemFactory = descItemFactory;
		this.nodeCacheService = nodeCacheService;
		this.structItemRepos = structItemRepos;
        this.descItemService = descItemService;
        this.structObjService = structObjService;
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
        if (levels.isEmpty()) {
			return;
		}

        Map<Integer, List<ArrDescItem>> descItemsMap = null;
		Map<Integer, RestoredNode> cachedNodes = null;

        // Check if load from cache
        if (version.getLockChange() == null) {
            Collection<Integer> nodeIds = new ArrayList<>(nodes.size());
            for (ArrNode node : nodes) {
                nodeIds.add(node.getNodeId());
            }
            cachedNodes = nodeCacheService.getNodes(nodeIds);

            for (Level level : levels) {
                List<ArrDescItem> levelDescItems = cachedNodes.get(level.getNodeId()).getDescItems();
                List<DescItem> items = ModelFactory.createDescItems(levelDescItems, descItemFactory, structObjService);
                level.setDescItems(items);
            }
        } else {
            // Load from DB
			List<ArrDescItem> descItems = descItemService.findByNodesAndDeleteChange(nodes, version.getLockChange());

            descItemsMap = ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());

            for (Level level : levels) {
                List<ArrDescItem> levelDescItems = descItemsMap.get(level.getNodeId());
                List<DescItem> items = ModelFactory.createDescItems(levelDescItems, descItemFactory, structObjService);
                level.setDescItems(items);
            }
        }


	}
}
