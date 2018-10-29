package cz.tacr.elza.print.item;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.RulItemType;

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

    private final Object viewDefinition;

    public ItemType(RulItemType rulItemType) {
        this.name = rulItemType.getName();
        this.dataType = DataType.fromId(rulItemType.getDataTypeId());
        this.shortcut = rulItemType.getShortcut();
        this.description = rulItemType.getDescription();
        this.code = rulItemType.getCode();
        this.viewOrder = rulItemType.getViewOrder();
        this.viewDefinition = rulItemType.getViewDefinition();
    }

    public ItemType(RulItemType rulItemType, DataType dataType) {
        this.name = rulItemType.getName();
        this.dataType = dataType;
        this.shortcut = rulItemType.getShortcut();
        this.description = rulItemType.getDescription();
        this.code = rulItemType.getCode();
        this.viewOrder = rulItemType.getViewOrder();
        this.viewDefinition = rulItemType.getViewDefinition();
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

    public Object getViewDefinition() {
        return viewDefinition;
    }
}
