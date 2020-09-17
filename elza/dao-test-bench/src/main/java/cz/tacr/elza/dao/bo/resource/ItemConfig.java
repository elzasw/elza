package cz.tacr.elza.dao.bo.resource;

public abstract class ItemConfig {

    private String itemType;
    private String itemSpec;
    private Boolean readOnly;

    abstract public Object getItem();

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

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

}
