package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Akce na počítání Evidenčních jednotek.
 *
 * @author Martin Šlapa
 * @since 01.07.2016
 */
@Component
@Scope("prototype")
public class UnitCountAction extends Action {

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Typy
     */
    private Map<String, RulItemType> itemTypes = new HashMap<>();

    /**
     * Specifikace
     */
    private Map<String, RulItemSpec> itemSpecs = new HashMap<>();

    /**
     * Počet EJ
     */
    private Integer count = 0;

    /**
     * Seznam ignorovaných uzlů, které byly již započítané.
     */
    private Set<Integer> ignoredNodeId = new HashSet<>();

    /**
     * Již zapracované obaly
     */
    private Set<String> storageNumbers = new HashSet<>();

    UnitCountAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        String outputType = config.getString("output_type", null);
        outputItemType = findItemType(outputType);
        checkValidDataType(outputItemType, "INT");

        loadTypeAndSpec("find_1");
        loadTypeAndSpec("under");

        loadTypeAndSpec("find_2");
        loadTypeAndSpec("find_3");
        loadType("value");

        loadTypeAndSpec("find_4");
        loadType("value2");

        loadTypeAndSpec("unit_extra");
        loadType("unit_extra_count");

        loadTypeAndSpec("exclude_hierarchical");


        loadType("extra");

    }

    /**
     * Načtení typu a specifikace podle kódu.
     *
     * @param code kódy typu a specifikace jako řetězec oddělený mezerou
     */
    protected void loadTypeAndSpec(final String code) {
        String attribute = config.getString(code, null);
        if (attribute == null) {
            throw new IllegalArgumentException("Neplatný atribut: find_type");
        }
        String[] split = attribute.split(" ");
        if (split.length != 2) {
            throw new IllegalArgumentException("Neplatný atribut: musí obsahovat kód typu a specifikace");
        }

        RulItemType type = findItemType(split[0]);
        RulItemSpec spec = findItemSpec(split[1]);

        if (!spec.getItemType().equals(type)) {
            throw new IllegalArgumentException("Neplatný atribut: specifikace nepatří pod typ");
        }

        itemTypes.put(code, type);
        itemSpecs.put(code, spec);
    }

    /**
     * Načtení typu podle kódu.
     *
     * @param code kód atributu
     */
    protected void loadType(final String code) {
        String attribute = config.getString(code, null);
        RulItemType type = findItemType(attribute);
        itemTypes.put(code, type);
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {

        // Jednotlivost přímo pod sérii
        itemUnderType(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);

        // Logická složka
        isType1(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);

        // Množstevní EJ
        isType2(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);

        // S uvedením EJ
        isType3(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);
    }

    /**
     * Počítání podle "S uvedením EJ".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    private void isType3(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        boolean isFind4 = isItem(items, "find_4");
        boolean isFind1 = isItem(items, "find_1");

        if (!isFind4 && !isFind1) {
            return;
        }

        if (isFind1) {
            count++;
        } else {
            boolean count = !hasIgnoredParent(parentNodeDescItems);

            if (count) {
                countItemValue2(items);
                countExtraItem(items);
                ignoredNodeId.add(node.getNodeId());
            }
        }


    }

    /**
     * Počítání podle "Množstevní EJ".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    private void isType2(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        boolean isFind = isItem(items, "find_3");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentNodeDescItems);

        if (count) {
            countItemValue(items);
            countExtraItem(items);
            ignoredNodeId.add(node.getNodeId());
        }
    }

    /**
     * Připočítání položek podle typu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countItemValue2(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("value2");

        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                count += ((ArrItemInt) item.getItem()).getValue();
            }
        }
    }

    /**
     * Připočítání položek podle typu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countItemValue(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("value");

        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                ArrPacket packet = ((ArrItemPacketRef) item.getItem()).getPacket();
                String storageNumber = packet.getStorageNumber();
                if (!storageNumbers.contains(storageNumber)) {
                    count++;
                    storageNumbers.add(storageNumber);
                }
            }
        }
    }

    /**
     * Počítání podle "Logická složka".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    private void isType1(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {

        boolean isFind = isItem(items, "find_2");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentNodeDescItems);

        if (count) {
            countItem(items);
            countExtraItem(items);
            ignoredNodeId.add(node.getNodeId());
        }
    }

    /**
     * Připočítání položek podle extra typu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countExtraItem(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("extra");
        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                count += ((ArrItemInt) item.getItem()).getValue();
            }
        }
    }

    /**
     * Detekce jednotlivosti pod sérií.
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    public void itemUnderType(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {

        boolean isFind = isItem(items, "find_1");

        if (!isFind) {
            return;
        }

        boolean onlyItemCount = hasIgnoredParent(parentNodeDescItems);
        boolean isUnder = isUnder(parentNodeDescItems);

        if (isUnder || onlyItemCount) {
            countItem(items);

            if (isUnder) {
                ignoredNodeId.add(node.getNodeId());
            }
        }

    }

    /**
     * Spočítání EJ na uzlu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countItem(final List<ArrDescItem> items) {
        int unitExtraCount = 0;
        RulItemType unitExtraCountType = itemTypes.get("unit_extra_count");
        RulItemType unitExtraType = itemTypes.get("unit_extra");
        RulItemSpec unitExtraSpec = itemSpecs.get("unit_extra");

        boolean isExtra = false;
        for (ArrDescItem item : items) {
            if (item.getItemType().equals(unitExtraCountType)) {
                unitExtraCount += ((ArrItemInt) item.getItem()).getValue();
            }

            if (item.getItemType().equals(unitExtraType)
                    && item.getItemSpec() != null
                    && item.getItemSpec().equals(unitExtraSpec)) {
                isExtra = true;
            }
        }

        if (isExtra) {
            count += unitExtraCount;
        }
    }

    /**
     * Jsou mezi ignorovanými některý z rodičů?
     *
     * @param parentNodeDescItems   rodiče uzlu
     * @return  jsou?
     */
    protected boolean hasIgnoredParent(final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        if (parentNodeDescItems != null && parentNodeDescItems.values().size() > 0) {
            ArrayList<ArrNode> nodes = new ArrayList<>(parentNodeDescItems.keySet());
            for (ArrNode arrNode : nodes) {
                if (ignoredNodeId.contains(arrNode.getNodeId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Je uzel pod typem?
     *
     * @param parentNodeDescItems rodiče uzlu
     * @return je?
     */
    protected boolean isUnder(final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        if (parentNodeDescItems != null && parentNodeDescItems.values().size() > 0) {
            ArrayList<List<ArrDescItem>> lists = new ArrayList<>(parentNodeDescItems.values());
            List<ArrDescItem> parentItems = lists.get(lists.size() - 1); // hodnoty posledního elementu (předchůdce)
            for (ArrDescItem parentItem : parentItems) {
                if (parentItem.getItemType().equals(itemTypes.get("under")) &&
                        parentItem.getItemSpec() != null && parentItem.getItemSpec().equals(itemSpecs.get("under"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Existuje v uzlu daný typ?
     *
     * @param items seznam hodnot uzlu
     * @param itemCode  typ atributu
     * @return
     */
    protected boolean isItem(final List<ArrDescItem> items, final String itemCode) {
        for (ArrDescItem item : items) {
            if (item.getItemType().equals(itemTypes.get(itemCode)) &&
                    item.getItemSpec() != null && item.getItemSpec().equals(itemSpecs.get(itemCode))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canApply(final TypeLevel typeLevel) {
        if (typeLevel.equals(TypeLevel.PARENT) && applyParents) {
            return true;
        }

        if (typeLevel.equals(TypeLevel.CHILD) && applyChildren) {
            return true;
        }

        return false;
    }

    @Override
    public ActionResult getResult() {
        UnitCountActionResult result = new UnitCountActionResult();
        result.setItemType(outputItemType.getCode());
        result.setCount(count);
        return result;
    }

}
