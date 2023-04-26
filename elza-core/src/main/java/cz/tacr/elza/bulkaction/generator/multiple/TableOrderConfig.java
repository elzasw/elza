package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class TableOrderConfig {
    protected String columnName;
    protected List<String> valueOrder;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public List<String> getValueOrder() {
        return valueOrder;
    }

    public void setValueOrder(List<String> valueOrder) {
        this.valueOrder = valueOrder;
    }

}
