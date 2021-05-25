package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.exception.SystemException;

import java.util.List;

public class ApBindingVO {

    private Integer id;

    private String externalSystemCode;
    
    private String value;

    private String detailUrl;

    private String extState;

    private String extRevision;

    private String extUser;

    private String extReplacedBy;

    private String detailUrlExtReplacedBy;

    private SyncStateVO syncState;

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

    public SyncStateVO getSyncState() {
        return syncState;
    }

    public void setSyncState(SyncStateVO syncState) {
        this.syncState = syncState;
    }

    public List<ApBindingItemVO> getBindingItemList() {
        return bindingItemList;
    }

    public void setBindingItemList(List<ApBindingItemVO> bindingItemList) {
        this.bindingItemList = bindingItemList;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(final String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public String getDetailUrlExtReplacedBy() {
        return detailUrlExtReplacedBy;
    }

    public void setDetailUrlExtReplacedBy(final String detailUrlExtReplacedBy) {
        this.detailUrlExtReplacedBy = detailUrlExtReplacedBy;
    }

    /**
     * Creates value object from AP external id.
     */
    public static ApBindingVO newInstance(ApBindingState src, ApChange lastChange) {
        ApBindingVO vo = new ApBindingVO();
        vo.setId(src.getBinding().getBindingId());
        vo.setExternalSystemCode(src.getBinding().getApExternalSystem().getCode());
        vo.setValue(src.getBinding().getValue());
        vo.setExtState(src.getExtState());
        vo.setExtRevision(src.getExtRevision());
        vo.setExtUser(src.getExtUser());
        vo.setExtReplacedBy(src.getExtReplacedBy());
        switch (src.getSyncOk()) {
        case SYNC_OK:
            if (lastChange.getChangeId() > src.getSyncChangeId()) {
                vo.setSyncState(SyncStateVO.LOCAL_CHANGE);
            } else {
                vo.setSyncState(SyncStateVO.SYNC_OK);
            }
            break;
        case NOT_SYNCED:
            vo.setSyncState(SyncStateVO.NOT_SYNCED);
            break;
        default:
            throw new SystemException("Chyba datových polí ApBindingState.SyncOk");
        }
        return vo;
    }
}
