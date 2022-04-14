package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.CachedBinding;
import cz.tacr.elza.service.cache.CachedPart;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApBindingVO {

    private Integer id;

    private Integer externalSystemId;

    @Deprecated
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

    public Integer getExternalSystemId() {
        return externalSystemId;
    }

    public void setExternalSystemId(Integer externalSystemId) {
        this.externalSystemId = externalSystemId;
    }

    @Deprecated
    public String getExternalSystemCode() {
        return externalSystemCode;
    }

    @Deprecated
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
     * @param nonBindedItems Flag if exists non binded items or parts, it means local change
     * @return ApBindingVO
     */
    public static ApBindingVO newInstance(ApBindingState bindingState, ApChange lastChange, boolean nonBindedItems) {
        ApBindingVO vo = new ApBindingVO();
        vo.setId(bindingState.getBinding().getBindingId());
        vo.setExternalSystemId(bindingState.getBinding().getApExternalSystem().getExternalSystemId());
        vo.setExternalSystemCode(bindingState.getBinding().getApExternalSystem().getCode());
        vo.setValue(bindingState.getBinding().getValue());
        vo.setExtState(bindingState.getExtState());
        vo.setExtRevision(bindingState.getExtRevision());
        vo.setExtUser(bindingState.getExtUser());
        vo.setExtReplacedBy(bindingState.getExtReplacedBy());
        vo.setSyncState(createSyncStateVO(bindingState, lastChange, nonBindedItems));
        return vo;
    }
    
    public static ApBindingVO newInstance(CachedBinding binding, List<CachedPart> parts, ApChange lastChange) {
    	List<ApBindingItem> bindingItemList = binding.getBindingItemList();
    	ApBindingState bindingState = binding.getBindingState();
    	return newInstance(bindingState, bindingItemList, parts, lastChange);
    }

    /**
     * Creates value object from CachedBinding and List<CachedPart>.
     *
     * @param binding
     * @param parts
     * @return ApBindingVO
     */
    public static ApBindingVO newInstance(ApBindingState bindingState, List<ApBindingItem> bindingItemList, 
    		List<CachedPart> parts, ApChange lastChange) {
    	
        List<ApBindingItemVO> bindingItemVOList = new ArrayList<>();
        Map<Integer, ApBindingItemVO> bindedParts = new HashMap<>(), 
        		bindedItems = new HashMap<>();

        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
            	ApBindingItemVO bivo = ApBindingItemVO.newInstance(bindingItem);
            	
            	if(bindingItem.getPartId()!=null) {
            		bindedParts.put(bindingItem.getPartId(), bivo);
            	} else 
                if(bindingItem.getItemId()!=null) {
                	bindedItems.put(bindingItem.getItemId(), bivo);
                }
            	
                bindingItemVOList.add(bivo);
            }
        }
        
        // set sync state        
        boolean nonBindedItems = false;
        if(CollectionUtils.isNotEmpty(parts)) {
        	Integer syncChangeId = bindingState.getSyncChangeId();
            for(CachedPart part: parts) {
            	ApBindingItemVO bindedPart = bindedParts.get(part.getPartId());
            	if(bindedPart==null) {
            		nonBindedItems = true;
            	} else {
            		bindedPart.setSync(syncChangeId >= part.getCreateChangeId());
            	}
            	if(CollectionUtils.isNotEmpty(part.getItems())) {
                	for(ApItem item : part.getItems()) {
                		ApBindingItemVO bindedItem = bindedItems.get(item.getItemId());
                		if(bindedItem==null) {
                			nonBindedItems = true;
                    	} else {
                    		bindedItem.setSync(syncChangeId >= item.getCreateChangeId());
                    	}
                	}            		
            	}
            }
        }
    	
        ApBindingVO vo = newInstance(bindingState, lastChange, nonBindedItems);
        vo.setBindingItemList(bindingItemVOList);
        return vo;
    }
    
	public static ApBindingVO newInstance(ApBindingState bindingState, 
			List<ApBindingItem> bindingItemList,
			List<ApPart> parts, 
			Map<Integer, List<ApItem>> items, ApChange lastChange) {
		
        List<ApBindingItemVO> bindingItemVOList = new ArrayList<>();
        Map<Integer, ApBindingItemVO> bindedParts = new HashMap<>(), 
        		bindedItems = new HashMap<>();

        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
            	ApBindingItemVO bivo = ApBindingItemVO.newInstance(bindingItem);
            	
            	if(bindingItem.getPartId()!=null) {
            		bindedParts.put(bindingItem.getPartId(), bivo);
            	} else 
                if(bindingItem.getItemId()!=null) {
                	bindedItems.put(bindingItem.getItemId(), bivo);
                }
            	
                bindingItemVOList.add(bivo);
            }
        }
        
        // set sync state        
        boolean nonBindedItems = false;
        if(CollectionUtils.isNotEmpty(parts)) {
        	Integer syncChangeId = bindingState.getSyncChangeId();
            for(ApPart part: parts) {
            	ApBindingItemVO bindedPart = bindedParts.get(part.getPartId());
            	if(bindedPart==null) {
            		nonBindedItems = true;
            	} else {
            		bindedPart.setSync(syncChangeId >= part.getCreateChangeId());
            	}
            	List<ApItem> itemList = items.get(part.getPartId());
            	if(CollectionUtils.isNotEmpty(itemList)) {
                	for(ApItem item : itemList) {
                		ApBindingItemVO bindedItem = bindedItems.get(item.getItemId());
                		if(bindedItem==null) {
                			nonBindedItems = true;
                    	} else {
                    		bindedItem.setSync(syncChangeId >= item.getCreateChangeId());
                    	}
                	}            		
            	}
            }
        }
    	
        ApBindingVO vo = newInstance(bindingState, lastChange, nonBindedItems);
        vo.setBindingItemList(bindingItemVOList);
        return vo;
	}


    private static SyncStateVO createSyncStateVO(ApBindingState bindingState, ApChange lastChange, boolean nonBindedItems) {
    	SyncStateVO syncState;
    	
        if (bindingState.getSyncOk() != null) {            
            switch (bindingState.getSyncOk()) {
            case SYNC_OK:
                if (lastChange.getChangeId() > bindingState.getSyncChangeId() || nonBindedItems) {
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
        } else {
        	if(nonBindedItems) {
        		syncState = SyncStateVO.LOCAL_CHANGE;
        	} else {
        		syncState = null;
        	}
        }
        return syncState;
    }
}
