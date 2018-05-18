package cz.tacr.elza.core.data;

import java.util.List;

import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;

public interface RuleSystem {

    RulRuleSet getRuleSet();

    /**
     * Return collection of all structured types
     *
     * @return
     */
    List<RulStructuredType> getStructuredTypes();

    RulStructuredType getStructuredTypeById(Integer id);

    RulStructuredType getStructuredTypeByCode(String code);

    List<RuleSystemItemType> getItemTypes();

    RuleSystemItemType getItemTypeById(Integer id);

    /**
     * Return description item by code
     *
     * @param code
     *            Item type code
     * @return Return description item. If item does not exist return null.
     */
    RuleSystemItemType getItemTypeByCode(String code);

}
