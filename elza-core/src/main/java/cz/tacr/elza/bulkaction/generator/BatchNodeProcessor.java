package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrDescItem;
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
    final List<TreeNode> requests;

    final private Map<Integer, TreeNode> treeNodeMap;

	public BatchNodeProcessor(MultipleBulkAction bulkAction,
			int batchChildNodeSize, List<Action> actions, LevelWithItems parentLevelWithItems,
                              NodeCacheService nodeCacheService, Map<Integer, TreeNode> treeNodeMap) {
		this.bulkAction = bulkAction;
		this.batchChildNodeSize = batchChildNodeSize;
		this.actions = actions;
		this.parentLevelWithItems = parentLevelWithItems;
		this.nodeCacheService = nodeCacheService;
        this.treeNodeMap = treeNodeMap;
		requests = new ArrayList<>(batchChildNodeSize); 
	}

    public void addItem(TreeNode childNode) {
        requests.add(childNode);
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
        for (TreeNode n : requests) {
            nodeIds.add(n.getId());
		}
		// fetch nodes
		Map<Integer, RestoredNode> nodes = nodeCacheService.getNodes(nodeIds);
		
		// process nodes
        for (TreeNode n : requests) {
            RestoredNode restoredNode = nodes.get(n.getId());
			List<ArrDescItem> items = restoredNode.getDescItems();
			
            LevelWithItems childLevelWithItems = new LevelWithItems(n, parentLevelWithItems, items);
			
            bulkAction.generate(childLevelWithItems, treeNodeMap);
		}
		
		// clear queue
		requests.clear();
	}

}
