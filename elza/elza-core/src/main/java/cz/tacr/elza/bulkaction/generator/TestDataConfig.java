package cz.tacr.elza.bulkaction.generator;

import java.util.List;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

public class TestDataConfig extends BaseActionConfig {

	List<Integer> itemsToGenerate;

	public List<Integer> getItemsToGenerate() {
		return itemsToGenerate;
	}

	public void setItemsToGenerate(List<Integer> itemsToGenerate) {
		this.itemsToGenerate = itemsToGenerate;
	}

	@Override
	public BulkAction createBulkAction() {
		return new TestDataGenerator(this);
	}

}
