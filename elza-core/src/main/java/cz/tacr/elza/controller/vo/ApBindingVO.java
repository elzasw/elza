package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.SyncState;

import java.util.List;

public class ApBindingVO {

    private Integer id;

    private String externalSystemCode;
    
    private String value;

    private String extState;

    private String extRevision;

    private String extUser;

    private String extReplacedBy;

    private SyncState syncState;

    private List<ApBindingItemVO> bindingItemList;

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

    public String getExtState() {
        return extState;
    }

    public void setExtState(String extState) {
        this.extState = extState;
    }

    public String getExtRevision() {
        return extRevision;
    }

    public void setExtRevision(String extRevision) {
        this.extRevision = extRevision;
    }

    public String getExtUser() {
        return extUser;
    }

    public void setExtUser(String extUser) {
        this.extUser = extUser;
    }

    public String getExtReplacedBy() {
        return extReplacedBy;
    }

    public void setExtReplacedBy(String extReplacedBy) {
        this.extReplacedBy = extReplacedBy;
    }

    public SyncState getSyncState() {
        return syncState;
    }

    public void setSyncState(SyncState syncState) {
        this.syncState = syncState;
    }

    public List<ApBindingItemVO> getBindingItemList() {
        return bindingItemList;
    }

    public void setBindingItemList(List<ApBindingItemVO> bindingItemList) {
        this.bindingItemList = bindingItemList;
    }

    /**
     * Creates value object from AP external id.
     */
    public static ApBindingVO newInstance(ApBindingState src) {
        ApBindingVO vo = new ApBindingVO();
        vo.setId(src.getBinding().getBindingId());
        vo.setExternalSystemCode(src.getBinding().getApExternalSystem().getCode());
        vo.setValue(src.getBinding().getValue());
        vo.setExtState(src.getExtState());
        vo.setExtRevision(src.getExtRevision());
        vo.setExtUser(src.getExtUser());
        vo.setExtReplacedBy(src.getExtReplacedBy());
        vo.setSyncState(src.getSyncOk());
        return vo;
    }
}
