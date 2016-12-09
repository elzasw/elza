package cz.tacr.elza.controller.vo.filter;

import java.util.Map;

/**
 * Filtry.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 4. 2016
 */
public class Filters {

    private ColumnFilters columnFilters = new ColumnFilters();

    /** Id nodu. */
    private Integer nodeId;

    public void setColumnFilters(final ColumnFilters columnFilters) {
        this.columnFilters = columnFilters;
    }

    public ColumnFilters getColumnFilters() {
        return columnFilters;
    }

    public Map<Integer, Filter> getFilters() {
        return getColumnFilters().getFilters();
    }

    public void setFilters(final Map<Integer, Filter> filters) {
        this.setColumnFilters(new ColumnFilters());
        this.getColumnFilters().setFilters(filters);
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }
}
