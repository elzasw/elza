package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.core.data.StaticDataProvider;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.UnitCountActionResult;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.StructuredItemRepository;

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
	 * Počet EJ
	 *
	 * Map is order according type
	 */
	private Map<String, Integer> resultMap = new TreeMap<>();

    @Autowired
    StructuredItemRepository structureItemRepository;

	final UnitCountConfig config;


    @Autowired
    UnitCountAction(final UnitCountConfig config) {
		Validate.notNull(config);

		this.config = config;
        // this.structureItemRepository = structureItemRepository;
    }

    @Override
	public void init(ArrBulkActionRun bulkActionRun) {
        Validate.notNull(structureItemRepository);

		StaticDataProvider ruleSystem = getStaticDataProvider();

		String outputType = config.getOutputType();
		outputItemType = ruleSystem.getItemTypeByCode(outputType);
		checkValidDataType(outputItemType, DataType.JSON_TABLE);

		// validate column names
		Validate.notBlank(config.getOutputColumnUnitName());
		Validate.notBlank(config.getOutputColumnUnitCount());

		// validate column definitions
		List<ElzaColumn> columnsDefinition = (List<ElzaColumn>) outputItemType.getEntity().getViewDefinition();
		Map<String, ElzaColumn> outputColumns = columnsDefinition.stream()
		        .collect(Collectors.toMap(ElzaColumn::getCode, Function.identity()));

		validateColumn(config.getOutputColumnUnitName(), ElzaColumn.DataType.TEXT, outputColumns);
		validateColumn(config.getOutputColumnUnitCount(), ElzaColumn.DataType.INTEGER, outputColumns);

		// initialize counters
		for (UnitCounterConfig counterCfg : config.getAggegators()) {
            UnitCounter uc = new UnitCounter(counterCfg, structureItemRepository, ruleSystem);
			counters.add(uc);
		}
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
	 * @param inc
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
