package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;

public class RuleSystemImpl implements RuleSystem {

    private final RulRuleSet ruleSet;

    private List<RulStructuredType> structuredTypes;

    private List<RuleSystemItemType> itemTypes;

    private Map<Integer, RulStructuredType> structuredTypeIdMap;

    private Map<String, RulStructuredType> structuredTypeCodeMap;

    private Map<Integer, RuleSystemItemType> itemTypeIdMap;

    private Map<String, RuleSystemItemType> itemTypeCodeMap;

    public RuleSystemImpl(RulRuleSet ruleSet) {
        this.ruleSet = Validate.notNull(ruleSet);
        this.structuredTypes = new ArrayList<>();
        this.itemTypes = new ArrayList<>();
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public List<RulStructuredType> getStructuredTypes() {
        return structuredTypes;
    }

    @Override
    public RulStructuredType getStructuredTypeById(Integer id) {
        Validate.notNull(id);
        return structuredTypeIdMap.get(id);
    }

    @Override
    public RulStructuredType getStructuredTypeByCode(String code) {
        Validate.notEmpty(code);
        return structuredTypeCodeMap.get(code);
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
    public void addStructuredType(RulStructuredType st) {
        this.structuredTypes.add(st);
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
        this.structuredTypeIdMap = StaticDataProvider.createLookup(structuredTypes,
                RulStructuredType::getStructuredTypeId);
        this.structuredTypeCodeMap = StaticDataProvider.createLookup(structuredTypes, RulStructuredType::getCode);

		this.itemTypeIdMap = StaticDataProvider.createLookup(itemTypes, RuleSystemItemType::getItemTypeId);
		this.itemTypeCodeMap = StaticDataProvider.createLookup(itemTypes, RuleSystemItemType::getCode);

		// switch to unmodifiable collections
        structuredTypes = Collections.unmodifiableList(structuredTypes);
		itemTypes = Collections.unmodifiableList(itemTypes);
		return this;
	}
}
