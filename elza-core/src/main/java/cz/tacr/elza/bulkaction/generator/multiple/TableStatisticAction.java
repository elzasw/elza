package cz.tacr.elza.bulkaction.generator.multiple;

import cz.tacr.elza.bulkaction.generator.result.ActionResult;
import cz.tacr.elza.bulkaction.generator.result.TableStatisticActionResult;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.utils.Yaml;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
        outputItemType = findItemType(outputType);
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
    public void apply(final List<ArrDescItem> items, final Map<ArrNode, List<ArrDescItem>> parentNodeDescItems) {
        for (ArrItem item : items) {
            if (inputItemTypes.contains(item.getItemType())) {
                ArrItemData itemData = item.getItem();
                data.add(itemData.toString());
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
