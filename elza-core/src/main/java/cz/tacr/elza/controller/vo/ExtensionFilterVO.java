package cz.tacr.elza.controller.vo;


public class ExtensionFilterVO {

    private String partTypeCode;

    private Integer itemTypeId;

    private Integer itemSpecId;

    private Object value;

    public String getPartTypeCode() {
        return partTypeCode;
    }

    public void setPartTypeCode(String partTypeCode) {
        this.partTypeCode = partTypeCode;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public Integer getItemSpecId() {
        return itemSpecId;
    }

    public void setItemSpecId(Integer itemSpecId) {
        this.itemSpecId = itemSpecId;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
