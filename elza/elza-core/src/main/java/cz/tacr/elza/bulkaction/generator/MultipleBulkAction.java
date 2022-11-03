package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkAction;
import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.bulkaction.generator.multiple.ActionConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TypeLevel;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Vícenásobná hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @since 29.06.2016
 */
public class MultipleBulkAction extends BulkAction {

	/**
	 * Size of batch for fetching child nodes from DB
	 */
	private final static int BATCH_CHILD_NODE_SIZE = 100;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_MULTIPLE";

    private List<Action> actions = new ArrayList<>();

	MultiActionConfig config;


	public MultipleBulkAction(MultiActionConfig multiActionConfig) {
		this.config = multiActionConfig;
	}

	/**
	 * Inicializace hromadné akce.
	 *
	 * @param bulkActionConfig
	 *            nastavení hromadné akce
	 * @param runContext
	 */
	@Override
	protected void init(ArrBulkActionRun bulkActionRun) {
		super.init(bulkActionRun);

		// initialize actions from configuration
		for (ActionConfig ac : config.getActions()) {
			Action a = appCtx.getBean(ac.getActionClass(), ac);
			// initialize each action after all properties are autowired
            a.init(this, bulkActionRun);
			actions.add(a);
		}

        try {
			/*
			// Yaml yaml = bulkActionConfig.getConfiguration(); //.getYaml();
			// List<String> actionCodes = yaml.getStringList("action");
			Object obj = bulkActionConfig.getConfiguration();
			List<String> actionCodes = new ArrayList<>();

			if (actionCodes.size() == 0) {
			    throw new IllegalArgumentException("Musí být definována alespoň jedna akce");
			}

			List<ActionType> actionTypes = actionCodes.stream().map(ActionType::valueOf).collect(Collectors.toList());
			*/
            /*
            for (int i = 0; i < actionTypes.size(); i++) {
                Action action = actionFactory.createNewAction(actionTypes.get(i), yaml.getSection("action." + i + "."));
                actions.add(action);
            }
            */

		} catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }


    @Override
	public void run(ActionRunContext runContext) {
		List<ArrNode> startingNodes = nodeRepository.findAllById(runContext.getInputNodeIds());

        // map of root nodes for action
        Map<ArrNode, LevelWithItems> nodeStartingLevels = new HashMap<>();
        // map of all nodes, including all parents
        Map<ArrNode, LevelWithItems> nodesWithItems = new HashMap<>();

        // prepare parent nodes
        for (ArrNode startingNode : startingNodes) {
            // read parents
			List<ArrLevel> levels = levelRepository.findAllParentsByNodeId(startingNode.getNodeId(), version.getLockChange(), true);

            LevelWithItems parentLevel = null;
            for(ArrLevel level: levels) {
                TreeNode tn = new TreeNode(level.getNodeId(), level.getPosition());
                LevelWithItems levelWithItems = prepareLevelWithItems(tn, parentLevel);

                nodesWithItems.put(level.getNode(), levelWithItems);
                parentLevel = levelWithItems;
            }
            // add starting node
            ArrLevel startingLevel = levelRepository.findByNodeAndDeleteChangeIsNull(startingNode);
            TreeNode tn = new TreeNode(startingLevel.getNodeId(), startingLevel.getPosition());

            LevelWithItems startingLevelWithItems = prepareLevelWithItems(tn, parentLevel);
            nodesWithItems.put(startingLevel.getNode(), startingLevelWithItems);

            nodeStartingLevels.put(startingNode, startingLevelWithItems);
        }

        // apply on all parentNodes
        for (Entry<ArrNode, LevelWithItems> entry : nodesWithItems.entrySet()) {
            ArrNode node = entry.getKey();
            // check if it is pure parent (not starting node)
            if(nodeStartingLevels.containsKey(node)) {
                continue;
            }

            LevelWithItems levelWithItems = entry.getValue();

			apply(levelWithItems, TypeLevel.PARENT);
        }

        // apply on all connected nodes
        for (ArrNode node : startingNodes) {

            LevelWithItems levelWithItems = nodeStartingLevels.get(node);
			Validate.notNull(levelWithItems);

            // read whole subtree
            Map<Integer, TreeNode> treeNodeMap = levelTreeCacheService.createTreeNodeMap(null, node.getNodeId());

            generate(levelWithItems, treeNodeMap);
        }

        if (multipleItemChangeContext != null) {
            multipleItemChangeContext.flush();
        }

        // Collect results
        Result result = new Result();
        for (Action action : actions) {
            ActionResult actionResult = action.getResult();
            if (actionResult != null) {
                result.addResults(actionResult);
            }
        }

        bulkActionRun.setResult(result);
    }

    /**
     * Load all description items for level and prepare bounding object
     * 
     * @param treeNode
     *            Level to be loaded
     * @param parentLevels
     *            Parent level to be set
     * @return Return loaded level
     */
    private LevelWithItems prepareLevelWithItems(final TreeNode treeNode, final LevelWithItems parentLevels) {
    	// we can load data from cache because we are running action on the recent node
        CachedNode cachedNode = nodeCacheService.getNode(treeNode.getId());
    	List<ArrDescItem> items = cachedNode.getDescItems();
        return new LevelWithItems(treeNode, parentLevels, items);
    }


    /**
     * Rekurzivní metody pro procházení JP ve stromu.
     * 
     * @param treeNodeMap
     *
     * @param node
     * @param parentLevel
     *            procházený uzel
     * @param parentNodeDescItems
     *            data předků
     */
    void generate(final LevelWithItems levelWithItems, Map<Integer, TreeNode> treeNodeMap) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);
            throw new BusinessException("Hromadná akce " + toString() + " byla přerušena.", ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
        }

        // apply on current node
		apply(levelWithItems, TypeLevel.CHILD);

        // apply on child nodes in batch
        TreeNode treeNode = treeNodeMap.get(levelWithItems.getNodeId());
        LinkedList<TreeNode> childNodes = treeNode.getChilds();
        //List<ArrLevel> childLevels = getChildren(level);

        BatchNodeProcessor bnp = new BatchNodeProcessor(this, BATCH_CHILD_NODE_SIZE, actions, levelWithItems,
                nodeCacheService,
                treeNodeMap);
        if (CollectionUtils.isNotEmpty(childNodes)) {
            for (TreeNode childNode : childNodes) {
                bnp.addItem(childNode);
            }
        }
        bnp.processAll();
    }

    /**
     * Aplikování akcí.
     *
     * @param node
     * @param items                 hodnoty atributů uzlu
     * @param typeLevel             typ uzlu
     * @param parentNodeDescItems   data předků
     */
    private void apply(LevelWithItems level, TypeLevel typeLevel) {
		try {
			for (Action action : actions) {
				action.apply(level, typeLevel);
			}
		} catch (Exception e) {
			throw e;
		}
    }

    @Override
	public String getName() {
		return "MultipleBulkAction";
    }
}
