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
import cz.tacr.elza.repository.DescItemTypeRepository;

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
	
	private DescItemTypeRepository descItemTypeRepository;

	private DescItemFactory descItemFactory;
	
	public DescItemReader(DescItemRepository descItemRepository, 
			              DescItemTypeRepository descItemTypeRepository,
			              DescItemFactory descItemFactory) 
	{
		this.descItemRepository = descItemRepository;
		this.descItemTypeRepository = descItemTypeRepository;
		this.descItemFactory = descItemFactory;
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
        Set<RulItemType> descItemTypesForPackets = descItemTypeRepository.findDescItemTypesForPackets();
        Set<RulItemType> descItemTypesForIntegers = descItemTypeRepository.findDescItemTypesForIntegers();
		
		Collection<ArrNode> nodes = items.values();
		Set<Level> levels = items.keySet();
		
        List<ArrDescItem> descItems = descItemRepository.findByNodes(nodes, version.getLockChange());

        Map<Integer, List<ArrDescItem>> descItemsMap =
                ElzaTools.createGroupMap(descItems, p -> p.getNode().getNodeId());

        for (Level level : levels) {
            List<ArrDescItem> levelDescItems = descItemsMap.get(level.getNodeId());
            List<DescItem> items = ModelFactory.createDescItems(levelDescItems,
            		descItemTypesForPackets, descItemTypesForIntegers, descItemFactory);
            level.setDescItems(items);
        }
		
	}
}
