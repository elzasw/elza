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
import cz.tacr.cam.schema.cam.UpdatesFromXml;
import cz.tacr.cam.schema.cam.UpdatesXml;
import cz.tacr.cam.schema.cam.UuidXml;
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
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CamService {

    private final Logger log = LoggerFactory.getLogger(CamService.class);

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

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${elza.ap.checkDb:false}")
    private boolean checkDb;

    private final String TRANSACTION_UUID = "91812cb8-3519-4f78-b0ec-df6e951e2c7c";
    private final Integer PAGE_SIZE = 1000;

    private EntityDBDispatcher createEntityDBDispatcher() {
        return new EntityDBDispatcher(apAccessPointRepository,
                stateRepository,
                bindingRepository,
                bindingItemRepository,
                dataRecordRefRepository,
                externalSystemService,
                accessPointService,
                apItemService,
                asyncRequestService,
                partService,
                accessPointCacheService,
                this);
    }

    public List<ApState> createAccessPoints(final ProcessingContext procCtx,
                                            final List<EntityXml> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }

        ApChange apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
        procCtx.setApChange(apChange);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.createEntities(procCtx, entities);

        return ec.getApStates();
    }

    public void connectAccessPoint(final ApState state, final EntityXml entity,
                                   final ProcessingContext procCtx, final boolean replace) {
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        procCtx.setApChange(apChange);

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());

        state.setDeleteChange(apChange);
        stateRepository.save(state);
        ApState stateNew = accessPointService.copyState(state, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(ApState.StateApproval.NEW);
        stateNew = stateRepository.save(stateNew);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.connectEntity(procCtx, stateNew, entity, replace);
    }


    /**
     * Vytvoreni novych propojeni (binding) pro vztaht
     *
     * @param dataRefList
     * @param binding
     * @param procCtx
     */
    void createBindingForRel(final List<ReferencedEntities> dataRefList, final ProcessingContext procCtx) {
        for (ReferencedEntities dataRef : dataRefList) {
            createBindingForRel(dataRef.getData(), dataRef.getEntityIdentifier(), procCtx);
        }
    }

    /**
     * Vytvoreni binding pro navazany record
     * 
     * @param item
     * @param value
     * @param procCtx
     */
    private void createBindingForRel(ArrDataRecordRef dataRecordRef, String value, ProcessingContext procCtx) {
        ApBinding refBinding = externalSystemService.findByScopeAndValueAndApExternalSystem(procCtx.getScope(),
                                                                                            value,
                                                                                            procCtx.getApExternalSystem());
        if (refBinding == null) {
            // check if not in the processing context
            refBinding = procCtx.getBindingByValue(value);
            if (refBinding == null) {
                // we can create new - last resort
                refBinding = externalSystemService.createApBinding(procCtx.getScope(),
                                                                   value,
                        procCtx.getApExternalSystem());
                procCtx.addBinding(refBinding);
            }
        } else {
            // proc se dela toto?
            Optional<ApBindingState> bindingState = bindingStateRepository.findActiveByBinding(refBinding);
            bindingState.ifPresent(bs -> {
                dataRecordRef.setRecord(bs.getAccessPoint());
            });
        }
        dataRecordRef.setBinding(refBinding);
        dataRecordRefRepository.save(dataRecordRef);
    }

    public void updateBindingAfterSave(BatchUpdateResultXml batchUpdateResult, ApAccessPoint accessPoint,
                                       String externalSystemCode, ExtSyncsQueueItem extSyncsQueueItem) {
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

    public void updateBindingAfterUpdate(BatchUpdateResultXml batchUpdateResult,
                                         ApAccessPoint accessPoint, ApExternalSystem apExternalSystem,
                                         ExtSyncsQueueItem extSyncsQueueItem) {
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
            // TODO: use deleteChange in BindingItem
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
            List<ApBindingItem> bindingItemList = bindingItemRepository.findByBinding(binding);

            if (CollectionUtils.isNotEmpty(bindingItemList)) {
                List<ApBindingItem> deleteBindings = new ArrayList<>(bindingItemList);
                for (ApBindingItem bindingItem : bindingItemList) {
                    if ((bindingItem.getPart() != null && bindingItem.getPart().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) ||
                            bindingItem.getItem() != null && bindingItem.getItem().getCreateChange().getChangeId() <= bindingState.getSyncChange().getChangeId()) {
                        deleteBindings.remove(bindingItem);
                    }
                }
                if (CollectionUtils.isNotEmpty(deleteBindings)) {
                    bindingItemRepository.deleteAll(deleteBindings);
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
                change,
                this.groovyService,
                this.apDataService,
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
                this.apDataService,
                state.getScope());

        ueb.build(batchUpdate.getChanges(), entityXml,
                  partList, itemMap, bindingParts, apExternalSystem.getType().toString());
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

        // kontrola datové struktury
        if (checkDb) {
            entityManager.flush();
            accessPointService.checkConsistency();
        }
    }

    private void synchronizeAccessPointsForExternalSystem(ApExternalSystem externalSystem,
                                                          List<EntityRecordRevInfoXml> entityRecordRevInfoXmls) {
        if (CollectionUtils.isEmpty(entityRecordRevInfoXmls)) {
            return;
        }
        List<String> recordCodes = getRecordCodes(entityRecordRevInfoXmls);
        List<ApBindingState> bindingStateList = externalSystemService.findByRecordCodesAndExternalSystem(recordCodes,
                                                                                                         externalSystem);

        if (CollectionUtils.isEmpty(bindingStateList)) {
            // TODO: ? is it error
            return;
        }
        for (ApBindingState bindingState : bindingStateList) {
            ApState state = accessPointService.getStateInternal(bindingState.getAccessPoint());
            EntityXml entity;
            try {
                // download entity from CAM
                entity = camConnector.getEntityById(Integer.parseInt(bindingState.getBinding().getValue()),
                                                    externalSystem.getCode());
            } catch (ApiException e) {
                throw prepareSystemException(e);
            }
            ProcessingContext procCtx = new ProcessingContext(state.getScope(), externalSystem,
                    staticDataService);
            synchronizeAccessPoint(procCtx, state, entity, bindingState, true);
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
        log.debug("Entity synchronization request, accesPointId: {}, revId: {}", state.getAccessPointId(),
                  entity.getRevi().getRid().getValue());
        if(state.getDeleteChangeId()!=null) {
            // TODO: save sync request
            /*bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);*/
            log.info("Received synchronization request for deleted entity, accessPointId: {}",
                     state.getAccessPointId());
            return;
            //throw new BusinessException("Synchronized entity is deleted, accessPointId: " + state.getAccessPoint(),
            //        ExternalCode.RECORD_NOT_FOUND);
        }

        if (syncQueue && checkLocalChanges(state, bindingState)) {
            //jedná se o noční synchronizaci a entita má lokální změny
            bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);
        } else {
            ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
            procCtx.setApChange(apChange);

            EntityDBDispatcher ec = createEntityDBDispatcher();
            ec.synchronizeAccessPoint(procCtx, state, bindingState, entity);

            procCtx.setApChange(null);
        }
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

    // PPy: Toto vyzaduje revizi
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

        // kontrola datové struktury
        if (checkDb) {
            entityManager.flush();
            accessPointService.checkConsistency();
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
