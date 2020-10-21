package cz.tacr.elza.service.cam;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.UpdatesFromXml;
import cz.tacr.cam.schema.cam.UpdatesXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApBindingSyncRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.vo.DataRef;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
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
    private ApAccessPointRepository accessPointRepository;

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

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private GroovyService groovyService;

    @Autowired
    private CamConnector camConnector;

    @Autowired
    private ApBindingSyncRepository bindingSyncRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    private final String TRANSACTION_UUID = "91812cb8-3519-4f78-b0ec-df6e951e2c7c";
    private final Integer PAGE_SIZE = 1000;


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
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            List<Integer> accessPointIds = ObjectListIterator.findIterable(dataRecordRefList, accessPointRepository::findAccessPointIdsByRefData);
            if (CollectionUtils.isNotEmpty(accessPointIds)) {
                ObjectListIterator.forEachPage(accessPointIds, accessPointRepository::updateToInit);
                asyncRequestService.enqueue(accessPointIds);
            }
        }

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

    public void updateBindingAfterSave(BatchUpdateResultXml batchUpdateResult, ApAccessPoint accessPoint, String externalSystemCode, ExtSyncsQueueItem extSyncsQueueItem) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            bindingState.setExtRevision(batchEntityRecordRev.getRev().getValue());
            binding.setValue(String.valueOf(batchEntityRecordRev.getEid().getValue()));

            extSyncsQueueItem.setState(ExtSyncsQueueItem.ExtAsyncQueueState.OK);
            extSyncsQueueItem.setDate(OffsetDateTime.now());
            extSyncsQueueItemRepository.save(extSyncsQueueItem);

            bindingStateRepository.save(bindingState);
            bindingRepository.save(binding);
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            bindingItemRepository.deleteByBinding(binding);
            bindingStateRepository.delete(bindingState);
            bindingRepository.delete(binding);

            StringBuilder message = new StringBuilder();
            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
            }

            extSyncsQueueItem.setState(ExtSyncsQueueItem.ExtAsyncQueueState.ERROR);
            extSyncsQueueItem.setDate(OffsetDateTime.now());
            extSyncsQueueItem.setStateMessage(message.toString());
            extSyncsQueueItemRepository.save(extSyncsQueueItem);
        }
    }

    public void updateBindingAfterUpdate(BatchUpdateResultXml batchUpdateResult, ApAccessPoint accessPoint, ApExternalSystem apExternalSystem, ExtSyncsQueueItem extSyncsQueueItem) {
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            ApChange change = apDataService.createChange(ApChange.Type.AP_UPDATE);
            externalSystemService.createNewApBindingState(bindingState, change, batchEntityRecordRev.getRev().getValue());

            extSyncsQueueItem.setState(ExtSyncsQueueItem.ExtAsyncQueueState.OK);
            extSyncsQueueItem.setDate(OffsetDateTime.now());
            extSyncsQueueItemRepository.save(extSyncsQueueItem);
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

            StringBuilder message = new StringBuilder();
            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
            }
            extSyncsQueueItem.setState(ExtSyncsQueueItem.ExtAsyncQueueState.ERROR);
            extSyncsQueueItem.setDate(OffsetDateTime.now());
            extSyncsQueueItem.setStateMessage(message.toString());
            extSyncsQueueItemRepository.save(extSyncsQueueItem);
        }
    }

    public BatchUpdateXml createCreateEntityBatchUpdate(final ApAccessPoint accessPoint, final String externalSystemCode, final String userName) {
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        ApBinding binding = externalSystemService.createApBinding(state.getScope(), null, apExternalSystem);
        ApBindingState bindingState = externalSystemService.createApBindingState(binding, accessPoint, change,
                EntityRecordStateXml.ERS_NEW.value(), null, userName, null);

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userName));
        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint,
                binding,
                state,
                this.groovyService,
                state.getScope());
        batchUpdate.getChanges().add(ceb.build(partList, itemMap, apExternalSystem.getType().toString()));
        return batchUpdate;
    }

    public BatchUpdateXml createUpdateEntityBatchUpdate(final ApAccessPoint accessPoint,
                                                        final ApBindingState bindingState,
                                                        final EntityXml entityXml,
                                                        final ApExternalSystem apExternalSystem,
                                                        final String userName) {
        ApState state = accessPointService.getStateInternal(accessPoint);

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));
        List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(bindingState.getBinding());

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userName));

        UpdateEntityBuilder ueb = new UpdateEntityBuilder(this.externalSystemService,
                this.bindingItemRepository,
                this.staticDataService.getData(),
                state,
                bindingState,
                this.groovyService,
                state.getScope());

        ueb.build(batchUpdate.getChanges(), entityXml, partList, itemMap, bindingParts, apExternalSystem.getType().toString());
        return batchUpdate;
    }

    private BatchInfoXml createBatchInfo(String userName) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(new LongStringXml(userName));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    @Transactional
    public void synchronizeAccessPointsForExternalSystem(final ApExternalSystem externalSystem) {
        ApBindingSync apBindingSync = bindingSyncRepository.findByApExternalSystem(externalSystem);
        if (apBindingSync == null) {
            apBindingSync = createApBindingSync(externalSystem);
        }

        List<EntityRecordRevInfoXml> entityRecordRevInfoXmls;
        String lastTransaction;

        try {
            UpdatesFromXml updatesFromXml = camConnector.getUpdatesFrom(apBindingSync.getLastTransaction(), externalSystem.getCode());
            if (updatesFromXml.getUps() != null && CollectionUtils.isNotEmpty(updatesFromXml.getUps().getRevisions())) {
                entityRecordRevInfoXmls = updatesFromXml.getUps().getRevisions();
                lastTransaction = updatesFromXml.getInf().getTo().getValue();
            } else {
                entityRecordRevInfoXmls = new ArrayList<>();
                lastTransaction = updatesFromXml.getInf().getTo().getValue();
                int count = updatesFromXml.getInf().getCnt().getValue().intValue();
                int page = 1;

                while (count > 0) {
                    UpdatesXml updatesXml = camConnector.getUpdatesFromTo(apBindingSync.getLastTransaction(), lastTransaction, page, PAGE_SIZE, externalSystem.getCode());
                    entityRecordRevInfoXmls.addAll(updatesXml.getRevisions());

                    page++;
                    count = count - PAGE_SIZE;
                }
            }
        } catch (ApiException e) {
            throw prepareSystemException(e);
        }

        synchronizeAccessPointsForExternalSystem(externalSystem, entityRecordRevInfoXmls);
        apBindingSync.setLastTransaction(lastTransaction);
        bindingSyncRepository.save(apBindingSync);
    }

    private void synchronizeAccessPointsForExternalSystem(ApExternalSystem externalSystem, List<EntityRecordRevInfoXml> entityRecordRevInfoXmls) {
        List<String> recordCodes = getRecordCodes(entityRecordRevInfoXmls);
        if (CollectionUtils.isNotEmpty(recordCodes)) {
            List<ApBindingState> bindingStateList = externalSystemService.findByRecordCodesAndExternalSystem(recordCodes, externalSystem);

            if (CollectionUtils.isNotEmpty(bindingStateList)) {
                for (ApBindingState bindingState : bindingStateList) {
                    ApState state = accessPointService.getStateInternal(bindingState.getAccessPoint());
                    EntityXml entity;
                    try {
                        entity = camConnector.getEntityById(Integer.parseInt(bindingState.getBinding().getValue()), externalSystem.getCode());
                    } catch (ApiException e) {
                        throw prepareSystemException(e);
                    }
                    ProcessingContext procCtx = new ProcessingContext(state.getScope(), externalSystem);
                    synchronizeAccessPoint(procCtx, state, entity, bindingState, true);
                }
            }
        }
    }

    private List<String> getRecordCodes(List<EntityRecordRevInfoXml> entityRecordRevInfoXmls) {
        List<String> recordCodes = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(entityRecordRevInfoXmls)) {
            for (EntityRecordRevInfoXml entityRecordRevInfoXml : entityRecordRevInfoXmls) {
                recordCodes.add(Long.toString(entityRecordRevInfoXml.getEid().getValue()));
            }
        }
        return recordCodes;
    }

    private ApBindingSync createApBindingSync(final ApExternalSystem externalSystem) {
        ApBindingSync apBindingSync = new ApBindingSync();
        apBindingSync.setApExternalSystem(externalSystem);
        apBindingSync.setLastTransaction(TRANSACTION_UUID);
        return bindingSyncRepository.save(apBindingSync);
    }

    private AbstractException prepareSystemException(ApiException e) {
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }

    /**
     * Synchronizace přístupového bodu z externího systému
     *
     * @param procCtx context
     * @param state stav přístupového bodu
     * @param entity entita z externího systému
     * @param bindingState stav propojení s externím systémem
     * @param syncQueue zda-li se jedná o volání z časovače
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state, EntityXml entity,
                                       ApBindingState bindingState, boolean syncQueue) {
        if (syncQueue && checkLocalChanges(state, bindingState)) {
            //jedná se o noční synchronizaci a entita má lokální změny
            bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);
        } else {
            StaticDataProvider sdp = staticDataService.getData();
            ApAccessPoint accessPoint = state.getAccessPoint();
            ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);

            ApType apType = sdp.getApTypeByCode(entity.getEnt().getValue());
            if (!state.getApTypeId().equals(apType.getApTypeId())) {
                //změna třídy entity
                state.setDeleteChange(apChange);
                stateRepository.save(state);
                ApState stateNew = accessPointService.copyState(state, apChange);
                stateNew.setApType(apType);
                state = stateRepository.save(stateNew);
            }

            //vytvoření nového stavu propojení
            externalSystemService.createNewApBindingState(bindingState, apChange, entity.getEns().value(),
                    entity.getRevi().getRid().getValue(),  entity.getRevi().getUsr().getValue(),
                    entity.getReid() != null ? entity.getReid().getValue() : null);

            List<ApPart> partList = new ArrayList<>();
            Map<Integer, List<ApItem>> itemMap = new HashMap<>();

            synchronizeParts(procCtx, entity, bindingState, apChange, accessPoint, partList, itemMap);

            accessPointService.generateSync(accessPoint.getAccessPointId(), state, partList, itemMap);
        }
    }

    /**
     * Synchronizace částí přístupového bodu z externího systému
     *
     * @param procCtx context
     * @param entity entita z externího systému
     * @param bindingState stav propojení s externím systémem
     * @param apChange změna
     * @param accessPoint přístupový bod
     * @param partList přidané nebo změněné části
     * @param itemMap prvky popisu přidaných nebo změněných částí
     */
    private void synchronizeParts(final ProcessingContext procCtx,
                                  final EntityXml entity,
                                  final ApBindingState bindingState,
                                  final ApChange apChange,
                                  final ApAccessPoint accessPoint,
                                  List<ApPart> partList,
                                  Map<Integer, List<ApItem>> itemMap) {
        StaticDataProvider sdp = staticDataService.getData();
        ApBinding binding = bindingState.getBinding();
        List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(binding);
        List<ApBindingItem> newBindingParts = new ArrayList<>();
        Map<Integer, List<ApBindingItem>> bindingItemMap = bindingItemRepository.findItemsByBinding(binding).stream()
                .collect(Collectors.groupingBy(i -> i.getItem().getPartId()));

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
                    ApPart apPart = partService.createPart(oldPart, apChange);
                    partService.deletePart(oldPart, apChange);
                    accessPointService.changeIndicesToNewPart(oldPart, apPart);
                    bindingItem.setPart(apPart);
                    bindingItemRepository.save(bindingItem);
                    accessPointService.changeBindingItemParts(oldPart, apPart);

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

        //smazání partů dle externího systému
        deleteParts(bindingParts, apChange);

        //nastavení odkazů na entitu
        createBindingForRel(dataRefList, binding, procCtx);

        //změna preferováného jména
        accessPoint.setPreferredPart(findPreferredPart(entity.getPrts().getList(), newBindingParts));
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

    /**
     * Založí nebo upraví entitu v externím systému
     *
     * @param extSyncsQueueItem info o ap pro synchronizaci
     * @param apExternalSystem externí systém
     */
    @Transactional
    public void synchronizeExtItem(ExtSyncsQueueItem extSyncsQueueItem, ApExternalSystem apExternalSystem) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(extSyncsQueueItem.getAccessPointId());
        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        String externalSystemCode = apExternalSystem.getCode();
        String userName = extSyncsQueueItem.getUsername();

        if (bindingState == null) {
            // založení nové entity
            BatchUpdateXml batchUpdate = createCreateEntityBatchUpdate(accessPoint, externalSystemCode, userName);
            try {
                BatchUpdateResultXml batchUpdateResult = camConnector.postNewBatch(batchUpdate, externalSystemCode);
                updateBindingAfterSave(batchUpdateResult, accessPoint, externalSystemCode, extSyncsQueueItem);
            } catch (ApiException e) {
                throw prepareSystemException(e);
            }
        } else {
            // update entity
            EntityXml entity;
            try {
                entity = camConnector.getEntityById(Integer.parseInt(bindingState.getBinding().getValue()), externalSystemCode);
            } catch (ApiException e) {
                throw prepareSystemException(e);
            }
            BatchUpdateXml batchUpdate = createUpdateEntityBatchUpdate(accessPoint, bindingState, entity, apExternalSystem, userName);
            try {
                BatchUpdateResultXml batchUpdateResult = camConnector.postNewBatch(batchUpdate, externalSystemCode);
                updateBindingAfterUpdate(batchUpdateResult, accessPoint, apExternalSystem, extSyncsQueueItem);
            } catch (ApiException e) {
                throw prepareSystemException(e);
            }
        }
    }
}
