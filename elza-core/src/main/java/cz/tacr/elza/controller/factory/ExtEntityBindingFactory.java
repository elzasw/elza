package cz.tacr.elza.controller.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.cam.v1.schema.cam.EntityRecordStateXml;
import cz.tacr.elza.controller.vo.ExtEntityBinding;
import cz.tacr.elza.controller.vo.ExtIssueIconState;
import cz.tacr.elza.controller.vo.ExtIssueSummary;
import cz.tacr.elza.controller.vo.ExtItemBinding;
import cz.tacr.elza.controller.vo.SyncState;
import cz.tacr.elza.domain.ApBindingIssue;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedBinding;
import cz.tacr.elza.service.cache.CachedPart;

/**
 * Builds {@link ExtEntityBinding} (the generated API DTO) from domain entities or
 * cache snapshots. Computes the sync-state badge and the per-part/item sync flags.
 */
public final class ExtEntityBindingFactory {

    private ExtEntityBindingFactory() {
    }

    public static ExtEntityBinding newInstance(CachedBinding binding, CachedAccessPoint cachedAccessPoint,
                                               ApChange lastChange) {
        return newInstance(binding.getBindingState(), binding.getBindingItemList(), cachedAccessPoint, lastChange);
    }

    public static ExtEntityBinding newInstance(ApBindingState bindingState, List<ApBindingItem> bindingItemList,
                                               CachedAccessPoint cachedAccessPoint, ApChange lastChange) {
        List<CachedPart> parts = cachedAccessPoint.getParts();

        List<ExtItemBinding> bindingItemVOList = new ArrayList<>();
        Map<Integer, ExtItemBinding> bindedParts = new HashMap<>();
        Map<Integer, ExtItemBinding> bindedItems = new HashMap<>();

        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                ExtItemBinding bivo = newItemInstance(bindingItem);
                if (bindingItem.getPartId() != null) {
                    bindedParts.put(bindingItem.getPartId(), bivo);
                } else if (bindingItem.getItemId() != null) {
                    bindedItems.put(bindingItem.getItemId(), bivo);
                }
                bindingItemVOList.add(bivo);
            }
        }

        boolean otherLocalChange = false;
        if (CollectionUtils.isNotEmpty(parts)) {
            Integer syncChangeId = bindingState.getCreateChangeId();
            for (CachedPart part : parts) {
                ExtItemBinding bindedPart = bindedParts.get(part.getPartId());
                if (bindedPart == null) {
                    otherLocalChange = true;
                } else {
                    bindedPart.setSync(syncChangeId >= part.getCreateChangeId());
                }
                if (CollectionUtils.isNotEmpty(part.getItems())) {
                    for (ApItem item : part.getItems()) {
                        ExtItemBinding bindedItem = bindedItems.get(item.getItemId());
                        if (bindedItem == null) {
                            otherLocalChange = true;
                        } else {
                            boolean synced = syncChangeId >= item.getCreateChangeId();
                            bindedItem.setSync(synced);
                            if (!synced) {
                                bindedParts.get(item.getPartId()).setSync(synced);
                            }
                        }
                    }
                }
            }
        }

        if (!Objects.equals(cachedAccessPoint.getPreferredPartId(), bindingState.getPreferredPartId())) {
            otherLocalChange = true;
        }
        if (!Objects.equals(cachedAccessPoint.getApState().getApTypeId(), bindingState.getApTypeId())) {
            otherLocalChange = true;
        }
        otherLocalChange = otherLocalChange || externalStateMismatch(bindingState, cachedAccessPoint.getApState().getStateApproval());

        ExtEntityBinding vo = newInstance(bindingState, lastChange, otherLocalChange);
        vo.setBindingItemList(bindingItemVOList);
        return vo;
    }

    public static ExtEntityBinding newInstance(ApBindingState bindingState,
                                               ApState state,
                                               List<ApBindingItem> bindingItemList,
                                               List<ApPart> parts,
                                               Map<Integer, List<ApItem>> items,
                                               ApChange lastChange) {
        List<ExtItemBinding> bindingItemVOList = new ArrayList<>();
        Map<Integer, ExtItemBinding> bindedParts = new HashMap<>();
        Map<Integer, ExtItemBinding> bindedItems = new HashMap<>();

        if (CollectionUtils.isNotEmpty(bindingItemList)) {
            for (ApBindingItem bindingItem : bindingItemList) {
                ExtItemBinding bivo = newItemInstance(bindingItem);
                if (bindingItem.getPartId() != null) {
                    bindedParts.put(bindingItem.getPartId(), bivo);
                } else if (bindingItem.getItemId() != null) {
                    bindedItems.put(bindingItem.getItemId(), bivo);
                }
                bindingItemVOList.add(bivo);
            }
        }

        boolean otherLocalChange = false;
        if (CollectionUtils.isNotEmpty(parts)) {
            Integer syncChangeId = bindingState.getCreateChangeId();
            for (ApPart part : parts) {
                ExtItemBinding bindedPart = bindedParts.get(part.getPartId());
                if (bindedPart == null) {
                    otherLocalChange = true;
                } else {
                    bindedPart.setSync(syncChangeId >= part.getCreateChangeId());
                }
                List<ApItem> itemList = items.get(part.getPartId());
                if (CollectionUtils.isNotEmpty(itemList)) {
                    for (ApItem item : itemList) {
                        ExtItemBinding bindedItem = bindedItems.get(item.getItemId());
                        if (bindedItem == null) {
                            otherLocalChange = true;
                        } else {
                            boolean synced = syncChangeId >= item.getCreateChangeId();
                            bindedItem.setSync(synced);
                            if (!synced) {
                                bindedParts.get(item.getPartId()).setSync(synced);
                            }
                        }
                    }
                }
            }
        }
        if (!Objects.equals(state.getApTypeId(), bindingState.getApTypeId())) {
            otherLocalChange = true;
        }
        if (!Objects.equals(state.getAccessPoint().getPreferredPartId(), bindingState.getPreferredPartId())) {
            otherLocalChange = true;
        }

        ExtEntityBinding vo = newInstance(bindingState, lastChange, otherLocalChange);
        vo.setBindingItemList(bindingItemVOList);
        return vo;
    }

    private static ExtEntityBinding newInstance(ApBindingState bindingState, ApChange lastChange,
                                                boolean otherLocalChange) {
        ExtEntityBinding vo = new ExtEntityBinding();
        vo.setId(bindingState.getBinding().getBindingId());
        vo.setExternalSystemId(bindingState.getBinding().getApExternalSystem().getExternalSystemId());
        vo.setExternalSystemCode(bindingState.getBinding().getApExternalSystem().getCode());
        vo.setValue(bindingState.getBinding().getValue());
        vo.setExtState(bindingState.getExtState());
        vo.setExtRevision(bindingState.getExtRevision());
        vo.setExtUser(bindingState.getExtUser());
        vo.setExtReplacedBy(bindingState.getExtReplacedBy());
        vo.setExtMetadataRevision(bindingState.getExtMetadataRevision());
        vo.setExtPrevRevision(bindingState.getExtPrevRevision());
        vo.setExtCreatedAt(bindingState.getExtCreatedAt());
        vo.setSyncState(createSyncState(bindingState, lastChange, otherLocalChange));
        return vo;
    }

    /**
     * Aggregate a binding's issues into the badge embedded in {@link ExtEntityBinding}.
     * Returns {@code null} when there are no issues (the wire field is then absent).
     * Icon state, from most to least prominent:
     * <ul>
     *   <li>{@code ATTENTION} — any ERROR, or any status {@code IR_FIX_NEEDED},</li>
     *   <li>{@code NEW} — at least one WARNING with no status yet,</li>
     *   <li>{@code NEUTRAL} — only WARNINGs already resolved.</li>
     * </ul>
     */
    public static ExtIssueSummary issueSummary(List<ApBindingIssue> issues) {
        if (CollectionUtils.isEmpty(issues)) {
            return null;
        }
        boolean attention = false;
        boolean isNew = false;
        for (ApBindingIssue issue : issues) {
            if (issue.getSeverity() == ApBindingIssue.Severity.ERROR
                    || issue.getStatus() == ApBindingIssue.Status.IR_FIX_NEEDED) {
                attention = true;
            } else if (issue.getSeverity() == ApBindingIssue.Severity.WARNING && issue.getStatus() == null) {
                isNew = true;
            }
        }
        ExtIssueIconState iconState = attention ? ExtIssueIconState.ATTENTION
                : isNew ? ExtIssueIconState.NEW
                : ExtIssueIconState.NEUTRAL;

        ExtIssueSummary summary = new ExtIssueSummary();
        summary.setCount(issues.size());
        summary.setIconState(iconState);
        return summary;
    }

    private static ExtItemBinding newItemInstance(ApBindingItem src) {
        ExtItemBinding vo = new ExtItemBinding();
        vo.setPartId(src.getPartId());
        vo.setItemId(src.getItemId());
        return vo;
    }

    private static boolean externalStateMismatch(ApBindingState bindingState, StateApproval localState) {
        String extState = bindingState.getExtState();
        if (extState == null) {
            return false;
        }
        if (extState.equals(EntityRecordStateXml.ERS_NEW.toString())) {
            return localState == StateApproval.APPROVED;
        }
        if (extState.equals(EntityRecordStateXml.ERS_APPROVED.toString())) {
            return localState != StateApproval.APPROVED;
        }
        return false;
    }

    private static SyncState createSyncState(ApBindingState bindingState, ApChange lastChange, boolean otherLocalChange) {
        if (bindingState.getSyncOk() != null) {
            switch (bindingState.getSyncOk()) {
                case SYNC_OK:
                    if (lastChange.getChangeId() > bindingState.getCreateChangeId() || otherLocalChange) {
                        return SyncState.LOCAL_CHANGE;
                    }
                    return SyncState.SYNC_OK;
                case NOT_SYNCED:
                    return SyncState.NOT_SYNCED;
                default:
                    throw new SystemException("Chyba datových polí ApBindingState.SyncOk");
            }
        }
        return otherLocalChange ? SyncState.LOCAL_CHANGE : null;
    }
}
