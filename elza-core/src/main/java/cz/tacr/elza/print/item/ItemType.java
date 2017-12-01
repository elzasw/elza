package cz.tacr.elza.print.item;

import java.util.List;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;

/**
 * Typ Itemu, odpovídá rul_item_type (+rul_data_type)
 *
 */
public class ItemType {

    String name;
    DataType dataType;
    String shortcut;
    String description;
    String code;
    Integer viewOrder;
    List<ElzaColumn> tableDefinition;

    public ItemType(RulItemType rulItemType) {
        name = rulItemType.getName();
        dataType = DataType.fromId(rulItemType.getDataTypeId());
        shortcut = rulItemType.getShortcut();
        description = rulItemType.getDescription();
        code = rulItemType.getCode();
        viewOrder = rulItemType.getViewOrder();
        tableDefinition = rulItemType.getColumnsDefinition();
	}

	public String getCode() {
        return code;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getShortcut() {
        return shortcut;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public List<ElzaColumn> getTableDefinition() {
		return tableDefinition;
    }
}
