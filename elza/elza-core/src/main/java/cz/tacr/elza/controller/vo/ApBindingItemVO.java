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
     * Create value object without sync state
     * 
     * Sync state has to be set in extra logic.
     * 
     * @param src
     * @return
     */
    public static ApBindingItemVO newInstance(ApBindingItem src) {
    	ApBindingItemVO bindingItemVO = new ApBindingItemVO();
    	
        bindingItemVO.setValue(src.getValue());
        bindingItemVO.setPartId(src.getPartId());
        bindingItemVO.setItemId(src.getItemId());
    	
        return bindingItemVO;
    }
}
