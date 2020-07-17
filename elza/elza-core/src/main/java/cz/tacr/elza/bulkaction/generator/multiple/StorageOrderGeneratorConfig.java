package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class StorageOrderGeneratorConfig
        implements ActionConfig {

    public static class WhenCondition {
        String itemType;
        List<String> itemSpecs;

        public String getItemType() {
            return itemType;
        }

        public void setItemType(String itemType) {
            this.itemType = itemType;
        }

        public List<String> getItemSpecs() {
            return itemSpecs;
        }

        public void setItemSpecs(List<String> itemSpecs) {
            this.itemSpecs = itemSpecs;
        }
    };

    String storageItemType;
    String orderItemType;
    List<WhenCondition> whenStorage;

    public List<WhenCondition> getWhenStorage() {
        return whenStorage;
    }

    public void setWhenStorage(List<WhenCondition> whenStorage) {
        this.whenStorage = whenStorage;
    }

    public String getStorageItemType() {
        return storageItemType;
    }

    public void setStorageItemType(String storageItemType) {
        this.storageItemType = storageItemType;
    }

    public String getOrderItemType() {
        return orderItemType;
    }

    public void setOrderItemType(String orderItemType) {
        this.orderItemType = orderItemType;
    }

    @Override
    public Class<? extends Action> getActionClass() {
        return StorageOrderGeneratorAction.class;
    }

}
