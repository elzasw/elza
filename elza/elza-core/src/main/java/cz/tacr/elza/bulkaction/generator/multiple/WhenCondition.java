package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.bulkaction.generator.LevelWithItems;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import liquibase.util.Validate;

public class WhenCondition {

	ItemType itemType;

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

	public WhenCondition(WhenConditionConfig whenConfig, StaticDataProvider sdp) {
		this.config = whenConfig;
		init(sdp);
	}

	// Prepare when condition from configuration
	private void init(StaticDataProvider sdp) {
		String itemTypeCode = config.getItemType();
		if (itemTypeCode != null) {
			itemType = sdp.getItemTypeByCode(itemTypeCode);
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
			initSomeOfs(someOfConfigs, sdp);
		}

		// prepare all
		List<WhenConditionConfig> allConfigs = config.getAll();
		if (allConfigs != null) {
			initAll(allConfigs, sdp);
		}

		// prepare parent condition
		WhenConditionConfig parentCondConfig = config.getParent();
		if (parentCondConfig != null) {
			parentCond = new WhenCondition(parentCondConfig, sdp);
		}
	}

	private void initAll(List<WhenConditionConfig> allConfigs, StaticDataProvider ruleSystem) {
		all = new ArrayList<>(allConfigs.size());

		for (WhenConditionConfig singleConfig : allConfigs) {
			WhenCondition cond = new WhenCondition(singleConfig, ruleSystem);
			all.add(cond);
		}

	}

	private void initSomeOfs(List<WhenConditionConfig> someOfConfigs, StaticDataProvider ruleSystem) {
		someOf = new ArrayList<>(someOfConfigs.size());

		for (WhenConditionConfig someOfConfig : someOfConfigs)
		{
			WhenCondition cond = new WhenCondition(someOfConfig, ruleSystem);
			someOf.add(cond);
		}

	}

	public boolean isTrue(LevelWithItems level) {
        if (!checkItemType(level)) {
            return false;
        }
        if (!checkSomeOf(level)) {
            return false;
        }
        if (!checkAll(level)) {
            return false;
        }
        if (!checkParent(level)) {
            return false;
        }
		return true;
	}

    private boolean checkParent(LevelWithItems level) {
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

    private boolean checkAll(LevelWithItems level) {
        // test all
        if (all != null) {
            for (WhenCondition cond : all) {
                if (!cond.isTrue(level)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkItemType(LevelWithItems level) {
        // check item type and item spec
        if (itemType != null) {
            List<ArrDescItem> items = level.getDescItems(itemType, itemSpec);
            if (items == null) {
                return false;
            }
        }
        return true;
    }

    private boolean checkSomeOf(LevelWithItems level) {

        // test someOf
        if (someOf != null) {
            boolean exists = false;
            for (WhenCondition some : someOf) {
                if (some.isTrue(level)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return false;
            }
        }
        return true;
    }
}
