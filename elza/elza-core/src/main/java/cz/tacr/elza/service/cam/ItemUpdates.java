package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.cam.schema.cam.ItemBinaryXml;
import cz.tacr.cam.schema.cam.ItemBooleanXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.ItemEnumXml;
import cz.tacr.cam.schema.cam.ItemIntegerXml;
import cz.tacr.cam.schema.cam.ItemLinkXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemUnitDateXml;
import cz.tacr.elza.domain.ApBindingItem;

/**
 * Collection of items updates
 *
 * Contains list of changed, not changed and new items
 */
public class ItemUpdates {
	
    static public class ChangedBindedItem {
        final ApBindingItem bindingItem;
        final Object xmlItem;

        public ChangedBindedItem(ApBindingItem bindingItem, Object xmlItem) {
            super();
            this.bindingItem = bindingItem;
            this.xmlItem = xmlItem;
        }

        ApBindingItem getBindingItem() {
            return bindingItem;
        }

        Object getXmlItem() {
            return xmlItem;
        }
    };
	
    List<ApBindingItem> notChangeItems = new ArrayList<>();
    List<ChangedBindedItem> changedItems = new ArrayList<>();
    List<Object> newItems =  new ArrayList<>();
    
	public List<ApBindingItem> getNotChangeItems() {
		return notChangeItems;
	}

	public List<ChangedBindedItem> getChangedItems() {
		return changedItems;
	}

	public List<Object> getNewItems() {
		return newItems;
	}

	public void addNotChanged(ApBindingItem bindingItem) {
		notChangeItems.add(bindingItem);
	}

	public void addNewItem(ItemBinaryXml itemBinary) {
		newItems.add(itemBinary);
	}

	public void addNewItem(ItemBooleanXml itemBoolean) {
		newItems.add(itemBoolean);
	}

	public void addNewItem(ItemEnumXml itemEnum) {
		newItems.add(itemEnum);
	}

	public void addNewItem(ItemIntegerXml itemInteger) {
		newItems.add(itemInteger);
	}

	public void addNewItem(ItemLinkXml itemLink) {
		newItems.add(itemLink);		
	}

	public void addNewItem(ItemStringXml itemString) {
		newItems.add(itemString);		
	}

	public void addNewItem(ItemUnitDateXml itemUnitDate) {
		newItems.add(itemUnitDate);		
	}

	public void addNewItem(ItemEntityRefXml itemEntityRef) {
		newItems.add(itemEntityRef);
	}

	public void addChanged(ApBindingItem bindingItem, Object itemXml) {
		ChangedBindedItem cbi = new ChangedBindedItem(bindingItem, itemXml);
		changedItems.add(cbi);
	}

	public int getItemCount() {
		return newItems.size() + changedItems.size() + notChangeItems.size();
	}

}
