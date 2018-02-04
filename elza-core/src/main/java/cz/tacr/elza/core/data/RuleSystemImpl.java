package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSystemImpl implements RuleSystem {

    private final RulRuleSet ruleSet;

    private List<RulPacketType> packetTypes;

    private List<RuleSystemItemType> itemTypes;

    private Map<Integer, RulPacketType> packetTypeIdMap;

    private Map<String, RulPacketType> packetTypeCodeMap;

    private Map<Integer, RuleSystemItemType> itemTypeIdMap;

    private Map<String, RuleSystemItemType> itemTypeCodeMap;

    public RuleSystemImpl(RulRuleSet ruleSet) {
        this.ruleSet = Validate.notNull(ruleSet);
        this.packetTypes = new ArrayList<>();
        this.itemTypes = new ArrayList<>();
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public List<RulPacketType> getPacketTypes() {
        return packetTypes;
    }

    @Override
    public RulPacketType getPacketTypeById(Integer id) {
        Validate.notNull(id);
        return packetTypeIdMap.get(id);
    }

    @Override
    public RulPacketType getPacketTypeByCode(String code) {
        Validate.notEmpty(code);
        return packetTypeCodeMap.get(code);
    }

    @Override
    public List<RuleSystemItemType> getItemTypes() {
        return itemTypes;
    }

    @Override
    public RuleSystemItemType getItemTypeById(Integer id) {
        Validate.notNull(id);
        return itemTypeIdMap.get(id);
    }

    @Override
    public RuleSystemItemType getItemTypeByCode(String code) {
        Validate.notEmpty(code);
        return itemTypeCodeMap.get(code);
    }

	/**
	 * Add packet type
	 **/
	public void addPacketType(RulPacketType pt) {
		this.packetTypes.add(pt);
	}

	/**
	 * Add item type
	 *
	 * @param rsit
	 */
	public void addItemType(RuleSystemItemType rsit) {
		itemTypes.add(rsit);
	}

	public RuleSystemImpl sealUp() {
		// update fields
		this.packetTypeIdMap = StaticDataProvider.createLookup(packetTypes, RulPacketType::getPacketTypeId);
		this.packetTypeCodeMap = StaticDataProvider.createLookup(packetTypes, RulPacketType::getCode);

		this.itemTypeIdMap = StaticDataProvider.createLookup(itemTypes, RuleSystemItemType::getItemTypeId);
		this.itemTypeCodeMap = StaticDataProvider.createLookup(itemTypes, RuleSystemItemType::getCode);

		// switch to unmodifiable collections
		packetTypes = Collections.unmodifiableList(packetTypes);
		itemTypes = Collections.unmodifiableList(itemTypes);
		return this;
	}
}
