package cz.tacr.elza.controller.vo.filter;

import java.util.Map;

/**
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 09.12.2016
 */
public class ColumnFilters {
    /** Mapa filtrů id rulDescItemType -> filter. */
    private Map<Integer, Filter> filters;

    public Map<Integer, Filter> getFilters() {
        return filters;
    }

    public void setFilters(Map<Integer, Filter> filters) {
        this.filters = filters;
    }
}
