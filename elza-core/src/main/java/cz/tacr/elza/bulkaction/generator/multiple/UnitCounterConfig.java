package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Map;

public class UnitCounterConfig {
    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

	WhenConditionConfig when;

	boolean stopProcessing;

	String itemType;

	Map<String, String> itemSpecMapping;

	/**
	 * Optional item with item count
	 *
	 * If not present default item count is 1. Item have to have type int.
	 */
	String itemCount;

	String objectType;

	/**
	 * Packet type mapping
	 */
	Map<String, String> objectMapping;

	public String getItemCount() {
		return itemCount;
	}

	public void setItemCount(String itemCount) {
		this.itemCount = itemCount;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Map<String, String> getObjectMapping() {
		return objectMapping;
	}

	public void setObjectMapping(Map<String, String> objectMapping) {
		this.objectMapping = objectMapping;
    }

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

    public WhenConditionConfig getWhen() {
		return when;
	}

	public void setWhen(WhenConditionConfig when) {
		this.when = when;
	}

	public boolean isStopProcessing() {
		return stopProcessing;
	}

	public void setStopProcessing(boolean stopProcessing) {
		this.stopProcessing = stopProcessing;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public Map<String, String> getItemSpecMapping() {
		return itemSpecMapping;
	}

	public void setItemSpecMapping(Map<String, String> itemSpecMapping) {
		this.itemSpecMapping = itemSpecMapping;
	}


}
