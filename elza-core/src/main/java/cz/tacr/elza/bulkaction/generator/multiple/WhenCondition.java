package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import liquibase.util.Validate;

public class WhenCondition {

	RuleSystemItemType itemType;

	RulItemSpec itemSpec;

	/**
	 * List of condition when at least one of them have to be true
	 */
	List<WhenCondition> someOf;

	/**
	 * List of condition when all of them have to be true
	 */
	List<WhenCondition> all;

	/**
	 * Condition for parent
	 */
	WhenCondition parentCond;

	final WhenConditionConfig config;

	public WhenCondition(WhenConditionConfig whenConfig, RuleSystem ruleSystem) {
		this.config = whenConfig;
		init(ruleSystem);
	}

	// Prepare when condition from configuration
	private void init(RuleSystem ruleSystem) {
		String itemTypeCode = config.getItemType();
		if (itemTypeCode != null) {
			itemType = ruleSystem.getItemTypeByCode(itemTypeCode);
			Validate.notNull(itemType, "Cannot find type with code: " + itemTypeCode);

			String itemSpecCode = config.getItemSpec();
			if (itemSpecCode != null) {
				itemSpec = itemType.getItemSpecByCode(itemSpecCode);
				Validate.notNull(itemType,
				        "Cannot find spec with code: " + itemSpecCode + " and type: " + itemTypeCode);
			}
		}
		// prepare someOf
		List<WhenConditionConfig> someOfConfigs = config.getSomeOf();
		if (someOfConfigs != null) {
			initSomeOfs(someOfConfigs, ruleSystem);
		}

		// prepare all
		List<WhenConditionConfig> allConfigs = config.getAll();
		if (allConfigs != null) {
			initAll(allConfigs, ruleSystem);
		}

		// prepare parent condition
		WhenConditionConfig parentCondConfig = config.getParent();
		if (parentCondConfig != null) {
			parentCond = new WhenCondition(parentCondConfig, ruleSystem);
		}
	}

	private void initAll(List<WhenConditionConfig> allConfigs, RuleSystem ruleSystem) {
		all = new ArrayList<>(allConfigs.size());

		for (WhenConditionConfig singleConfig : allConfigs) {
			WhenCondition cond = new WhenCondition(singleConfig, ruleSystem);
			all.add(cond);
		}

	}

	private void initSomeOfs(List<WhenConditionConfig> someOfConfigs, RuleSystem ruleSystem) {
		someOf = new ArrayList<>(someOfConfigs.size());

		for (WhenConditionConfig someOfConfig : someOfConfigs) 
		{
			WhenCondition cond = new WhenCondition(someOfConfig, ruleSystem);
			someOf.add(cond);
		}
		
	}

	public boolean isTrue(LevelWithItems level) {
		// check item type and item spec
		if (itemType != null) {
			List<ArrDescItem> items = level.getDescItems(itemType, itemSpec);
			if (items == null) {
				return false;
			}
		}

		// test someOf
		if (someOf != null) {
			boolean exists = false;
			for (WhenCondition some : someOf) {
				if (some.isTrue(level)) {
					exists = true;
					break;
				}
			}
			if(!exists) {
				return false;
			}
		}

		// test all
		if (all != null) {
			for (WhenCondition cond : all) {
				if (!cond.isTrue(level)) {
					return false;
				}
			}
		}

		// test parent condition
		if (parentCond != null) {
			LevelWithItems parent = level.getParent();
			if (parent == null) {
				// parent does not exists
				return false;
			}
			if (!parentCond.isTrue(parent)) {
				return false;
			}
		}
		return true;
	}

}