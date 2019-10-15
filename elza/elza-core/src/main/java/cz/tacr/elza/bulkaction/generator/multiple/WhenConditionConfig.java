package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class WhenConditionConfig {
	String itemType;
	String itemSpec;

	List<WhenConditionConfig> all;
	List<WhenConditionConfig> someOf;

	/**
	 * Condition for parent
	 */
	WhenConditionConfig parent;

	public List<WhenConditionConfig> getAll() {
		return all;
	}

	public void setAll(List<WhenConditionConfig> all) {
		this.all = all;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public String getItemSpec() {
		return itemSpec;
	}

	public void setItemSpec(String itemSpec) {
		this.itemSpec = itemSpec;
	}

	public List<WhenConditionConfig> getSomeOf() {
		return someOf;
	}

	public void setSomeOf(List<WhenConditionConfig> someOf) {
		this.someOf = someOf;
	}

	public WhenConditionConfig getParent() {
		return parent;
	}

	public void setParent(WhenConditionConfig parent) {
		this.parent = parent;
	}
}
