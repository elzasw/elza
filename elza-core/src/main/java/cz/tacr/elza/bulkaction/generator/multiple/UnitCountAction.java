package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.ActionRunContext;
import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Unit count action
 *
 */
@Component
@Scope("prototype")
public class UnitCountAction extends Action {

    /**
     * Výstupní atribut
     */
	private RuleSystemItemType outputItemType;

	/**
	 * Skip subtree
	 */
	LevelWithItems skipSubtree;

	/**
	 * List of counters
	 */
	List<UnitCounter> counters = new ArrayList<>();

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
	 * 
	 * Map is order according type
	 */
	private Map<String, Integer> resultMap = new TreeMap<>();
    
    /**
     * Seznam ignorovaných uzlů, které byly již započítané.
     */
    private Set<Integer> ignoredNodeId = new HashSet<>();

	final UnitCountConfig config;

	UnitCountAction(final UnitCountConfig config) {
		Validate.notNull(config);
		this.config = config;
    }

    @Override
	public void init(ActionRunContext runContext) {
		RuleSystem ruleSystem = getRuleSystem(runContext);

		String outputType = config.getOutputType();
		outputItemType = ruleSystem.getItemTypeByCode(outputType);
		checkValidDataType(outputItemType, DataType.JSON_TABLE);

		// validate column names
		Validate.notBlank(config.getOutputColumnUnitName());
		Validate.notBlank(config.getOutputColumnUnitCount());

		// validate column definitions
		List<ElzaColumn> columnsDefinition = outputItemType.getEntity().getColumnsDefinition();
		Map<String, ElzaColumn> outputColumns = columnsDefinition.stream()
		        .collect(Collectors.toMap(ElzaColumn::getCode, Function.identity()));

		validateColumn(config.getOutputColumnUnitName(), ElzaColumn.DataType.TEXT, outputColumns);
		validateColumn(config.getOutputColumnUnitCount(), ElzaColumn.DataType.INTEGER, outputColumns);

		// initialize counters
		for (UnitCounterConfig counterCfg : config.getAggegators()) {
			UnitCounter uc = new UnitCounter(counterCfg, ruleSystem);
			counters.add(uc);
		}

		/*
		
		loadType("UNIT_TYPE");
		
		loadTypeAndSpec("ITEM");
		
		loadTypeAndSpec(SERIES);
		
		loadTypeAndSpec("KTT");
		loadType("KTT_count");
		
		loadTypeAndSpec("FOLDER_LOGICAL");
		
		loadTypeAndSpec("FOLDER_UNITS");
		
		loadTypeAndSpec("FOLDER_SINGLE_TYPE");
		loadType("FOLDER_SINGLE_TYPE_count");
		
		loadTypeAndSpec("FOLDER_UNITS");
		loadType("STORAGE");
		
		loadType("EXTRA_UNITS");
		
		// Load item type mapping
		Yaml mappingCfg = config.getSection("item_type_mapping");
		for (String key : mappingCfg.getKeys()) {
			try {
				String value = mappingCfg.getString(key);
				this.itemTypeMap.put(key, value);
			} catch (YAMLKeyNotFoundException e) {
				throw new BusinessException("Failed to read configuration for action UnitCount", BaseCode.SYSTEM_ERROR);
			}
		}
		*/
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
            throw new BusinessException("Atribut " + outputItemType.getCode() + " nemá sloupec " + outputTableColumn, BaseCode.ID_NOT_EXIST);
        }
        if (!column.getDataType().equals(expectedColumnType)) {
            throw new BusinessException("Atribut " + outputItemType.getCode()
                    + " má sloupec " + column.getCode() + " jiného datového typu (" + outputItemType.getDataType().getCode()
                    + "), než je nastaveno (" + expectedColumnType.name() + ").", BaseCode.PROPERTY_HAS_INVALID_TYPE)
                    .set("property", column.getCode())
                    .set("expected", expectedColumnType.name())
                    .set("actual", outputItemType.getDataType().getCode());
        }
    }

    /**
     * Načtení typu a specifikace podle kódu.
     *
     * @param code kódy typu a specifikace jako řetězec oddělený mezerou
     */
    protected void loadTypeAndSpec(final String code) {
		/*
		String attribute = config.getString(code, null);
		if (attribute == null) {
		    throw new BusinessException("Neplatný atribut: " + attribute, BaseCode.PROPERTY_NOT_EXIST).set("property", code);
		}
		String[] split = attribute.split(" ");
		if (split.length != 2) {
		    throw new BusinessException("Neplatný atribut: musí obsahovat kód typu a specifikace", BaseCode.PROPERTY_IS_INVALID).set("property", code);
		}
		
		RulItemType type = findItemType(split[0], code);
		RulItemSpec spec = findItemSpec(split[1]);
		
		if (!spec.getItemType().equals(type)) {
		    throw new BusinessException("Neplatný atribut: specifikace nepatří pod typ", BaseCode.PROPERTY_IS_INVALID).set("property", code);
		}
		
		itemTypes.put(code, type);
		itemSpecs.put(code, spec);
		*/
    }

    /**
     * Načtení typu podle kódu.
     *
     * @param code kód atributu
     */
    protected void loadType(final String code) {
		/*
		String attribute = config.getString(code, null);
		RulItemType type = findItemType(attribute, code);
		itemTypes.put(code, type);
		*/
    }

    @Override
	public void apply(LevelWithItems level, TypeLevel typeLevel) {
		// Units are counted only on real nodes in finding aid
		// parents above root are not counted
		if (!typeLevel.equals(TypeLevel.CHILD)) {
			return;
		}

		for (UnitCounter counter : counters) {

			// Check if node stopped
			if (skipSubtree != null) {
				if (isInTree(skipSubtree, level)) {
					return;
				}
				// reset limit
				skipSubtree = null;
			}

			counter.apply(level, this);
		}

		//List<ArrDescItem> items = level.getDescItems();
		/*
		// Jednotlivost přímo pod Sérii
				itemUnderType(node, items, parentLevelWithItems, SERIES);
		
		// Jednotlivost přímo pod Logickou složkou
		itemUnderType(node, items, parentLevelWithItems, "FOLDER_LOGICAL");
		
		// Množstevní EJ
		isTypeEJ1(node, items, parentLevelWithItems);
		
		// S uvedením EJ
		isTypeEJ2(node, items, parentLevelWithItems);
		*/
    }

	private boolean isInTree(LevelWithItems parent, LevelWithItems subLevel) {
		if (parent == subLevel) {
			return true;
		}
		LevelWithItems level = subLevel.getParent();
		if (level == null) {
			return false;
		}
		return isInTree(parent, level);
	}

	/**
	 * Počítání podle "S uvedením EJ".
	 *
	 * @param node
	 *            procházený uzel
	 * @param items
	 *            seznam hodnot uzlu
	 * @param parentLevelWithItems
	 *            rodiče uzlu
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
			//countItemValue(items);
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
            if (countType.equals(item.getItemType()) && BooleanUtils.isNotTrue(item.getUndefined())) {
				addValue(countSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
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
            if (item.getItemType().equals(extraType) && BooleanUtils.isNotTrue(item.getUndefined())) {
                RulItemSpec itemSpec = item.getItemSpec();
				addValue(itemSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
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
				addValue(extraSpec.getShortcut(), ((ArrItemInt) item.getItem()).getValue());
            }
        }
    }


    /**
     * Add all items with given type
     * @param unitType
     * @param items
     */
    private void addItemWithTypeToCount(RulItemType unitType, List<ArrDescItem> items) {
    	for(ArrDescItem descItem: items)
    	{
    		if(descItem.getItemType().equals(unitType) && BooleanUtils.isNotTrue(descItem.getUndefined()))
    		{
				addValue(descItem.getItemSpec().getShortcut(), 1);
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
    public ActionResult getResult() {
        UnitCountActionResult result = new UnitCountActionResult();
        result.setItemType(outputItemType.getCode());
        ElzaTable table = new ElzaTable();

        for (Map.Entry<String, Integer> entry : resultMap.entrySet()) {
			Map.Entry<String, String> key = new AbstractMap.SimpleEntry<>(config.getOutputColumnUnitName(),
			        entry.getKey());
			Map.Entry<String, String> value = new AbstractMap.SimpleEntry<>(config.getOutputColumnUnitCount(),
			        entry.getValue().toString());
            table.addRow(new ElzaRow(key, value));
        }

        result.setTable(table);
        return result;
    }

	public void setSkipSubtree(LevelWithItems level) {
		this.skipSubtree = level;
	}

	/**
	 * Add value to the result
	 * 
	 * @param value
	 * @param count
	 */
	public void addValue(String value, int inc) {
		Validate.isTrue(inc >= 0, "Číslo nemůže být záporné");

		Integer sum = resultMap.get(value);
		if (sum == null) {
			resultMap.put(value, inc);
		} else {
			resultMap.put(value, sum + inc);
		}
	}

}
