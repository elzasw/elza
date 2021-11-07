package cz.tacr.elza.dataexchange.input.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.dataexchange.common.items.ImportableItemData;
import cz.tacr.elza.dataexchange.input.parts.context.ItemWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartsContext;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.schema.v2.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointsContext;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;

/**
 * Processing access point entries for access points or parties. Implementation
 * is not thread-safe.
 * <p>
 * When AP storage updates persist type: <br>
 * 1) CREATE -> all sub entities (also party) will be created <br>
 * 2) UPDATE -> <br>
 * AP: <br>
 * - existing AP was paired by UUID or external id <br>
 * - storage will ignore AP entity (update not needed) <br>
 * - persist type in AP info will be set to UPDATE <br>
 * - all sub entities will be invalidate (set deleteChangeId) <br>
 * - all imported sub entities will be created <br>
 * PARTY: <br>
 * - party will read UPDATE type from AP info <br>
 * - party entity must be updated <br>
 * - all current sub entities must be deleted <br>
 * - all imported sub entities will be created <br>
 * 3) NONE -> all AP related entities (party included) will be ignored
 */
public class AccessPointEntryProcessor implements ItemProcessor {

    protected final AccessPointsContext context;

    protected final PartsContext partsContext;

    protected final ImportContext importContext;

    protected final StaticDataProvider staticData;

    protected final boolean partyRelated;

    protected String entryId;

    protected AccessPointInfo apInfo;

    public AccessPointEntryProcessor(ImportContext context, boolean partyRelated) {
        this.context = context.getAccessPoints();
        this.staticData = context.getStaticData();
        this.partyRelated = partyRelated;
        this.importContext = context;
        this.partsContext = context.getParts();
    }

    @Override
    public void process(Object item) {
        processEntry((Party) item);
    }

    protected void proccessAp(AccessPoint ap) {
        AccessPointEntry entry = ap.getApe();
        entryId = entry.getId();
        ApEntity entity = createEntity(entry);
        List<ApBindingState> eids = createExternalIds(entry.getEid());
        if (ap.getFrgs() == null) {
            throw new NotImplementedException("Nejsou vyplněné fragmenty u " + entryId + ", je potřeba použít nový zápis!");
        } else {
            List<PartWrapper> parts = createParts(ap.getFrgs());
            apInfo = context.addAccessPoint(entity.accessPoint, entry.getId(), entity.state, eids, parts);
        }
    }

    // obsolete - can be removed
    protected void processEntry(Party party) {
        AccessPointEntry entry = party.getApe();
        entryId = entry.getId();
        // create AP and prepare AP info
        ApEntity entity = createEntity(entry);
        List<ApBindingState> eids = createExternalIds(entry.getEid());
        apInfo = context.addAccessPoint(entity.accessPoint, party.getId(), entity.state, eids, null);

    }

    private List<ApBindingState> createExternalIds(Collection<ExternalId> eids) {
        if (eids.isEmpty()) {
            return null;
        }
        List<ApBindingState> entities = new ArrayList<>(eids.size());
        for (ExternalId eid : eids) {
            if (StringUtils.isEmpty(eid.getT())) {
                throw new DEImportException("External id type is not set, apeId=" + entryId);
            }
            if (StringUtils.isEmpty(eid.getV())) {
                throw new DEImportException("External id without value, apeId=" + entryId);
            }
            ApExternalSystem apExternalSystem = context.getApExternalSystem(eid.getT());
            if (apExternalSystem == null) {
                throw new DEImportException("External id type not found, apEid=" + eid.getV() + ", code=" + eid.getT());
            }
            // create external id
            ApBinding entity = new ApBinding();
            entity.setValue(eid.getV());
            entity.setApExternalSystem(apExternalSystem);

            ApBindingState state = new ApBindingState();
            state.setCreateChange(context.getCreateChange());
            state.setApExternalSystem(apExternalSystem);
            state.setBinding(entity);

            entities.add(state);
        }
        return entities;
    }

    private List<PartWrapper> createParts(Fragments fragments) {
        if (fragments.getFrg().isEmpty()) {
            return null;
        }

        List<PartWrapper> wrapperParts = new ArrayList<>(fragments.getFrg().size());
        for (Fragment fragment : fragments.getFrg()) {
            if (StringUtils.isEmpty(fragment.getT())) {
                throw new DEImportException("Fragment id type is not set, fragmentId=" + fragment.getFid());
            }
            RulPartType partType = context.getRulPartType(fragment.getT());
            if (partType == null) {
                throw new DEImportException("Part type not found, fragmentId=" + fragment.getFid() + ", " + fragment.getT());
            }
            //create Ap Part
            ApPart partEntity = createPart(partType, apInfo);
            List<ItemWrapper> itemWrapperList = processItems(partEntity, fragment.getDdOrDoOrDp(), fragment.getFid());
            PartInfo partInfo = new PartInfo(fragment.getFid(), apInfo, partType, partsContext);
            PartWrapper partWrapper = new PartWrapper(partEntity, partInfo, itemWrapperList);

            //parent part
            if(fragment.getPid() != null) {
                context.addToParentPartIdMap(partWrapper, fragment.getPid());
            }

            wrapperParts.add(partWrapper);

        }
        return wrapperParts;
    }

