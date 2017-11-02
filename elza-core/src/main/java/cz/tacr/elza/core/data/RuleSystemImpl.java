package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;

import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSystemImpl extends RuleSystem {

	RuleSystemImpl(RulRuleSet ruleSet) {
		super(ruleSet);

		packetTypes = new ArrayList<>();
		itemTypes = new ArrayList<>();
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

	public RuleSystem sealUp() {
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
