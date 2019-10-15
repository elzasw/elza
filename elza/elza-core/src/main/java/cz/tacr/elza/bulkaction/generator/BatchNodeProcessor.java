package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;

public class BatchNodeProcessor {
	
	final NodeCacheService nodeCacheService;
	
	final int batchChildNodeSize;
	final List<Action> actions;
	final LevelWithItems parentLevelWithItems;
	final MultipleBulkAction bulkAction;
	
	/**
	 * List for pending requests
	 */
	final List<ArrLevel> requests;

	public BatchNodeProcessor(MultipleBulkAction bulkAction,
			int batchChildNodeSize, List<Action> actions, LevelWithItems parentLevelWithItems,
			NodeCacheService nodeCacheService) {
		this.bulkAction = bulkAction;
		this.batchChildNodeSize = batchChildNodeSize;
		this.actions = actions;
		this.parentLevelWithItems = parentLevelWithItems;
		this.nodeCacheService = nodeCacheService;
		
		requests = new ArrayList<>(batchChildNodeSize); 
	}

	public void addItem(ArrLevel childLevel) {
		requests.add(childLevel);
		if(requests.size()>=batchChildNodeSize) {
			processAll();
		}
	}

	/**
	 * Process all pending requests and empty queue
	 */
	public void processAll() {
		// Check if empty
		if(requests.size()==0) {
			return;
		}
		
		// prepare nodeIds for cache request
		List<Integer> nodeIds = new ArrayList<>(requests.size());
		for(ArrLevel l: requests) {
			nodeIds.add(l.getNodeId());
		}
		// fetch nodes
		Map<Integer, RestoredNode> nodes = nodeCacheService.getNodes(nodeIds);
		
		// process nodes
		for(ArrLevel l: requests) {
			RestoredNode restoredNode = nodes.get(l.getNodeId());
			List<ArrDescItem> items = restoredNode.getDescItems();
			
			LevelWithItems childLevelWithItems = new LevelWithItems(l, parentLevelWithItems, items);
			
			bulkAction.generate(childLevelWithItems);
		}
		
		// clear queue
		requests.clear();
	}

}
