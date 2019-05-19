package cz.tacr.elza.bulkaction.generator.multiple;

public class StorageOrderGeneratorConfig
        implements ActionConfig {

    String storageItemType;
    String orderItemType;

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
