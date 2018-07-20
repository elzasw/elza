package cz.tacr.elza.core.rules;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulItemTypeExt;

/**
 * Builder for extended Item type definition
 *
 */
public class ItemTypeExtBuilder {

	List<RulItemTypeExt> itemTypes = new ArrayList<>();

	public List<RulItemTypeExt> getResult() {
		// sort result
		itemTypes.sort((o1, o2) -> {
			if (o1.getViewOrder() == null) {
				if (o2.getViewOrder() == null) {
					return o1.getItemTypeId().compareTo(o2.getItemTypeId());
				} else {
					return -1;
				}
			}
			if (o2.getViewOrder() == null) {
				return 1;
			}
			return o1.getViewOrder().compareTo(o2.getViewOrder());
		});

		return itemTypes;
	}

	public void add(List<ItemType> typeList) {
		for (ItemType rst : typeList) {
			add(rst);
		}
	}

	public void add(ItemType rst) {

		RulItemTypeExt itemTypeExt = new RulItemTypeExt(rst.getEntity(), rst.getItemSpecs());

		itemTypeExt.setType(RulItemType.Type.IMPOSSIBLE);
		itemTypeExt.setRepeatable(true);
		itemTypeExt.setCalculable(false);
		itemTypeExt.setCalculableState(false);
		itemTypeExt.setIndefinable(false);
		itemTypeExt.setPolicyTypeCode(null);

		itemTypes.add(itemTypeExt);
	}

}
