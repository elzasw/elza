package cz.tacr.elza.bulkaction.generator;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;


/**
 * Class to bound level, its description items and parent
 */
public class LevelWithItems {
	LevelWithItems parent;
    final TreeNode treeNode;
    
    /**
     * Description items
     */
    final List<ArrDescItem> descItems = new ArrayList<>();

    public LevelWithItems(final TreeNode n) {
        this.treeNode = n;
    }
    
    public LevelWithItems(final TreeNode n, final LevelWithItems parentLevel) {
        this.treeNode = n;
		this.parent = parentLevel;
	}

    public LevelWithItems(final TreeNode n, final LevelWithItems parentLevel, final List<ArrDescItem> items) {
        this.treeNode = n;
		this.parent = parentLevel;
        if (items != null) {
            descItems.addAll(items);
        }
	}

	public LevelWithItems getParent()
    {
    	return parent;
    }

    public TreeNode getTreeNode() {
        return treeNode;
	}

	public List<ArrDescItem> getDescItems() {
		return descItems;
	}

    public Integer getNodeId() {
        return treeNode.getId();
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
