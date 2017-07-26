package cz.tacr.elza.print.item;

import java.util.List;

import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.table.ElzaColumn;

/**
 * Typ Itemu, odpovídá rul_item_type (+rul_data_type)
 *
 */
public class ItemType {

    String name;
    String dataType;
    String shortcut;
    String description;
    String code;
    Integer viewOrder;
    List<ElzaColumn> tableDefinition;

    public ItemType(RulItemType rulItemType) {
        name = rulItemType.getName();
        dataType = rulItemType.getDataType().getCode();
        shortcut = rulItemType.getShortcut();
        description = rulItemType.getDescription();
        code = rulItemType.getCode();
        viewOrder = rulItemType.getViewOrder();
        tableDefinition = rulItemType.getColumnsDefinition();
	}

	public String getCode() {
        return code;
    }

    public String getDataType() {
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
    
    public static ItemType instanceOf(final RulItemType rulItemType) {
    	return new ItemType(rulItemType); 
    }
}
