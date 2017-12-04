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

    private final String name;

    private final DataType dataType;

    private final String shortcut;

    private final String description;

    private final String code;

    private final Integer viewOrder;

    private final List<ElzaColumn> tableDefinition;

    public ItemType(RulItemType rulItemType) {
        this.name = rulItemType.getName();
        this.dataType = DataType.fromId(rulItemType.getDataTypeId());
        this.shortcut = rulItemType.getShortcut();
        this.description = rulItemType.getDescription();
        this.code = rulItemType.getCode();
        this.viewOrder = rulItemType.getViewOrder();
        this.tableDefinition = rulItemType.getColumnsDefinition();
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
