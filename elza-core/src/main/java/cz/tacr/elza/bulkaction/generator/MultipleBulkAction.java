package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionInterruptedException;
import cz.tacr.elza.bulkaction.generator.multiple.Action;
import cz.tacr.elza.bulkaction.generator.multiple.ActionFactory;
import cz.tacr.elza.bulkaction.generator.multiple.ActionType;
import cz.tacr.elza.bulkaction.generator.multiple.TypeLevel;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.Result;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.utils.Yaml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Vícenásobná hromadná akce prochází strom otevřené verze archivní pomůcky a doplňuje u položek požadované atributy.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public class MultipleBulkAction extends BulkAction {

    @Autowired
    private ActionFactory actionFactory;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DescItemRepository descItemRepository;

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

        List<ArrNode> nodes = nodeRepository.findAll(inputNodeIds);

        Set<ArrNode> parentNodes = new TreeSet<>();

        Map<ArrNode, List<LevelWithItems>> nodeParentLevels = new HashMap<>();
        Map<ArrNode, LevelWithItems> nodeLevelWithItems = new HashMap<>();

        for (ArrNode node : nodes) {
            List<ArrLevel> levels = levelRepository.findAllParentsByNodeAndVersion(node, fundVersion);
            Collections.reverse(levels);

            List<LevelWithItems> levelWithItemsList = new ArrayList<>();

            for (ArrLevel level : levels) {
                LevelWithItems levelWithItems = new LevelWithItems(level);
                nodeLevelWithItems.put(level.getNode(), levelWithItems);
                levelWithItemsList.add(levelWithItems);
            }

            nodeParentLevels.put(node, levelWithItemsList);

            parentNodes.addAll(levels.stream().filter(level -> !node.equals(level.getNode())).map(ArrLevel::getNode).collect(Collectors.toList()));
        }

        ArrNode rootNode = fundVersion.getRootNode();

        for (ArrNode node : parentNodes) {
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            LevelWithItems levelWithItems = nodeLevelWithItems.get(node);
            List<ArrDescItem> descItems = loadDescItems(level);
            levelWithItems.descItems.addAll(descItems);
            apply(node, descItems, TypeLevel.PARENT, null);
        }

        for (ArrNode node : nodes) {
            ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, rootNode, null);
            Assert.notNull(level, "Level neexistuje, nodeId=" + node.getNodeId() + ", rootNodeId=" + rootNode.getNodeId());

            List<LevelWithItems> levelWithItemsList = nodeParentLevels.get(node);

            Map<ArrNode, List<ArrDescItem>> nodeDescItems = new LinkedHashMap<>();
            for (LevelWithItems levelWithItems : levelWithItemsList) {
                nodeDescItems.put(levelWithItems.level.getNode(), levelWithItems.descItems);
            }

            generate(node, level, nodeDescItems);
        }

        // Collect results
        Result result = new Result();
        for (Action action : actions) {
        	result.getResults().add(action.getResult());
        }

        bulkActionRun.setResult(result);
    }

    /**
     * Pomocná třída pro svázání levelu a hodnot atributů.
     */
    class LevelWithItems {
        final ArrLevel level;
        final List<ArrDescItem> descItems = new ArrayList<>();

        public LevelWithItems(final ArrLevel level) {
            this.level = level;
        }
    }

    /**
     * Rekurzivní metody pro procházení JP ve stromu.
     *
     * @param node
     * @param level procházený uzel
     * @param parentNodeDescItems data předků
     */
    private void generate(final ArrNode node, final ArrLevel level, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        if (bulkActionRun.isInterrupted()) {
            bulkActionRun.setState(cz.tacr.elza.api.ArrBulkActionRun.State.INTERRUPTED);
            throw new BulkActionInterruptedException("Hromadná akce " + toString() + " byla přerušena.");
        }

        List<ArrDescItem> descItems = loadDescItems(level);
        apply(node, descItems, TypeLevel.CHILD, parentNodeDescItems);

        List<ArrLevel> childLevels = getChildren(level);

        Map<ArrNode, List<ArrDescItem>> newNarentNodeDescItems = new LinkedHashMap<>(parentNodeDescItems);
        newNarentNodeDescItems.put(level.getNode(), descItems);

        for (ArrLevel childLevel : childLevels) {
            generate(childLevel.getNode(), childLevel, newNarentNodeDescItems);
        }
    }

    /**
     * Aplikování akcí.
     *
     * @param node
     * @param items                 hodnoty atributů uzlu
     * @param typeLevel             typ uzlu
     * @param parentNodeDescItems   data předků
     */
    private void apply(final ArrNode node, final List<ArrDescItem> items, final TypeLevel typeLevel, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        actions.stream().filter(action -> action.canApply(typeLevel))
                .forEach(action -> action.apply(node, items, parentNodeDescItems));
    }

    /**
     * Načtení hodnot uzlu.
     *
     * @param level uzel
     * @return  hodnoty uzlu
     */
    private List<ArrDescItem> loadDescItems(final ArrLevel level) {
        List<ArrDescItem> descItems = descItemRepository.findByNodeAndDeleteChangeIsNull(level.getNode());
        return itemService.loadData(descItems);
    }

    @Override
    public String toString() {
        return "MultipleBulkAction{" +
                "version=" + fundVersion +
                '}';
    }
}