package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

/**
 * Configuration for serial number generator
 *
 */
public class SerialNumberConfig extends BaseActionConfig {

	String itemType;

	boolean useCurrentNumbering = false;

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public boolean getUseCurrentNumbering() {
		return useCurrentNumbering;
	}

	public void setUseCurrentNumbering(boolean useCurrentNumbering) {
		this.useCurrentNumbering = useCurrentNumbering;
	}

	@Override
	public BulkAction createBulkAction() {
		return new SerialNumberBulkAction(this);
	}

}
