package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Akce na počítání Evidenčních jednotek.
 *
 * @author Martin Šlapa
 * @author Petr Pytelka
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

    /**
     * Name of column where to store Unit type
     */
    private String outputColumnUnitName;
    
    /**
     * Name of column where to store Unit count
     */
    private String outputColumnUnitCount;

    UnitCountAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        String outputType = config.getString("output_type", null);
        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "JSON_TABLE");

        outputColumnUnitName = config.getString("output_column_unit_name", null);
        checkValidateParam("output_column_unit_name", outputColumnUnitName);
        outputColumnUnitCount = config.getString("output_column_unit_value", null);
        checkValidateParam("output_column_unit_value", outputColumnUnitCount);

        List<ElzaColumn> columnsDefinition = outputItemType.getColumnsDefinition();
        Map<String, ElzaColumn> outputColumns = columnsDefinition.stream().collect(Collectors.toMap(ElzaColumn::getCode, Function.identity()));

        validateColumn(outputColumnUnitName, ElzaColumn.DataType.TEXT,  outputColumns);
        validateColumn(outputColumnUnitCount, ElzaColumn.DataType.INTEGER, outputColumns);

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
     * @param expectedColumnType 	očekávaný typ sloupce
     * @param outputColumns         mapa sloupců
     */
    protected void validateColumn(final String outputTableColumn,
                                  final ElzaColumn.DataType expectedColumnType,
                                  final Map<String, ElzaColumn> outputColumns) {
        ElzaColumn column = outputColumns.get(outputTableColumn);
        if (column == null) {
            throw new IllegalArgumentException("Atribut " + outputItemType.getCode() + " nemá sloupec " + outputTableColumn);
        }
        if (!column.getDataType().equals(expectedColumnType)) {
            throw new IllegalArgumentException("Atribut " + outputItemType.getCode()
                    + " má sloupec " + column.getCode() + " jiného datového typu (" + outputItemType.getDataType().getCode()
                    + "), než je nastaveno (" + expectedColumnType.name() + ").");
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
    public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {

        // Jednotlivost přímo pod Sérii
        itemUnderType(node, items, parentLevelWithItems, "SERIES");

        // Jednotlivost přímo pod Logickou složkou
        itemUnderType(node, items, parentLevelWithItems, "FOLDER_LOGICAL");

        // Množstevní EJ
        isTypeEJ1(node, items, parentLevelWithItems);

        // S uvedením EJ
        isTypeEJ2(node, items, parentLevelWithItems);
    }

    /**
     * Počítání podle "S uvedením EJ".
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentLevelWithItems rodiče uzlu
     */
    private void isTypeEJ2(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        boolean isFind = isItem(items, "FOLDER_SINGLE_TYPE");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentLevelWithItems);


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
     * @param parentLevelWithItems rodiče uzlu
     */
    private void isTypeEJ1(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        boolean isFind = isItem(items, "FOLDER_UNITS");

        if (!isFind) {
            return;
        }

        boolean count = !hasIgnoredParent(parentLevelWithItems);

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
                	RulPacketType packetType = packet.getPacketType();
                	if(packetType!=null) {
                		String shortcut = packetType.getShortcut();
                		addToCount(shortcut, 1);
                	}
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
     * Detekce jednotlivosti pod danou úrovní.
     *
     * @param node                procházený uzel
     * @param items               seznam hodnot uzlu
     * @param parentLevelWithItems rodiče uzlu
     * @param type                typ, pod kterým hledáme (z předpisu akce)
     */
    public void itemUnderType(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems, final String type) {

        // jedna se o jednotlivost?
        boolean found = isItem(items, "ITEM");

        if (!found) {
            // pokud ne, není co počítat
            return;
        }

        // Check if under given type
        boolean isUnder = isDirectlyUnder(parentLevelWithItems, type);
        if (!isUnder) {
        	return;
        }
        
        // Flag if item should be counted
        boolean canCount = !hasIgnoredParent(parentLevelWithItems);
        if(canCount) {
            RulItemType unitType = itemTypes.get("UNIT_TYPE");
            addItemWithTypeToCount(unitType, items);
        }

        /*
        if (isItem(items, "KTT")) {
                countKkt(items);
        }*/
    }

    /**
     * Add all items with given type
     * @param unitType
     * @param items
     */
    private void addItemWithTypeToCount(RulItemType unitType, List<ArrDescItem> items) {
    	for(ArrDescItem descItem: items)
    	{
    		if(descItem.getItemType().equals(unitType))
    		{
    			// TODO: ignore some types    			
    			addToCount(descItem.getItemSpec().getShortcut(), 1);
    		}
    	}
		
	}

	/**
     * Jsou mezi ignorovanými některý z rodičů?
     *
     * @param parentLevelWithItems rodiče uzlu
     * @return jsou?
     */
    protected boolean hasIgnoredParent(LevelWithItems parentLevelWithItems) {
        while(parentLevelWithItems!=null) {
        	ArrNode arrNode = parentLevelWithItems.getLevel().getNode();
            if (ignoredNodeId.contains(arrNode.getNodeId())) {
                return true;
            }        	
            parentLevelWithItems = parentLevelWithItems.getParent();
    	}
        return false;
    }

    /**
     * Je uzel primo pod typem?
     *
     * @param parentLevelWithItems rodiče uzlu
     * @param type typ, pod kterým hledáme (z předpisu akce)
     * @return je?
     */
    protected boolean isDirectlyUnder(LevelWithItems parentLevelWithItems, final String type) {
        RulItemType itemType = itemTypes.get(type);
        RulItemSpec itemSpec = itemSpecs.get(type);

        if(parentLevelWithItems!=null) {
    		List<ArrDescItem> descItems = parentLevelWithItems.getDescItems();
            for (ArrDescItem parentItem : descItems) {
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
    	// Units are counted only on real nodes in finding aid
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
            Map.Entry<String, String> key = new AbstractMap.SimpleEntry<>(outputColumnUnitName, entry.getKey());
            Map.Entry<String, String> value = new AbstractMap.SimpleEntry<>(outputColumnUnitCount, entry.getValue().toString());
            table.addRow(new ElzaRow(key, value));
        }

        result.setTable(table);
        return result;
    }

}
