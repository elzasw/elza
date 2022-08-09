package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;

public class FilterRules {

    final private List<ItemType> restrictionTypes;

    final private List<FilterRule> filterRules;

    public FilterRules(final FilterConfig filterConfig, final StaticDataProvider sdp) {
        this.restrictionTypes = initRestrictionTypes(filterConfig, sdp);
        this.filterRules = initFilterRules(filterConfig, sdp);
    }

    private List<ItemType> initRestrictionTypes(final FilterConfig filterConfig, final StaticDataProvider sdp) {
        List<ItemType> restrictionTypes = new ArrayList<>(filterConfig.getRestrictions().size());
        for (String structItemTypeCode : filterConfig.getRestrictions()) {
            ItemType structItemType = sdp.getItemTypeByCode(structItemTypeCode);
            if (structItemType == null || structItemType.getDataType() != DataType.STRUCTURED) {
                throw new IllegalStateException("Struct item type is undefined or not STRUCTURED");
            }
            restrictionTypes.add(structItemType);
        }
        return restrictionTypes;
    }

    private List<FilterRule> initFilterRules(final FilterConfig filterConfig, final StaticDataProvider sdp) {
        List<FilterRule> rules = new ArrayList<>(filterConfig.getDefs().size());
        for (Def def : filterConfig.getDefs()) {
            FilterRule rule = new FilterRule(def, sdp);
            rules.add(rule);
        }
        return rules;
    }

    public List<ItemType> getRestrictionTypes() {
        return restrictionTypes;
    }

    public List<FilterRule> getFilterRules() {
        return filterRules;
    }
}
