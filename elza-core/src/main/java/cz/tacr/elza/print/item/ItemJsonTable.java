package cz.tacr.elza.print.item;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.table.ElzaColumn;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;

/**
 * Item to print content of table
 */
public class ItemJsonTable extends AbstractItem {

    private final static String ROW_SEPARATOR = ", ";
    private final static String ITEM_SEPARATOR = " ";

    private final ElzaTable value;

    private final List<ElzaColumn> tableDef;

    public ItemJsonTable(List<ElzaColumn> tableDef, final ElzaTable value) {
        this.value = value;
        this.tableDef = tableDef;
    }

    @Override
    public String getSerializedValue() {
        StringBuilder sb = new StringBuilder();

        boolean hasData = false;
        for (ElzaRow row : value.getRows()) {
            Map<String, String> columns = row.getValues();

            boolean itemAdded = false;
            // Store single value - iterate by columns
            for (ElzaColumn col : tableDef) {
                String value = columns.get(col.getCode());
                if (StringUtils.isNotBlank(value)) {
                    // add separator
                    if (hasData) {
                        if (itemAdded) {
                            sb.append(ITEM_SEPARATOR);
                        } else {
                            sb.append(ROW_SEPARATOR);
                            itemAdded = true;
                        }
                    } else {
                        hasData = true;
                        itemAdded = true;
                    }
                    sb.append(value);
                }
            }
        }
        return sb.toString();
    }

    @Override
    protected ElzaTable getValue() {
        return value;
    }
}
