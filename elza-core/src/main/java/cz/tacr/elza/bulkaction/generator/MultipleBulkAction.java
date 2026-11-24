package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.BulkActionTransactional;
import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.bulkaction.generator.multiple.ActionConfig;
import cz.tacr.elza.bulkaction.generator.multiple.TypeLevel;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.dataexchange.output.filters.NodeRestrictionEvaluator;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.service.LevelTreeCacheService;
import cz.tacr.elza.service.LevelTreeCacheService.NodeHierarchy;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.output.NodeRestrictionEvaluatorFactory;

/**
 * Vícenásobná hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @since 29.06.2016
 */
public class MultipleBulkAction extends BulkActionTransactional {

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

    @Autowired
    private NodeRestrictionEvaluatorFactory restrictionEvaluatorFactory;

    private List<Action> actions = new ArrayList<>();

    /**
     * Filter of the output the action is run for, null when the action processes all levels
     */
    private LevelOutputFilter levelOutputFilter;

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
	protected void init(ActionRunContext runContext) {
		super.init(runContext);

		// initialize actions from configuration
		for (ActionConfig ac : config.getActions()) {
			Action a = appCtx.getBean(ac.getActionClass(), ac);
			// initialize each action after all properties are autowired
            a.init(this, bulkActionRun);
			actions.add(a);
		}

        String outputFilterCode = bulkActionRun.getOutputFilterCode();
        if (outputFilterCode != null) {
            Integer ruleSetId = getFondsVersion().getRuleSetId();
            NodeRestrictionEvaluator evaluator = restrictionEvaluatorFactory.create(ruleSetId, outputFilterCode);
            levelOutputFilter = new LevelOutputFilter(evaluator);
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
        // parent nodes hidden by the output filter
        Set<ArrNode> hiddenNodes = new HashSet<>();

        // prepare parent nodes
        for (ArrNode startingNode : startingNodes) {
            // read parents
			List<ArrLevel> levels = levelRepository.findAllParentsByNodeId(startingNode.getNodeId(), getFondsVersion().getLockChange(), true);

            LevelWithItems parentLevel = null;
            for(ArrLevel level: levels) {
                TreeNode tn = new TreeNode(level.getNodeId(), level.getPosition());
                LevelWithItems levelWithItems = prepareLevelWithItems(tn, parentLevel);

                if (levelOutputFilter != null) {
                    LevelWithItems visibleLevel = levelOutputFilter.filter(levelWithItems);
                    if (visibleLevel != null) {
                        levelWithItems = visibleLevel;
                    } else {
                        // a hidden parent is not passed to the actions; the evaluator remembers it
                        // and hides its descendants, including the starting node
                        hiddenNodes.add(level.getNode());
                    }
                }

                nodesWithItems.put(level.getNode(), levelWithItems);
                parentLevel = levelWithItems;
            }
            // add starting node
            ArrLevel startingLevel = levelRepository.findByNodeAndDeleteChangeIsNull(startingNode);
            TreeNode tn = new TreeNode(startingLevel.getNodeId(), startingLevel.getPosition());

            // the starting node is filtered later together with its subtree
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
            if (hiddenNodes.contains(node)) {
                continue;
            }

            LevelWithItems levelWithItems = entry.getValue();

			apply(levelWithItems, TypeLevel.PARENT);
        }

        // apply on all connected nodes
        for (ArrNode node : startingNodes) {

            LevelWithItems levelWithItems = nodeStartingLevels.get(node);
			Objects.requireNonNull(levelWithItems);

            // read whole subtree
            NodeHierarchy treeNode = levelTreeCacheService.createTreeNodeMap(null, node.getNodeId());

            generate(levelWithItems, treeNode.getNodes());
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
    void generate(final LevelWithItems level, Map<Integer, TreeNode> treeNodeMap) {
        if (interrupt) {
			throw new BulkActionInterruptedException("The action was interrupted");
        }

        LevelWithItems levelWithItems = level;
        if (levelOutputFilter != null) {
            levelWithItems = levelOutputFilter.filter(level);
            if (levelWithItems == null) {
                // the filter hides the level with its whole subtree
                return;
            }
        }

        // apply on current node
		apply(levelWithItems, TypeLevel.CHILD);

        // apply on child nodes in batch
        TreeNode treeNode = treeNodeMap.get(levelWithItems.getNodeId());
        List<TreeNode> childNodes = treeNode.getChildren();
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
