package cz.tacr.elza.service.cam;

import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CamService {

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;

    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private PartService partService;

    @Autowired
    private UserService userService;

    @Autowired
    private AccessPointItemService apItemService;


    public List<ApState> createAccessPoints(final ProcessingContext procCtx,
                                            final List<EntityXml> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        ApExternalSystem apExternalSystem = procCtx.getApExternalSystem();

        List<ApState> states = new ArrayList<>();

        Function<EntityXml, String> idGetter;

        // prepare list of already used ids
        Map<String, EntityXml> uuids = CamHelper.getEntitiesByUuid(entities);
        List<ApAccessPoint> existingAps = apAccessPointRepository.findApAccessPointsByUuids(uuids.keySet());
        Set<String> usedUuids = existingAps.stream().map(ApAccessPoint::getUuid).collect(Collectors.toSet());

        // Read existing binding from DB
        List<String> values;
        if (apExternalSystem.getType() == ApExternalSystemType.CAM) {
            values = CamHelper.getEids(entities);
            idGetter = CamHelper::getEntityId;
        } else if (apExternalSystem.getType() == ApExternalSystemType.CAM_UUID) {
            values = CamHelper.getEuids(entities);
            idGetter = CamHelper::getEntityUuid;
        } else {
            throw new IllegalStateException("Unkonw external system type: " + apExternalSystem.getType());
        }
        List<ApBinding> bindingList = bindingRepository.findByScopeAndValuesAndExternalSystem(procCtx.getScope(),
                values,
                apExternalSystem);
        procCtx.addBindings(bindingList);

        // get list of connected records
        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByBindingIn(bindingList);

        for (EntityXml entity : entities) {
            String bindingValue = idGetter.apply(entity);

            ApBinding binding = procCtx.getBindingByValue(bindingValue);
            if (binding == null) {
                binding = externalSystemService.createApBinding(procCtx.getScope(), bindingValue, apExternalSystem);
                procCtx.addBinding(binding);
            }

            // prepare uuid
            String srcUuid = CamHelper.getEntityUuid(entity);
            String apUuid = usedUuids.contains(srcUuid) ? null : srcUuid;

            ApState state = createAccessPoint(procCtx, entity, binding, apUuid);
            states.add(state);
            accessPointService.setAccessPointInDataRecordRefs(state.getAccessPoint(), dataRecordRefList, binding);
        }
        dataRecordRefRepository.saveAll(dataRecordRefList);
        //TODO fantiš smazat po vyzkoušení, že je OK
//        List<ApPart> partList = itemRepository.findPartsByDataRecordRefList(dataRecordRefList);
//        if (CollectionUtils.isNotEmpty(partList)) {
//            for (ApPart part : partList) {
//                updatePartValue(part);
//            }
//        }

        return states;
    }

    public ApState createAccessPoint(final ProcessingContext procCtx,
                                     final EntityXml entity,
                                     ApBinding binding,
                                     String uuid) {
        Validate.notNull(procCtx, "Context cannot be null");

        StaticDataProvider sdp = staticDataService.getData();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        ApState apState = accessPointService.createAccessPoint(procCtx.getScope(), type, apChange, uuid);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, sdp, apState, binding);

        accessPointService.publishAccessPointCreateEvent(accessPoint);

        return apState;
    }

    public void connectAccessPoint(final ApState state, final EntityXml entity,
                                   final ProcessingContext procCtx, final boolean replace) {
        StaticDataProvider sdp = staticDataService.getData();
        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());

        state.setDeleteChange(apChange);
        stateRepository.save(state);
        ApState stateNew = accessPointService.copyState(state, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(ApState.StateApproval.NEW);
        stateRepository.save(stateNew);

        if (replace) {
            partService.deleteParts(accessPoint, apChange);
        }


        ApBinding binding = externalSystemService.createApBinding(procCtx.getScope(),
                Long.toString(entity.getEid().getValue()),
                procCtx.getApExternalSystem());

        createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, sdp, stateNew, binding);

        accessPointService.publishAccessPointUpdateEvent(accessPoint);
    }

    private void createPartsFromEntityXml(final ProcessingContext procCtx,
                                          final EntityXml entity,
                                          final ApAccessPoint accessPoint,
                                          final ApChange apChange,
                                          final StaticDataProvider sdp,
                                          final ApState apState,
                                          final ApBinding binding) {
        Validate.notNull(binding);

        externalSystemService.createApBindingState(binding, accessPoint, apChange,
                entity.getEns().value(), entity.getRevi().getRid().getValue(),
                entity.getRevi().getUsr().getValue(),
                entity.getReid() != null ? entity.getReid().getValue() : null);
        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

        List<DataRef> dataRefList = new ArrayList<>();

        for (PartXml part : entity.getPrts().getList()) {
            RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
            ApPart parentPart = part.getPrnt() != null ? accessPointService.findParentPart(binding, part.getPrnt().getValue()) : null;

            ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
            externalSystemService.createApBindingItem(binding, part.getPid().getValue(), apPart, null);

            List<ApItem> itemList = partService.createPartItems(apChange, apPart, part.getItms().getItems(), binding, dataRefList);

            itemMap.put(apPart.getPartId(), itemList);
            partList.add(apPart);
        }

        createBindingForRel(dataRefList, binding, procCtx);

        accessPoint.setPreferredPart(accessPointService.findPreferredPart(partList));

        accessPointService.generateSync(accessPoint.getAccessPointId(), apState, partList, itemMap);
    }

    /**
     * Vytvoreni novych propojeni (binding)
     *
     * @param dataRefList
     * @param binding
     * @param procCtx
     */
    private void createBindingForRel(final List<DataRef> dataRefList, final ApBinding binding,
                                     final ProcessingContext procCtx) {
        //TODO fantiš optimalizovat
        for (DataRef dataRef : dataRefList) {
            // Tento kod je divny, co to dela?
            ApBindingItem apBindingItem = externalSystemService.findByBindingAndUuid(binding, dataRef.getUuid());
            // Co kdyz vazba neni nalezena?
            if (apBindingItem.getItem() != null) {
                ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) apBindingItem.getItem().getData();
                ApBinding refBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(procCtx.getScope(),
                        dataRef.getValue(),
                        procCtx.getApExternalSystem());
                if (refBinding == null) {
                    // check if not in the processing context
                    refBinding = procCtx.getBindingByValue(dataRef.getValue());
                    if (refBinding == null) {
                        // we can create new - last resort
                        refBinding = externalSystemService.createApBinding(procCtx.getScope(),
                                dataRef.getValue(),
                                procCtx.getApExternalSystem());
                        procCtx.addBinding(refBinding);
                    }
                } else {
                    // proc se dela toto?
                    ApBindingState bindingState = externalSystemService.findByBinding(refBinding);
                    if (bindingState != null) {
                        dataRecordRef.setRecord(bindingState.getAccessPoint());
                    }
                }
                dataRecordRef.setBinding(refBinding);
                dataRecordRefRepository.save(dataRecordRef);
            }
        }
    }

    public void updateBindingAfterSave(BatchUpdateResultXml batchUpdateResult, Integer accessPointId, String externalSystemCode) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            bindingState.setExtRevision(batchEntityRecordRev.getRev().getValue());
            binding.setValue(String.valueOf(batchEntityRecordRev.getEid().getValue()));

            bindingStateRepository.save(bindingState);
            bindingRepository.save(binding);
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            bindingItemRepository.deleteByBinding(binding);
            bindingStateRepository.delete(bindingState);
            bindingRepository.delete(binding);

            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                StringBuilder message = new StringBuilder();
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
                throw new SystemException(message.toString(), ExternalCode.EXTERNAL_SYSTEM_ERROR);
            }
        }
    }

    public void updateBindingAfterUpdate(BatchUpdateResultXml batchUpdateResult, ApAccessPoint accessPoint, ApExternalSystem apExternalSystem) {
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
            externalSystemService.createNewApBindingState(bindingState, change, batchEntityRecordRev.getRev().getValue());
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            List<ApBindingItem> bindingItemList = bindingItemRepository.findByBinding(binding);

            if (CollectionUtils.isNotEmpty(bindingItemList)) {
                for (ApBindingItem bindingItem : bindingItemList) {
                    if ((bindingItem.getPart() != null && bindingItem.getPart().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) ||
                            bindingItem.getItem() != null && bindingItem.getItem().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) {
                        bindingItemList.remove(bindingItem);
                    }
                }
                if (CollectionUtils.isNotEmpty(bindingItemList)) {
                    bindingItemRepository.deleteAll(bindingItemList);
                }
            }

            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                StringBuilder message = new StringBuilder();
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
                throw new SystemException(message.toString(), ExternalCode.EXTERNAL_SYSTEM_ERROR);
            }
        }
    }

    public BatchUpdateXml createCreateEntityBatchUpdate(final Integer accessPointId, final String externalSystemCode) {
        ApAccessPoint accessPoint = accessPointService.getAccessPoint(accessPointId);
        ApState state = accessPointService.getState(accessPoint);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
        UserDetail userDetail = userService.getLoggedUserDetail();

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        ApBinding binding = externalSystemService.createApBinding(state.getScope(), null, apExternalSystem);
        ApBindingState bindingState = externalSystemService.createApBindingState(binding, accessPoint, change,
                EntityRecordStateXml.ERS_NEW.value(), null, userDetail.getUsername(), null);

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userDetail));
        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint, binding, state);
        batchUpdate.getChanges().add(ceb.build(partList, itemMap));
        return batchUpdate;
    }

    public BatchUpdateXml createUpdateEntityBatchUpdate(final ApAccessPoint accessPoint,
                                                        final ApBindingState bindingState,
                                                        final EntityXml entityXml) {
        ApState state = accessPointService.getState(accessPoint);
        UserDetail userDetail = userService.getLoggedUserDetail();

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));
        List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(bindingState.getBinding());

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userDetail));

        UpdateEntityBuilder ueb = new UpdateEntityBuilder(this.externalSystemService,
                this.bindingItemRepository,
                this.staticDataService.getData(),
                state,
                bindingState);

        ueb.build(batchUpdate.getChanges(), entityXml, partList, itemMap, bindingParts);
        return batchUpdate;
    }

    private BatchInfoXml createBatchInfo(UserDetail userDetail) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(new LongStringXml(userDetail.getUsername()));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state, EntityXml entity,
                                       ApBindingState bindingState, boolean syncQueue) {
        //TODO fantiš other external systems
        if (syncQueue && checkLocalChanges(state, bindingState)) {
            bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            ApBinding binding = bindingState.getBinding();
            ApAccessPoint accessPoint = state.getAccessPoint();
            ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);

            List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(binding);
            List<ApBindingItem> newBindingParts = new ArrayList<>();
            Map<Integer, List<ApBindingItem>> bindingItemMap = bindingItemRepository.findItemsByBinding(binding).stream()
                    .collect(Collectors.groupingBy(i -> i.getItem().getPartId()));

            externalSystemService.createNewApBindingState(bindingState, apChange, entity.getEns().value(),
                    entity.getRevi().getRid().getValue(),  entity.getRevi().getUsr().getValue(),
                    entity.getReid() != null ? entity.getReid().getValue() : null);

            List<ApPart> partList = new ArrayList<>();
            Map<Integer, List<ApItem>> itemMap = new HashMap<>();

            List<DataRef> dataRefList = new ArrayList<>();

            for (PartXml part : entity.getPrts().getList()) {
                ApBindingItem bindingItem = accessPointService.findBindingItemByUuid(bindingParts, part.getPid().getValue());
                if (bindingItem != null) {
                    List<ApBindingItem> bindingItems = bindingItemMap.getOrDefault(bindingItem.getPart().getPartId(), new ArrayList<>());
                    List<ApBindingItem> notChangeItems = new ArrayList<>();
                    List<Object> newItems = apItemService.findNewOrChangedItems(part.getItms().getItems(), bindingItems, notChangeItems);

                    if (CollectionUtils.isNotEmpty(newItems) || CollectionUtils.isNotEmpty(bindingItems)) {
                        //nové nebo změněné itemy z externího systému
                        deleteChangedOrRemovedItems(bindingItems, apChange);

                        ApPart oldPart = bindingItem.getPart();
                        partService.deletePart(oldPart, apChange);
                        ApPart apPart = partService.createPart(oldPart, apChange);
                        bindingItem.setPart(apPart);
                        bindingItemRepository.save(bindingItem);

                        changePartInItems(apPart, notChangeItems, apChange);
                        changePartInItems(apPart, apChange, oldPart);

                        partService.createPartItems(apChange, apPart, newItems, binding, dataRefList);

                        itemMap.put(apPart.getPartId(), itemRepository.findValidItemsByPart(apPart));
                        partList.add(apPart);
                    }
                    newBindingParts.add(bindingItem);
                    bindingParts.remove(bindingItem);
                } else {
                    //nový part v externím systému
                    RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
                    ApPart parentPart = part.getPrnt() != null ? accessPointService.findBindingItemByUuid(newBindingParts, part.getPrnt().getValue()).getPart() : null;

                    ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
                    newBindingParts.add(externalSystemService.createApBindingItem(binding, part.getPid().getValue(), apPart, null));
                    List<ApItem> itemList = partService.createPartItems(apChange, apPart, part.getItms().getItems(), binding, dataRefList);

                    itemMap.put(apPart.getPartId(), itemList);
                    partList.add(apPart);
                }
            }
            deleteParts(bindingParts, apChange);

            createBindingForRel(dataRefList, binding, procCtx);

            accessPoint.setPreferredPart(findPreferredPart(entity.getPrts().getList(), newBindingParts));

            accessPointService.generateSync(accessPoint.getAccessPointId(), state, partList, itemMap);
        }
    }

    private void deleteParts(List<ApBindingItem> bindingParts, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(bindingParts)) {
            List<ApPart> partList = new ArrayList<>();
            for (ApBindingItem bindingItem : bindingParts) {
                partList.add(bindingItem.getPart());
            }
            apItemService.deletePartsItems(partList, apChange);
            partService.deleteParts(partList, apChange);

            bindingItemRepository.deleteAll(bindingParts);
        }
    }

    private ApPart findPreferredPart(List<PartXml> partList, List<ApBindingItem> bindingParts) {
        StaticDataProvider sdp = StaticDataProvider.getInstance();
        RulPartType defaultPartType = sdp.getDefaultPartType();
        for (PartXml part : partList) {
            if (part.getT().value().equals(defaultPartType.getCode())) {
                ApBindingItem bindingPart = accessPointService.findBindingItemByUuid(bindingParts, part.getPid().getValue());
                if (bindingPart != null) {
                    return bindingPart.getPart();
                }
            }
        }
        return null;
    }

    private void changePartInItems(ApPart apPart, List<ApBindingItem> notChangeItems, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(notChangeItems)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApBindingItem bindingItem : notChangeItems) {
                ApItem item = bindingItem.getItem();
                item.setDeleteChange(apChange);
                itemList.add(item);

                ApItem newItem = apItemService.createItem(item, apChange, apPart);
                itemList.add(newItem);

                bindingItem.setItem(newItem);
            }
            bindingItemRepository.saveAll(notChangeItems);
            itemRepository.saveAll(itemList);
        }
    }

    private void changePartInItems(ApPart apPart, ApChange apChange, ApPart oldPart) {
        List<ApItem> notConnectedItems = itemRepository.findValidItemsByPart(oldPart);
        if (CollectionUtils.isNotEmpty(notConnectedItems)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApItem item : notConnectedItems) {
                item.setDeleteChange(apChange);
                itemList.add(item);

                ApItem newItem = apItemService.createItem(item, apChange, apPart);
                itemList.add(newItem);
            }
            itemRepository.saveAll(itemList);
        }
    }

    private void deleteChangedOrRemovedItems(List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isNotEmpty(bindingItemsInPart)) {
            List<ApItem> itemList = new ArrayList<>();
            for (ApBindingItem bindingItem : bindingItemsInPart) {
                ApItem item = bindingItem.getItem();
                item.setDeleteChange(apChange);
                itemList.add(item);
            }
            bindingItemRepository.deleteAll(bindingItemsInPart);
            itemRepository.saveAll(itemList);
        }
    }

    private boolean checkLocalChanges(final ApState state, final ApBindingState bindingState) {
        //TODO fantiš možná není nutné koukat na party
        List<ApPart> partList = partService.findNewerPartsByAccessPoint(state.getAccessPoint(), bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(partList)) {
            return CollectionUtils.isNotEmpty(bindingItemRepository.findByParts(partList));
        }
        List<ApItem> itemList = itemRepository.findNewerValidItemsByAccessPoint(state.getAccessPoint(), bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(itemList)) {
            return CollectionUtils.isNotEmpty(bindingItemRepository.findByItems(itemList));
        }

        return false;
    }

    public void updateAccessPoints(final ProcessingContext procCtx,
                                   final List<SyncEntityRequest> syncRequests) {
        if (syncRequests == null || syncRequests.isEmpty()) {
            return;
        }

        for (SyncEntityRequest syncReq : syncRequests) {
            synchronizeAccessPoint(procCtx, syncReq.getState(),
                    syncReq.getEntityXml(), syncReq.getBindingState(), false);
        }
    }

}