    private List<ItemWrapper> processItems(ApPart partEntity, List<DescriptionItem> itemList, String fragmentId) {
        List<ItemWrapper> itemWrapperList = new ArrayList<>();
        for (DescriptionItem di : itemList) {
            ItemType itemType = staticData.getItemTypeByCode(di.getT());
            if (itemType == null) {
                throw new DEImportException("Description item type not found, code:" + di.getT());
            }
            DataType dataType = itemType.getDataType();

            if (itemType.getDataType() == DataType.STRING && itemType.getEntity().getStringLengthLimit() != null) {
                if (((DescriptionItemString) di).getV().length() > itemType.getEntity().getStringLengthLimit()) {
                    throw new BusinessException("Délka řetězce : " + ((DescriptionItemString) di).getV()
                            + " je delší než maximální povolená : " + itemType.getEntity().getStringLengthLimit(), BaseCode.INVALID_LENGTH);
                }
            }

            ImportableItemData itemData = di.createData(importContext, dataType);
            ArrData data = itemData.getData();
            ApItem apItem = createApItem(partEntity, data, itemType, itemWrapperList);

            if (di.getS() != null && !di.getS().isEmpty()) {
                apItem.setItemSpec(resolveItemSpec(itemType, di.getS()));
            }

            ItemWrapper itemWrapper = partsContext.addItem(apItem, partsContext.getPartInfo(fragmentId));
            itemWrapperList.add(itemWrapper);
        }

        return itemWrapperList;
    }

    protected ApPart createPart(RulPartType type, AccessPointInfo apInfo) {
        ApPart entity;
        try {
            entity = new ApPart();
        } catch (Exception e) {
            throw new SystemException("Failed to intialized no arg constructor, entity: " + ApPart.class, e);
        }
        entity.setPartType(type);
        entity.setCreateChange(context.getCreateChange());
        entity.setLastChange(context.getCreateChange());
        entity.setState(ApStateEnum.OK);

        return entity;
    }

    private ApItem createApItem(ApPart partEntity, ArrData data, ItemType itemType, List<ItemWrapper> itemList) {
        ApItem apItem = new ApItem();
        apItem.setPart(partEntity);
        apItem.setData(data);
        apItem.setItemType(itemType.getEntity());
        apItem.setCreateChange(context.getCreateChange());
        apItem.setObjectId(context.nextItemObjectId());
        apItem.setPosition(nextPosition(itemList));
        return apItem;
    }

    private int nextPosition(List<ItemWrapper> itemWrapperList) {
        if (itemWrapperList.size() == 0) {
            return 1;
        }
        int position = 2;
        for (ItemWrapper existsItem : itemWrapperList) {
            ApItem apItem = (ApItem) existsItem.getEntity();
            if (apItem.getDeleteChange() == null) {
                if (apItem.getPosition() >= position) {
                    position = apItem.getPosition() + 1;
                }
            }
        }
        return position;
    }

    public static RulItemSpec resolveItemSpec(ItemType rsit, String specCode) {
        boolean specCodeExists = StringUtils.isNotEmpty(specCode);
        String typeCode = rsit.getCode();

        if (rsit.hasSpecifications()) {
            if (specCodeExists) {
                RulItemSpec itemSpec = rsit.getItemSpecByCode(specCode);
                if (itemSpec == null) {
                    throw new DEImportException(
                            "Description item specification not found, typeCode:" + typeCode + ", specCode:"
                                    + specCode);
                }
                return itemSpec;
            } else {
                throw new DEImportException(
                        "Description item specification missing, typeCode:" + typeCode + ", specCode:" + specCode);
            }
        } else if (specCodeExists) {
            throw new DEImportException(
                    "Specification for description item not expected, typeCode:" + typeCode + ", specCode:" + specCode);
        }
        return null;
    }

    private static class ApEntity {

        ApAccessPoint accessPoint;
        ApState state;

        ApEntity(final ApAccessPoint accessPoint, final ApState state) {
            this.accessPoint = accessPoint;
            this.state = state;
        }
    }

    private ApEntity createEntity(AccessPointEntry entry) {
        if (StringUtils.isEmpty(entry.getId())) {
            throw new DEImportException("AP entry id is empty");
        }
        // resolve AP type
        if (entry.getT() == null) {
            throw new DEImportException("AP type is not set, apeId:" + entry.getId());
        }
        ApType apType = staticData.getApTypeByCode(entry.getT());
        if (apType == null) {
            throw new DEImportException("AP has invalid type, apeId:" + entry.getId());
        }
        if (apType.isReadOnly()) {
            throw new DEImportException("AP type is read only, apeId:" + entry.getId());
        }

        // create AP
        ApAccessPoint accessPoint = new ApAccessPoint();
        accessPoint.setUuid(StringUtils.trimToNull(entry.getUuid()));
        accessPoint.setState(ApStateEnum.OK);

        StateApproval stateApproval = StateApproval.NEW;
        if (entry.getS() != null) {
            stateApproval = StateApproval.valueOf(entry.getS().toString());
        }

        ApState apState = new ApState();
        apState.setAccessPoint(accessPoint);
        apState.setApType(apType);
        apState.setScope(context.getScope());
        apState.setStateApproval(stateApproval);
        apState.setCreateChange(context.getCreateChange());

        return new ApEntity(accessPoint, apState);
    }

}
