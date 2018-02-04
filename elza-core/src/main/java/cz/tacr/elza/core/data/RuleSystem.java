package cz.tacr.elza.core.data;

import java.util.List;

import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;

public interface RuleSystem {

    RulRuleSet getRuleSet();

    List<RulPacketType> getPacketTypes();

    RulPacketType getPacketTypeById(Integer id);

    RulPacketType getPacketTypeByCode(String code);

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
