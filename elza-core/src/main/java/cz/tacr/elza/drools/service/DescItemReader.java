package cz.tacr.elza.drools.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.drools.model.DescItem;
import cz.tacr.elza.drools.model.Level;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Class to read description items for the given levels
 *
 * @author Petr Pytelka
 *
 */
public class DescItemReader {
	/**
	 * Map of levels to be set
	 */
	Map<Level, ArrNode> items = new HashMap<>();

	private DescItemRepository descItemRepository;

	private ItemTypeRepository itemTypeRepository;

	private DescItemFactory descItemFactory;

	private NodeCacheService nodeCacheService;

	public DescItemReader(DescItemRepository descItemRepository,
						  ItemTypeRepository itemTypeRepository,
						  DescItemFactory descItemFactory,
						  NodeCacheService nodeCacheService)
	{
		this.descItemRepository = descItemRepository;
		this.itemTypeRepository = itemTypeRepository;
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
	public void read(ArrFundVersion version) {
        Set<RulItemType> descItemTypesForPackets = itemTypeRepository.findDescItemTypesForPackets();
        Set<RulItemType> descItemTypesForIntegers = itemTypeRepository.findDescItemTypesForIntegers();

		Collection<ArrNode> nodes = items.values();
		Set<Level> levels = items.keySet();

        Map<Integer, List<ArrDescItem>> descItemsMap = null;
        Map<Integer, CachedNode> cachedNodes = null;

        //if (version.getLockChange() == null) {
        //    Collection<Integer> nodeIds = new ArrayList<>(nodes.size());
        //    for (ArrNode node : nodes) {
        //        nodeIds.add(node.getNodeId());
        //    }
        //    cachedNodes = nodeCacheService.getNodes(nodeIds);
        //} else {
            List<ArrDescItem> descItems = descItemRepository.findByNodes(nodes, version.getLockChange());
            descItemsMap = ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());
        //}

        for (Level level : levels) {
            List<ArrDescItem> levelDescItems;
            //if (version.getLockChange() == null) {
            //    levelDescItems = cachedNodes.get(level.getNodeId()).getDescItems();
            //} else {
                levelDescItems = descItemsMap.get(level.getNodeId());
            //}
            List<DescItem> items = ModelFactory.createDescItems(levelDescItems,
            		descItemTypesForPackets, descItemTypesForIntegers, descItemFactory, version.getLockChange() == null);
            level.setDescItems(items);
        }

	}
}
