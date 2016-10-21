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
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private Map<String, Integer> countMap = new HashMap<>();

    /**
     * Seznam ignorovaných uzlů, které byly již započítané.
     */
    private Set<Integer> ignoredNodeId = new HashSet<>();

    /**
     * Již zapracované obaly
     */
    private Set<String> storageNumbers = new HashSet<>();

    // typy a názvy sloupců tabulky
    private String outputTableColumn1;
    private String outputTableColumn1Type;
    private String outputTableColumn2;
    private String outputTableColumn2Type;

    UnitCountAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        String outputType = config.getString("output_type", null);
        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "JSON_TABLE");

        outputTableColumn1 = config.getString("output_table_column1", null);
        checkValidateParam("output_table_column1", outputTableColumn1);
        outputTableColumn1Type = config.getString("output_table_column1_type", null);
        checkValidateParam("output_table_column1_type", outputTableColumn1Type);
        outputTableColumn2 = config.getString("output_table_column2", null);
        checkValidateParam("output_table_column2", outputTableColumn2);
        outputTableColumn2Type = config.getString("output_table_column2_type", null);
        checkValidateParam("output_table_column2_type", outputTableColumn2Type);

        List<ElzaColumn> columnsDefinition = outputItemType.getColumnsDefinition();
        Map<String, ElzaColumn> outputColumns = columnsDefinition.stream().collect(Collectors.toMap(ElzaColumn::getCode, Function.identity()));

        validateColumn(outputTableColumn1, outputTableColumn1Type, outputColumns);
        validateColumn(outputTableColumn2, outputTableColumn2Type, outputColumns);

        loadType("UNIT_TYPE");

        loadTypeAndSpec("ITEM");

        loadTypeAndSpec("SERIES");

        loadTypeAndSpec("KTT");
        loadType("KTT_count");

        loadTypeAndSpec("FOLDER_LOGICAL");

        loadTypeAndSpec("FOLDER_UNITS");

        loadTypeAndSpec("FOLDER_SINGLE_TYPE");
        loadType("FOLDER_SINGLE_TYPE_count");

        loadTypeAndSpec("FOLDER_UNITS");
        loadType("STORAGE");

        loadType("EXTRA_UNITS");

    }

    /**
     * Ověření vyplnění parametru - sloupce v tabulce.
     *
     * @param paramName         název parametru
     * @param outputTableColumn sloupec v tabulce
     */
    private void checkValidateParam(final String paramName, final String outputTableColumn) {
        Assert.hasText(outputTableColumn, "Nebyl nastaven parametr: " + paramName);
    }

    /**
     * Validace sloupce z definice typu atributu.
     *
     * @param outputTableColumn     název sloupce
     * @param outputTableColumnType typ sloupce
     * @param outputColumns         mapa sloupců
     */
    protected void validateColumn(final String outputTableColumn,
                                  final String outputTableColumnType,
                                  final Map<String, ElzaColumn> outputColumns) {
        ElzaColumn column = outputColumns.get(outputTableColumn);
        if (column == null) {
            throw new IllegalArgumentException("Atribut " + outputItemType.getCode() + " nemá sloupec " + outputTableColumn);
        }
        ElzaColumn.DataType dataType = ElzaColumn.DataType.valueOf(outputTableColumnType);
        if (!column.getDataType().equals(dataType)) {
            throw new IllegalArgumentException("Atribut " + outputItemType.getCode()
                    + " má sloupec " + column.getCode() + " jiného datového typu (" + outputItemType.getDataType().getCode()
                    + "), než je nastaveno (" + dataType.name() + ").");
        }
    }

    /**
     * Načtení typu a specifikace podle kódu.
     *
     * @param code kódy typu a specifikace jako řetězec oddělený mezerou
     */
    protected void loadTypeAndSpec(final String code) {
        String attribute = config.getString(code, null);
        if (attribute == null) {
            throw new IllegalArgumentException("Neplatný atribut: " + attribute);
        }
        String[] split = attribute.split(" ");
        if (split.length != 2) {
            throw new IllegalArgumentException("Neplatný atribut: musí obsahovat kód typu a specifikace");
        }

        RulItemType type = findItemType(split[0], code);
        RulItemSpec spec = findItemSpec(split[1]);

        if (!spec.getItemType().equals(type)) {
            throw new IllegalArgumentException("Neplatný atribut: specifikace nepatří pod typ");
        }

        itemTypes.put(code, type);
        itemSpecs.put(code, spec);
    }

    /**
     * Přičtení počtu podle typu.
     *
     * @param type typ sumy
     * @param inc  počet o který se suma navyšuje
     */
    private void addToCount(final String type, final Integer inc) {
        Assert.isTrue(inc >= 0, "Číslo nemůže být záporné");
        Integer sum = countMap.get(type);
        if (sum == null) {
            countMap.put(type, inc);
        } else {
            countMap.put(type, sum + inc);
        }
    }

    /**
     * Načtení typu podle kódu.
     *
     * @param code kód atributu
     */
    protected void loadType(final String code) {
        String attribute = config.getString(code, null);
        RulItemType type = findItemType(attribute, code);
        itemTypes.put(code, type);
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {

        // Jednotlivost přímo pod Sérii
        itemUnderType(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems, "SERIES");

        // Jednotlivost přímo pod Logickou složkou
        itemUnderType(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems, "FOLDER_LOGICAL");

        // Množstevní EJ
        isTypeEJ1(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);

        // S uvedením EJ
        isTypeEJ2(node, items, (LinkedHashMap<ArrNode, List<ArrDescItem>>) parentNodeDescItems);
    }

    /**
     * Počítání podle "S uvedením EJ".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    private void isTypeEJ2(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        boolean isFind = isItem(items, "FOLDER_SINGLE_TYPE");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentNodeDescItems);


        if (count) {
            countItemValue2(items);
            countExtraUnit(items);
            ignoredNodeId.add(node.getNodeId());
        }

    }

    /**
     * Počítání podle "Množstevní EJ".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     */
    private void isTypeEJ1(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        boolean isFind = isItem(items, "FOLDER_UNITS");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentNodeDescItems);

        if (count) {
            countItemValue(items);
            countExtraUnit(items);
            ignoredNodeId.add(node.getNodeId());
        }
    }

    /**
     * Připočítání položek podle typu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countItemValue2(final List<ArrDescItem> items) {
        RulItemType countType = itemTypes.get("FOLDER_SINGLE_TYPE_count");
        RulItemSpec countSpec = itemSpecs.get("FOLDER_SINGLE_TYPE");

        for (ArrDescItem item : items) {
            if (countType.equals(item.getItemType())) {
                addToCount(countSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
            }
        }
    }

    /**
     * Připočítání položek podle typu.
     *
     * @param items seznam hodnot uzlu
     */
    private void countItemValue(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("STORAGE");

        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                ArrPacket packet = ((ArrItemPacketRef) item.getItem()).getPacket();
                String storageNumber = packet.getStorageNumber();
                if (!storageNumbers.contains(storageNumber)) {
                    String shortcut = packet.getPacketType().getShortcut();
                    addToCount(shortcut, 1);
                    storageNumbers.add(storageNumber);
                }
            }
        }
    }

    /**
     * Připočítání položek podle extra unit.
     *
     * @param items seznam hodnot uzlu
     */
    private void countExtraUnit(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("EXTRA_UNITS");
        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                RulItemSpec itemSpec = item.getItemSpec();
                addToCount(itemSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
            }
        }
    }

    /**
     * Připočítání položek podle KKT.
     *
     * @param items seznam hodnot uzlu
     */
    private void countKkt(final List<ArrDescItem> items) {
        RulItemType extraType = itemTypes.get("KTT_count");
        RulItemSpec extraSpec = itemSpecs.get("KTT");

        for (ArrDescItem item : items) {
            if (item.getItemType().equals(extraType)) {
                addToCount(extraSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
            }
        }
    }

    /**
     * Detekce jednotlivosti pod sérií.
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentNodeDescItems rodiče uzlu
     * @param type                typ, pod kterým hledáme (z předpisu akce)
     */
    public void itemUnderType(final ArrNode node, final List<ArrDescItem> items, final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems, final String type) {

        // existuje typ?
        boolean found = isItem(items, "ITEM");

        if (!found) {
            // pokud ne, není co počítat
            return;
        }

        boolean onlyItemCount = hasIgnoredParent(parentNodeDescItems);
        boolean isUnder = isUnder(parentNodeDescItems, type);

        if (isUnder || onlyItemCount) {

            if (isItem(items, "KTT")) {
                countKkt(items);
            }

            RulItemSpec item = itemSpecs.get("ITEM");
            addToCount(item.getShortcut(), 1);

            if (isUnder) {
                ignoredNodeId.add(node.getNodeId());
            }
        }

    }

    /**
     * Jsou mezi ignorovanými některý z rodičů?
     *
     * @param parentNodeDescItems rodiče uzlu
     * @return jsou?
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
     * @param type typ, pod kterým hledáme (z předpisu akce)
     * @return je?
     */
    protected boolean isUnder(final LinkedHashMap<ArrNode, List<ArrDescItem>> parentNodeDescItems, final String type) {
        if (parentNodeDescItems != null && parentNodeDescItems.values().size() > 0) {
            ArrayList<List<ArrDescItem>> lists = new ArrayList<>(parentNodeDescItems.values());
            List<ArrDescItem> parentItems = lists.get(lists.size() - 1); // hodnoty posledního elementu (předchůdce)
            RulItemType itemType = itemTypes.get(type);
            RulItemSpec itemSpec = itemSpecs.get(type);
            for (ArrDescItem parentItem : parentItems) {
                if (parentItem.getItemType().equals(itemType) &&
                        parentItem.getItemSpec() != null && parentItem.getItemSpec().equals(itemSpec)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Existuje v uzlu daný typ?
     *
     * @param items    seznam hodnot uzlu
     * @param itemCode typ atributu
     * @return true podle item existuje mezi hodnotami atributů
     */
    protected boolean isItem(final List<ArrDescItem> items, final String itemCode) {
        RulItemType itemType = itemTypes.get(itemCode);
        RulItemSpec itemSpec = itemSpecs.get(itemCode);
        for (ArrDescItem item : items) {
            if (item.getItemType().equals(itemType) &&
                    item.getItemSpec() != null && item.getItemSpec().equals(itemSpec)) {
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
        ElzaTable table = new ElzaTable();

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            Map.Entry<String, String> key = new AbstractMap.SimpleEntry<>(outputTableColumn1, entry.getKey());
            Map.Entry<String, String> value = new AbstractMap.SimpleEntry<>(outputTableColumn2, entry.getValue().toString());
            table.addRow(new ElzaRow(key, value));
        }

        result.setTable(table);
        return result;
    }

}
