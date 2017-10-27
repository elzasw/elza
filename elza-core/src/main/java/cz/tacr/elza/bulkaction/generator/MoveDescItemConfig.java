package cz.tacr.elza.bulkaction.generator;

import cz.tacr.elza.bulkaction.BaseActionConfig;
import cz.tacr.elza.bulkaction.BulkAction;

public class MoveDescItemConfig extends BaseActionConfig {

	public static class ItemSource {
		String itemType;

		public String getItemType() {
			return itemType;
		}

		public void setItemType(String itemType) {
			this.itemType = itemType;
		}
	}

	public static class ItemTarget {
		String itemType;
		String itemSpec;

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
	}

	ItemSource source;

	ItemTarget target;

	public ItemSource getSource() {
		return source;
	}

	public void setSource(ItemSource source) {
		this.source = source;
	}

	public ItemTarget getTarget() {
		return target;
	}

	public void setTarget(ItemTarget target) {
		this.target = target;
	}

	@Override
	public BulkAction createBulkAction() {
		MoveDescItem moveDescItem = new MoveDescItem(this);
		return moveDescItem;
	}

}
