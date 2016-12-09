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

    /** Id nodu. */
    private Integer nodeId;

    public Map<Integer, Filter> getFilters() {
        return filters;
    }

    public void setFilters(final Map<Integer, Filter> filters) {
        this.filters = filters;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }
}
