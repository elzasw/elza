package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.bulkaction.generator.multiple.ActionFactory;
import cz.tacr.elza.bulkaction.generator.multiple.ActionType;
import cz.tacr.elza.bulkaction.generator.multiple.TypeLevel;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.utils.Yaml;

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
    private ActionFactory actionFactory;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DescItemRepository descItemRepository;
    
    @Autowired 
    private NodeCacheService nodeCacheService;

    /**
     * Identifikátor hromadné akce
     */
    public static final String TYPE = "GENERATOR_MULTIPLE";

    /**
     * Verze archivní pomůcky
     */
    private ArrFundVersion fundVersion;

    /**
     * Změna
     */
    private ArrChange change;

    private List<Action> actions = new ArrayList<>();

    private ArrBulkActionRun bulkActionRun;

    /**
     * Inicializace hromadné akce.
     * @param bulkActionConfig nastavení hromadné akce
     * @param bulkActionRun
     */
    private void init(final BulkActionConfig bulkActionConfig, final ArrBulkActionRun bulkActionRun) {
        try {
            Yaml yaml = bulkActionConfig.getYaml();
            List<String> actionCodes = yaml.getStringList("action");

            if (actionCodes.size() == 0) {
                throw new IllegalArgumentException("Musí být definována alespoň jedna akce");
            }

            List<ActionType> actionTypes = actionCodes.stream().map(ActionType::valueOf).collect(Collectors.toList());

            for (int i = 0; i < actionTypes.size(); i++) {
                Action action = actionFactory.createNewAction(actionTypes.get(i), yaml.getSection("action." + i + "."));
                actions.add(action);
            }

        } catch (Yaml.YAMLNotInitializedException | Yaml.YAMLKeyNotFoundException e) {
            throw new IllegalArgumentException(e);
        }

        this.bulkActionRun = bulkActionRun;
        this.fundVersion = bulkActionRun.getFundVersion();
        this.change = bulkActionRun.getChange();
    }


    @Override
    @Transactional
    public void run(final List<Integer> inputNodeIds,
                    final BulkActionConfig bulkAction,
                    final ArrBulkActionRun bulkActionRun) {
        init(bulkAction, bulkActionRun);

        List<ArrNode> startingNodes = nodeRepository.findAll(inputNodeIds);

        // map of root nodes for action
        Map<ArrNode, LevelWithItems> nodeStartingLevels = new HashMap<>();
        // map of all nodes, including all parents
        Map<ArrNode, LevelWithItems> nodesWithItems = new HashMap<>();

        ArrNode rootNode = fundVersion.getRootNode();
        // prepare parent nodes
        for (ArrNode startingNode : startingNodes) {
            // read parents
            List<ArrLevel> levels = levelRepository.findAllParentsByNodeAndVersion(startingNode, fundVersion);
            Collections.reverse(levels);

            LevelWithItems parentLevel = null;
            for(ArrLevel level: levels) {
                LevelWithItems levelWithItems = prepareLevelWithItems(level, parentLevel);

                nodesWithItems.put(level.getNode(), levelWithItems);
                parentLevel = levelWithItems;
            }
            // add starting node
            ArrLevel startingLevel = levelRepository.findNodeInRootTreeByNodeId(startingNode, rootNode, null);
            LevelWithItems startingLevelWithItems = prepareLevelWithItems(startingLevel, parentLevel);
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

            apply(node, levelWithItems.descItems, TypeLevel.PARENT, levelWithItems.getParent());
        }

        // apply on all connected nodes
        for (ArrNode node : startingNodes) {

            LevelWithItems levelWithItems = nodeStartingLevels.get(node);
            Assert.notNull(levelWithItems);

            generate(levelWithItems);
        }

        // Collect results
        Result result = new Result();
        for (Action action : actions) {
            result.getResults().add(action.getResult());
        }

        bulkActionRun.setResult(result);
    }

    /**
     * Load all description items for level and prepare bounding object
     * @param level Level to be loaded
     * @param parentLevels Parent level to be set
     * @return Return loaded level
     */
    private LevelWithItems prepareLevelWithItems(final ArrLevel level, final LevelWithItems parentLevels) {
    	// we can load data from cache because we are running action on the recent node
    	CachedNode cachedNode = nodeCacheService.getNode(level.getNodeId());
    	List<ArrDescItem> items = cachedNode.getDescItems();
        return new LevelWithItems(level, parentLevels, items);
    }


    /**
     * Rekurzivní metody pro procházení JP ve stromu.
     *
     * @param node
     * @param level procházený uzel
     * @param parentNodeDescItems data předků
     */
    void generate(final LevelWithItems levelWithItems) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(ArrBulkActionRun.State.INTERRUPTED);
            throw new BusinessException("Hromadná akce " + toString() + " byla přerušena.", ArrangementCode.BULK_ACTION_INTERRUPTED).set("code", bulkActionRun.getBulkActionCode());
        }

        // apply on current node
        LevelWithItems parentLevel = levelWithItems.getParent();
        ArrLevel level = levelWithItems.getLevel();
        ArrNode node = level.getNode();

        apply(node, levelWithItems.descItems, TypeLevel.CHILD, parentLevel);

        // apply on child nodes in batch
        List<ArrLevel> childLevels = getChildren(level);
        
        BatchNodeProcessor bnp = new BatchNodeProcessor(this, BATCH_CHILD_NODE_SIZE, actions, levelWithItems, nodeCacheService); 
        for (ArrLevel childLevel : childLevels) {
        	bnp.addItem(childLevel);
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
    private void apply(final ArrNode node, final List<ArrDescItem> items, final TypeLevel typeLevel, final LevelWithItems parentLevelWithItems) {
        actions.stream().filter(action -> action.canApply(typeLevel))
        .forEach(action -> action.apply(node, items, parentLevelWithItems));
    }

    /**
     * Načtení hodnot uzlu.
     *
     * @param level uzel
     * @return  hodnoty uzlu
     */
    private List<ArrDescItem> loadDescItems(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode());
        itemService.loadData(descItems);
        return descItems;
    }

    @Override
    public String toString() {
        return "MultipleBulkAction{" +
                "version=" + fundVersion +
                '}';
    }
}
