package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingItem;

public class ApBindingItemVO {

    private String value;

    private Integer partId;

    private Integer itemId;

    private Boolean sync;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(Integer partId) {
        this.partId = partId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Boolean getSync() {
        return sync;
    }

    public void setSync(Boolean sync) {
        this.sync = sync;
    }

    /**
     * Creates value object from AP external id.
     */
    public static ApBindingItemVO newInstance(ApBindingItem src) {
        ApBindingItemVO vo = new ApBindingItemVO();
        vo.setValue(src.getValue());
        vo.setSync(src.getCamIdentifier());
        vo.setItemId(src.getItem() != null ? src.getItem().getItemId() : null);
        vo.setPartId(src.getPart() != null ? src.getPart().getPartId() : null);
        return vo;
    }
}
