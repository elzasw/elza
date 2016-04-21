package cz.tacr.elza.controller.vo.filter;

import java.util.Map;

/**
 * Filtry.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 4. 2016
 */
public class Filters {

    /** Mapa filtrů id rulDescItemType -> filter. */
    private Map<Integer, Filter> filters;

    public Map<Integer, Filter> getFilters() {
        return filters;
    }

    public void setFilters(Map<Integer, Filter> filters) {
        this.filters = filters;
    }
}
