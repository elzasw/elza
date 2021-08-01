package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.CachedBinding;
import cz.tacr.elza.service.cache.CachedPart;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
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
     * Creates value object from ApBindingState.
     *
     * @param bindingState
     * @param lastChange
     * @return ApBindingVO
     */
    public static ApBindingVO newInstance(ApBindingState bindingState, ApChange lastChange) {
        ApBindingVO vo = new ApBindingVO();
        vo.setId(bindingState.getBinding().getBindingId());
        vo.setExternalSystemCode(bindingState.getBinding().getApExternalSystem().getCode());
        vo.setValue(bindingState.getBinding().getValue());
        vo.setExtState(bindingState.getExtState());
        vo.setExtRevision(bindingState.getExtRevision());
        vo.setExtUser(bindingState.getExtUser());
        vo.setExtReplacedBy(bindingState.getExtReplacedBy());
        vo.setSyncState(createSyncStateVO(bindingState, lastChange));
        return vo;
    }

    /**
     * Creates value object from CachedBinding and List<CachedPart>.
     *
     * @param binding
     * @param parts
     * @return ApBindingVO
     */
    public static ApBindingVO newInstance(CachedBinding binding, List<CachedPart> parts, ApChange lastChange) {
        ApBindingVO vo = newInstance(binding.getBindingState(), lastChange);
        vo.setBindingItemList(createApBindingItemsVO(binding, parts));
        return vo;
    }

    private static SyncStateVO createSyncStateVO(ApBindingState bindingState, ApChange lastChange) {
        if (bindingState.getSyncOk() != null) {
            SyncStateVO syncState;
            switch (bindingState.getSyncOk()) {
            case SYNC_OK:
                if (lastChange.getChangeId() > bindingState.getSyncChangeId()) {
                    syncState = SyncStateVO.LOCAL_CHANGE;
                } else {
                    syncState = SyncStateVO.SYNC_OK;
                }
                break;
            case NOT_SYNCED:
                syncState = SyncStateVO.NOT_SYNCED;
                break;
            default:
                throw new SystemException("Chyba datových polí ApBindingState.SyncOk");
            }
            return syncState;
        }
        return null;
    }

    private static List<ApBindingItemVO> createApBindingItemsVO(CachedBinding binding, List<CachedPart> parts) {
        List<ApBindingItemVO> bindingItemVOList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(binding.getBindingItemList())) {
            for (ApBindingItem bindingItem : binding.getBindingItemList()) {
                bindingItemVOList.add(createApBindingItemVO(bindingItem, binding.getBindingState(), parts));
            }
        }
        return bindingItemVOList;
    }

    private static ApBindingItemVO createApBindingItemVO(ApBindingItem bindingItem, ApBindingState bindingState, List<CachedPart> parts) {
        ApBindingItemVO bindingItemVO = new ApBindingItemVO();
        bindingItemVO.setValue(bindingItem.getValue());
        bindingItemVO.setPartId(bindingItem.getPartId());
        bindingItemVO.setItemId(bindingItem.getItemId());
        bindingItemVO.setSync(getSync(bindingItem, bindingState.getSyncChangeId(), parts));
        return bindingItemVO;
    }

    private static Boolean getSync(ApBindingItem bindingItem, Integer syncChangeId, List<CachedPart> parts) {
        if (bindingItem.getItemId() != null) {
            return getSyncFromItem(bindingItem.getItemId(), syncChangeId, parts);
        } else {
            return getSyncFromPart(bindingItem.getPartId(), syncChangeId, parts);
        }
    }

    private static Boolean getSyncFromItem(Integer itemId, Integer syncChangeId, List<CachedPart> parts) {
        if (CollectionUtils.isNotEmpty(parts)) {
            for (CachedPart part : parts) {
                if (CollectionUtils.isNotEmpty(part.getItems())) {
                    for (ApItem item : part.getItems()) {
                        if (item.getItemId().equals(itemId)) {
                            return syncChangeId >= item.getCreateChangeId();
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Boolean getSyncFromPart(Integer partId, Integer syncChangeId, List<CachedPart> parts) {
        if (CollectionUtils.isNotEmpty(parts)) {
            for (CachedPart part : parts) {
                if (part.getPartId().equals(partId)) {
                    return syncChangeId >= part.getLastChangeId();
                }
            }
        }
        return false;
    }

}
