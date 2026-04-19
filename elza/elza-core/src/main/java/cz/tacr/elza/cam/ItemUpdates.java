package cz.tacr.elza.cam;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ApBindingItem;

/**
 * Collection of items updates.
 *
 * Contains list of changed, not changed and new items.
 */
public class ItemUpdates {

    public static class ChangedBindedItem {
        final ApBindingItem bindingItem;
        final Object xmlItem;

        public ChangedBindedItem(ApBindingItem bindingItem, Object xmlItem) {
            this.bindingItem = bindingItem;
            this.xmlItem = xmlItem;
        }

        public ApBindingItem getBindingItem() {
            return bindingItem;
        }

        public Object getXmlItem() {
            return xmlItem;
        }
    }

    private final List<ApBindingItem> notChangeItems = new ArrayList<>();
    private final List<ChangedBindedItem> changedItems = new ArrayList<>();
    private final List<Object> newItems = new ArrayList<>();

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

    public void addNewItem(Object xmlItem) {
        newItems.add(xmlItem);
    }

    public void addChanged(ApBindingItem bindingItem, Object itemXml) {
        changedItems.add(new ChangedBindedItem(bindingItem, itemXml));
    }

    public int getItemCount() {
        return newItems.size() + changedItems.size() + notChangeItems.size();
    }
}
