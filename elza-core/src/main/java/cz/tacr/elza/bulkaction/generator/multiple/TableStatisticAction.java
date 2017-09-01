package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cz.tacr.elza.service.ArrangementService;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.TableStatisticActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.utils.Yaml;

/**
 * Akce na vytvoření tabulky statistiky.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
@Component
@Scope("prototype")
public class TableStatisticAction extends Action {

    /**
     * Vstupní atributy
     */
    private Set<RulItemType> inputItemTypes;

    /**
     * Výstupní atribut
     */
    private RulItemType outputItemType;

    /**
     * Datový typ sloupce
     */
    private ElzaColumn.DataType columnDataType;

    /**
     * Kód sloupce
     */
    private String columnCode;

    /**
     * Seznam dat pro sloupec tabulky
     */
    private Set<String> data = new TreeSet<>();

    TableStatisticAction(final Yaml config) {
        super(config);
    }

    @Override
    public void init() {
        Set<String> inputTypes = config.getStringList("input_types", null).stream().collect(Collectors.toSet());
        String outputType = config.getString("output_type", null);
        inputItemTypes = findItemTypes(inputTypes);
        outputItemType = findItemType(outputType, "output_type");
        checkValidDataType(outputItemType, "JSON_TABLE");

        List<ElzaColumn> columnsDefinition = outputItemType.getColumnsDefinition();

        String outputTableCode = config.getString("output_table_code", null);
        String outputTableType = config.getString("output_table_type", null);

        for (ElzaColumn column : columnsDefinition) {
            if (column.getCode().equals(outputTableCode) && column.getDataType().name().equals(outputTableType)) {
                columnCode = column.getCode();
                columnDataType = column.getDataType();
            }
        }

        if (columnCode == null) {
            throw new IllegalArgumentException("Neplatný sloupec tabulky výstupního atributu");
        }
    }

    @Override
    public void apply(final ArrNode node, final List<ArrDescItem> items, final LevelWithItems parentLevelWithItems) {
        for (ArrItem item : items) {
            if (inputItemTypes.contains(item.getItemType())) {
                String text;
                RulItemSpec itemSpec = item.getItemSpec();
                if (BooleanUtils.isTrue(item.getUndefined())) {
                    text = ArrangementService.UNDEFINED;
                } else {
                    ArrItemData itemData = item.getItem();
                    itemData.setSpec(itemSpec);
                    text = itemData.toString();
                }
                if (itemSpec != null) {
                    text = itemSpec.getName() + ": " + text;
                }
                data.add(text);
            }
        }
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
        ElzaTable table = new ElzaTable();

        for (String record : data) {
            ElzaRow row = new ElzaRow();
            row.setValue(columnCode, record);
            table.addRow(row);
        }

        TableStatisticActionResult result = new TableStatisticActionResult();
        result.setItemType(outputItemType.getCode());
        result.setColumnCode(columnCode);
        result.setColumnDataType(columnDataType.name());
        result.setTable(table);

        return result;
    }

}
