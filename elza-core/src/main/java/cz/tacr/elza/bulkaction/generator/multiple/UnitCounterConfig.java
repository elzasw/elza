package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.Map;

public class UnitCounterConfig {
    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

	WhenConditionConfig when;

	boolean stopProcessing;

	/**
	 * Stop the remaining aggregators on the current level only.
	 *
	 * Unlike stopProcessing the subtree of the level is still processed. Ignored when
	 * stopProcessing is set.
	 */
	boolean stopLevelProcessing;

	/**
	 * Add dates of a level that forms no evidence unit into the date range of the closest
	 * enclosing counted structured object, i.e. of the unit the level is stored in.
	 *
	 * Affects date ranges only, never the counts.
	 */
	boolean datesToEnclosingUnit;

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
     * Type of the item stored in the object
     */
    String objectItemType;

    /**
     * Packet type mapping
     */
    Map<String, String> objectItemMapping;

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

    public Map<String, String> getObjectItemMapping() {
        return objectItemMapping;
	}

    public void setObjectItemMapping(Map<String, String> objectMapping) {
        this.objectItemMapping = objectMapping;
	}

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

    public String getObjectItemType() {
        return objectItemType;
    }

    public void setObjectItemType(String objectItemType) {
        this.objectItemType = objectItemType;
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

	public boolean isStopLevelProcessing() {
		return stopLevelProcessing;
	}

	public void setStopLevelProcessing(boolean stopLevelProcessing) {
		this.stopLevelProcessing = stopLevelProcessing;
	}

	public boolean isDatesToEnclosingUnit() {
		return datesToEnclosingUnit;
	}

	public void setDatesToEnclosingUnit(boolean datesToEnclosingUnit) {
		this.datesToEnclosingUnit = datesToEnclosingUnit;
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
