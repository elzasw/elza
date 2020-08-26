package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import org.springframework.lang.Nullable;

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
    public static ApBindingItemVO newInstance(@Nullable ApBindingState state, ApBindingItem src) {
        ApBindingItemVO vo = new ApBindingItemVO();
        vo.setValue(src.getValue());
        ApItem item = src.getItem();
        ApPart part = src.getPart();
        vo.setItemId(item != null ? item.getItemId() : null);
        vo.setPartId(part != null ? part.getPartId() : null);
        Integer changeId;
        if (item != null) {
            changeId = item.getCreateChange().getChangeId();
        } else if (part != null) {
            changeId = part.getCreateChange().getChangeId();
        } else {
            throw new IllegalStateException();
        }
        if (state == null || state.getSyncChange() == null) {
            vo.setSync(false);
        } else {
            Integer stateChangeId = state.getSyncChange().getChangeId();
            vo.setSync(stateChangeId >= changeId);
        }

        return vo;
    }
}
