package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;

import java.util.List;

public class CachedBinding implements AccessPointCacheSerializable {

    private Integer id;

    private String externalSystemCode;

    private String value;

    private ApBindingState bindingState;

    private List<ApBindingItem> bindingItemList;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExternalSystemCode() {
        return externalSystemCode;
    }

    public void setExternalSystemCode(String externalSystemCode) {
        this.externalSystemCode = externalSystemCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ApBindingState getBindingState() {
        return bindingState;
    }

    public void setBindingState(ApBindingState bindingState) {
        this.bindingState = bindingState;
    }

    public List<ApBindingItem> getBindingItemList() {
        return bindingItemList;
    }

    public void setBindingItemList(List<ApBindingItem> bindingItemList) {
        this.bindingItemList = bindingItemList;
    }
}
