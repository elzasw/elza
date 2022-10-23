package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.FilterConfig.Def;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

public class FilterRules {

    final private List<ItemType> restrictionTypes;

    final private List<FilterRule> filterRules;

    public FilterRules(final FilterConfig filterConfig, final StaticDataProvider sdp) {
        this.restrictionTypes = initRestrictionTypes(filterConfig, sdp);
        this.filterRules = initFilterRules(filterConfig, sdp);
    }

    private List<ItemType> initRestrictionTypes(final FilterConfig filterConfig, final StaticDataProvider sdp) {
        List<ItemType> restrictionTypes = new ArrayList<>(filterConfig.getRestrictions().size());
        for (String itemTypeCode : filterConfig.getRestrictions()) {
            ItemType itemType = sdp.getItemTypeByCode(itemTypeCode);
            if (itemType == null) {
                throw new BusinessException("Item type is undefined: " + itemTypeCode, BaseCode.INVALID_STATE);
            }
            restrictionTypes.add(itemType);
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
