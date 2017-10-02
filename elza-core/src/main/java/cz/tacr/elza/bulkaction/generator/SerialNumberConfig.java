package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

/**
 * Configuration for serial number generator
 *
 */
public class SerialNumberConfig extends BaseActionConfig {

	String itemType;

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	@Override
	public BulkAction createBulkAction() {
		return new SerialNumberBulkAction(this);
	}

}
