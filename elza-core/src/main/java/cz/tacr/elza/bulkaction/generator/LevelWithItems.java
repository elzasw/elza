package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;


/**
 * Class to bound level, its description items and parent
 */
public class LevelWithItems {
	LevelWithItems parent;
    final ArrLevel level;
    
    /**
     * Description items
     */
    final List<ArrDescItem> descItems = new ArrayList<>();

    public LevelWithItems(final ArrLevel level) {
        this.level = level;
    }
    
    public LevelWithItems(final ArrLevel level, final LevelWithItems parentLevel) {
    	this.level = level;
		this.parent = parentLevel;
	}

	public LevelWithItems(final ArrLevel level, final LevelWithItems parentLevel, final List<ArrDescItem> items) {
    	this.level = level;
		this.parent = parentLevel;
        if (items != null) {
            descItems.addAll(items);
        }
	}

	public LevelWithItems getParent()
    {
    	return parent;
    }

	public ArrLevel getLevel() {
		return level;
	}

	public List<ArrDescItem> getDescItems() {
		return descItems;
	}

	public ArrNode getNode() {
		return level.getNode();
	}

	/**
     * Return list of items with given spec
     * 
     * @param itemType
     * @param itemSpec
     *            if null all specs are returned
     * @return Return null if such itema does not exists
     */
	public List<ArrDescItem> getDescItems(ItemType itemType, RulItemSpec itemSpec) {
		List<ArrDescItem> result = null;

		for (ArrDescItem item : descItems) {
			if (itemType.getItemTypeId().equals(item.getItemTypeId())) {
				// check if no itemSpec or have to match
				if (itemSpec == null || itemSpec.getItemSpecId().equals(item.getItemSpecId())) {

					// append to result
					if (result == null) {
						result = new ArrayList<>(1);
					}
					result.add(item);
				}
			}
		}

		return result;
	}

    /**
     * Return list of description items
     * 
     * Function return list of description items directly places on the level or
     * at the parent
     * 
     * @param bulkRangeType
     * @return
     */
    public List<ArrDescItem> getInheritedDescItems(ItemType itemType) {
        List<ArrDescItem> result = null;

        for (ArrDescItem item : descItems) {
            if (itemType.getItemTypeId().equals(item.getItemTypeId())) {
                // append to result
                if (result == null) {
                    result = new ArrayList<>(1);
                }
                result.add(item);
            }
        }

        // if result not exists try to get it from parent        
        if (result == null) {
            if (parent != null) {
                return parent.getInheritedDescItems(itemType);
            }
        }

        return result;
    }
}
