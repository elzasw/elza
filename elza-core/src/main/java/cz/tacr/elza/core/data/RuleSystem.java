package cz.tacr.elza.core.data;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSystem {

    private final RulRuleSet ruleSet;

	protected List<RulPacketType> packetTypes;

	protected List<RuleSystemItemType> itemTypes;

	protected Map<Integer, RulPacketType> packetTypeIdMap;

	protected Map<String, RulPacketType> packetTypeCodeMap;

	protected Map<Integer, RuleSystemItemType> itemTypeIdMap;

	protected Map<String, RuleSystemItemType> itemTypeCodeMap;

    RuleSystem(RulRuleSet ruleSet) {
        this.ruleSet = Validate.notNull(ruleSet);
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public List<RulPacketType> getPacketTypes() {
        return packetTypes;
    }

    public RulPacketType getPacketTypeById(Integer id) {
        Validate.notNull(id);
        return packetTypeIdMap.get(id);
    }

    public RulPacketType getPacketTypeByCode(String code) {
        Validate.notEmpty(code);
        return packetTypeCodeMap.get(code);
    }

    public List<RuleSystemItemType> getItemTypes() {
        return itemTypes;
    }

    public RuleSystemItemType getItemTypeById(Integer id) {
        Validate.notNull(id);
        return itemTypeIdMap.get(id);
    }

	/**
	 * Return description item by code
	 * 
	 * @param code
	 *            Item type code
	 * @return Return description item. If item does not exist return null.
	 */
    public RuleSystemItemType getItemTypeByCode(String code) {
        Validate.notEmpty(code);
        return itemTypeCodeMap.get(code);
    }

}
