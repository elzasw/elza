package cz.tacr.elza.service.cam;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.CreateEntityXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ErrorMessageXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.UpdatesFromXml;
import cz.tacr.cam.schema.cam.UpdatesXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.connector.CamConnector;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApChange.Type;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ExternalCode;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

    @Autowired
    private UserService userService;

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

    public void updateBindingAfterSave(BatchUpdateResultXml batchUpdateResult,
                                       ApAccessPoint accessPoint,
                                       String externalSystemCode,
                                       ExtSyncsQueueItem extSyncsQueueItem) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        ApBinding binding = bindingState.getBinding();

        if (batchUpdateResult instanceof BatchUpdateSavedXml) {
            BatchUpdateSavedXml batchUpdateSaved = (BatchUpdateSavedXml) batchUpdateResult;
            BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

            ApChange change = apDataService.createChange(ApChange.Type.AP_SYNCH);
            bindingState = externalSystemService.createNewApBindingState(bindingState, change,
                                                                         batchEntityRecordRev.getRev().getValue());
            binding.setValue(String.valueOf(batchEntityRecordRev.getEid().getValue()));

            setQueueItemState(extSyncsQueueItem, ExtSyncsQueueItem.ExtAsyncQueueState.OK, OffsetDateTime.now(), null);

            bindingRepository.save(binding);
        } else {
            BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;

            StringBuilder message = new StringBuilder();
            if (CollectionUtils.isNotEmpty(batchUpdateErrorXml.getMessages())) {
                for (ErrorMessageXml errorMessage : batchUpdateErrorXml.getMessages()) {
                    message.append(errorMessage.getMsg().getValue()).append("\n");
                }
            }

            setQueueItemState(extSyncsQueueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              message.toString());
        }
    }

    private void setQueueItemState(ExtSyncsQueueItem item, ExtAsyncQueueState state,
                                   OffsetDateTime dateTime,
                                   String message) {
        setQueueItemState(Collections.singletonList(item), state, dateTime, message);
    }

    private void setQueueItemState(List<ExtSyncsQueueItem> items, ExtAsyncQueueState state,
                                   OffsetDateTime dateTime,
                                   String message) {
        for (ExtSyncsQueueItem item : items) {
            if (state != null) {
                item.setState(state);
            }
            item.setDate(dateTime);
            item.setStateMessage(message);
        }
        extSyncsQueueItemRepository.saveAll(items);
    }

    public BatchUpdateXml createCreateEntityBatchUpdate(final ApAccessPoint accessPoint,
                                                        final ApState state,
                                                        final ApBindingState bindingState,
                                                        final String externalSystemCode,
                                                        final String userName) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(userName));
        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint,
                bindingState,
                state,
                apExternalSystem,
                this.groovyService,
                this.apDataService,
                state.getScope());
        CreateEntityXml createEntityXml = ceb.build(partList, itemMap, apExternalSystem.getType().toString());
        batchUpdate.getChanges().add(createEntityXml);
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

        List<Object> changes = ueb.build(entityXml,
                  partList, itemMap, apExternalSystem.getType().toString());
        
        batchUpdate.getChanges().addAll(changes);
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
        Map<String, ApBindingState> bindingStateMap = bindingStateList.stream()
                .collect(Collectors.toMap(p -> p.getBinding().getValue(), p -> p));

        UsrUser user = userService.getLoggedUser();
        List<ExtSyncsQueueItem> extSyncsQueueItems = new ArrayList<>();
        for (String recordCode : recordCodes) {
            ApBindingState bindingState = bindingStateMap.get(recordCode);
            // update or add new items from CAM_COMPLETE
            if (bindingState != null || externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                ExtSyncsQueueItem extSyncsQueue = new ExtSyncsQueueItem();
                if (bindingState != null) {
                    extSyncsQueue.setAccessPoint(bindingState.getAccessPoint());
                    extSyncsQueue.setState(ExtAsyncQueueState.UPDATE);
                } else {
                    ApBinding binding = externalSystemService.createApBinding(externalSystem.getScope(), recordCode, externalSystem);
                    extSyncsQueue.setBinding(binding);
                    extSyncsQueue.setState(ExtAsyncQueueState.IMPORT_NEW);
                }
                extSyncsQueue.setExternalSystem(externalSystem);
                extSyncsQueue.setDate(OffsetDateTime.now());
                extSyncsQueue.setUsername(user == null? "admin" : user.getUsername());
                extSyncsQueueItems.add(extSyncsQueue);
            }
        }
        if (!extSyncsQueueItems.isEmpty()) {
            extSyncsQueueItemRepository.saveAll(extSyncsQueueItems);
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
        log.error("Failed to send data to external system, responseCode: {}, responseBode: {}",
                  e.getCode(), e.getResponseBody(), e);
        return new SystemException("Došlo k chybě při komunikaci s externím systémem.", e)
                .set("responseBody", e.getResponseBody())
                .set("responseCode", e.getCode())
                .set("responseHeaders", e.getResponseHeaders());
    }

    /**
     * Synchronizace (vytvoření nového nebo aktualizace) přístupového bodu z externího systému
     *
     * @param procCtx context
     * @param state stav přístupového bodu
     * @param bindingState stav propojení s externím systémem
     * @param binding při vytváření nové entity
     * @param entity entita z externího systému
     * @param syncQueue zda-li se jedná o volání z fronty
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state, ApBindingState bindingState, ApBinding binding,
                                       EntityXml entity, boolean syncQueue) {
        if (binding != null) {
            log.debug("Entity creation request, bindingId: {}, revId: {}", binding.getBindingId(), entity.getRevi().getRid().getValue());
        } else {
            log.debug("Entity synchronization request, accesPointId: {}, revId: {}", state.getAccessPointId(), entity.getRevi().getRid().getValue());
        }

        if (state != null && state.getDeleteChangeId() != null) {
            // TODO: save sync request
            /*bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);*/
            log.info("Received synchronization request for deleted entity, accessPointId: {}",
                     state.getAccessPointId());
            return;
            //throw new BusinessException("Synchronized entity is deleted, accessPointId: " + state.getAccessPoint(),
            //        ExternalCode.RECORD_NOT_FOUND);
        }

        // Kontrola na zalozeni nove entity
        // overeni existence UUID
        if (bindingState == null && binding != null) {
            ApAccessPoint accessPoint = apAccessPointRepository.findApAccessPointByUuid(entity.getEuid().getValue());
            if (accessPoint != null) {
                log.error("Entity with uuid:{} already exists", entity.getEuid().getValue());
                throw new BusinessException("Entity with uuid: " + entity.getEuid().getValue() + "  already exists", ExternalCode.IMPORT_FAIL);
            }
        }

        if (state != null && syncQueue && checkLocalChanges(state, bindingState)) {
            //jedná se o noční synchronizaci a entita má lokální změny
            bindingState.setSyncOk(SyncState.NOT_SYNCED);
            bindingStateRepository.save(bindingState);
        } else {
            ApChange apChange = apDataService.createChange(binding == null? ApChange.Type.AP_UPDATE : ApChange.Type.AP_CREATE);
            procCtx.setApChange(apChange);

            EntityDBDispatcher ec = createEntityDBDispatcher();
            if (apChange.getType() == Type.AP_CREATE) {
                ec.createAccessPoint(procCtx, entity, binding);
            } else {
                ec.synchronizeAccessPoint(procCtx, state, bindingState, entity);
            }

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
            synchronizeAccessPoint(procCtx, syncReq.getState(), syncReq.getBindingState(), null, syncReq.getEntityXml(), false);
        }

        // kontrola datové struktury
        if (checkDb) {
            entityManager.flush();
            accessPointService.checkConsistency();
        }
    }

    /**
     * Synchronizace záznamů ELZA -> CAM
     * 
     * @param extSyncsQueueItem
     * @return return true if item was processed. return false if failed and will be
     *         retried later
     */
    @Transactional
    public boolean synchronizeExtItem(ExtSyncsQueueItem extSyncsQueueItem) {
        Integer externalSystemId = extSyncsQueueItem.getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);

        try {
            synchronizeExtItem(extSyncsQueueItem, externalSystem);
            return true;
        } catch (ApiException e) {
            // if ApiException -> it means we connected server and it is logical failure 
            setQueueItemState(extSyncsQueueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              e.getMessage());
            log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
            return true;
        } catch (Exception e) {
            // other exception -> retry later
            setQueueItemState(extSyncsQueueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW,
                              OffsetDateTime.now(),
                              e.getMessage());
            return false;
        }
    }

    /**
     * Synchronizace jednoho záznamu CAM -> ELZA
     * 
     * @param queueItem
     * @return return true if item was processed. return false if failed and will be
     *         retried later
     */
    @Transactional
    public boolean synchronizeIntItem(ExtSyncsQueueItem queueItem) {
        Integer externalSystemId = queueItem.getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);

        ApState state;
        ApBindingState bindingState;
        ApBinding binding;
        String bindingValue;
        ApScope scope;
        if (queueItem.getAccessPointId() != null) {
            Integer accessPointId = queueItem.getAccessPointId();
            ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(accessPointId);

            state = accessPointService.getStateInternal(accessPoint);
            bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
            bindingValue = bindingState.getBinding().getValue();
            scope = state.getScope();
            binding = null;
        } else {
            state = null;
            bindingState = null;
            binding = bindingRepository.findById(queueItem.getBindingId()).get();
            bindingValue = binding.getValue();
            scope = externalSystem.getScope();
        }

        EntityXml entity;
        try {
            // download entity from CAM
            log.debug("Download entity from CAM, bindingValue: {} externalSystem: {}", bindingValue, externalSystem.getCode());
            entity = camConnector.getEntityById(bindingValue, externalSystem.getCode());
        } catch (ApiException e) {
            // if ApiException -> it means we connected server and it is logical failure 
            setQueueItemState(queueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              e.getMessage());
            log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
            return true;
        } catch (Exception e) {
            // other exception -> retry later
            setQueueItemState(queueItem,
                              queueItem.getState(),
                              OffsetDateTime.now(),
                              e.getMessage());
            return false;
        }
        ProcessingContext procCtx = new ProcessingContext(scope, externalSystem, staticDataService);
        try {
            synchronizeAccessPoint(procCtx, state, bindingState, binding, entity, true);
        } catch (Exception e) {
            setQueueItemState(queueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              e.getMessage());
            return true;
        }
        setQueueItemState(queueItem,
                          ExtSyncsQueueItem.ExtAsyncQueueState.OK,
                          OffsetDateTime.now(),
                          "Synchronized: ES -> ELZA");
        return true;
    }

    /**
     * Synchronizace seznamu záznamů CAM -> ELZA
     * 
     * @param queueItems seznam
     * @return return true if items was processed. return false if failed and will be
     *         retried later
     */
    @Transactional
    public boolean synchronizeIntItems(List<ExtSyncsQueueItem> queueItems) {
        if (queueItems.isEmpty()) {
            return true;
        }
        Integer externalSystemId = queueItems.get(0).getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
        ApScope scope = externalSystem.getScope();

        List<Integer> bindingIds = queueItems.stream().map(p -> p.getBindingId()).collect(Collectors.toList());
        List<ApBinding> bindings = bindingRepository.findAllById(bindingIds);
        List<String> bindingValues = bindings.stream().map(p -> p.getValue()).collect(Collectors.toList());
        Map<String, ApBinding> bindingMap = bindings.stream().collect(Collectors.toMap(x -> x.getValue(), x -> x));

        EntitiesXml entities = new EntitiesXml();
        try {
            log.debug("Download entity from CAM, bindingValues: {} externalSystem: {}", bindingValues, externalSystem.getCode());
            entities = camConnector.getEntitiesByIds(bindingValues, externalSystem.getCode());
        } catch (ApiException e) {
            // if ApiException -> it means we connected server and it is logical failure 
            setQueueItemState(queueItems,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              e.getMessage());
            log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
            return true;
        } catch (Exception e) {
            // other exception -> retry later
            setQueueItemState(queueItems,
                              null, // stav nezměníme
                              OffsetDateTime.now(),
                              e.getMessage());
            return false;
        }
        ProcessingContext procCtx = new ProcessingContext(scope, externalSystem, staticDataService);
        for (EntityXml entity : entities.getList()) {
            String value = String.valueOf(entity.getEid().getValue());
            ApBinding binding = bindingMap.get(value);
            try {
                synchronizeAccessPoint(procCtx, null, null, binding, entity, true);
            } catch (Exception e) {
                setQueueItemState(queueItems,
                                  ExtSyncsQueueItem.ExtAsyncQueueState.ERROR, 
                                  OffsetDateTime.now(),
                                  e.getMessage());
                return true;
            }
        }
        setQueueItemState(queueItems,
                          ExtSyncsQueueItem.ExtAsyncQueueState.OK,
                          OffsetDateTime.now(),
                          "Synchronized: ES -> ELZA");
        return true;
    }

    /**
     * Založí nebo upraví entitu v externím systému
     *
     * @param extSyncsQueueItem
     *            info o ap pro synchronizaci
     * @param apExternalSystem
     *            externí systém
     * @throws ApiException
     */
    private void synchronizeExtItem(ExtSyncsQueueItem extSyncsQueueItem, ApExternalSystem apExternalSystem)
            throws ApiException {
        String externalSystemCode = apExternalSystem.getCode();
        String userName = extSyncsQueueItem.getUsername();

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(extSyncsQueueItem.getAccessPointId());
        ApState state = accessPointService.getStateInternal(accessPoint);

        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, apExternalSystem);
        // TODO: get current bindings
        if (bindingState == null) {
            // create binding with uuid?
            ApBinding binding = externalSystemService.createApBinding(state.getScope(),
                                                                      accessPoint.getUuid(),
                                                                      apExternalSystem);
            ApChange change = apDataService.createChange(ApChange.Type.AP_CREATE);
            bindingState = externalSystemService.createApBindingState(binding, accessPoint, change,
                                                                      EntityRecordStateXml.ERS_NEW.value(),
                                                                      null, userName, null);
        }

        BatchUpdateXml batchUpdate;
        if (bindingState == null) {
            // založení nové entity
            batchUpdate = createCreateEntityBatchUpdate(accessPoint, state, bindingState,
                                                        externalSystemCode, userName);
        } else {
            // update entity
            EntityXml entity = camConnector.getEntityById(bindingState.getBinding().getValue(), externalSystemCode);
            batchUpdate = createUpdateEntityBatchUpdate(accessPoint, bindingState, entity, apExternalSystem, userName);
        }

        BatchUpdateResultXml batchUpdateResult = camConnector.postNewBatch(batchUpdate, externalSystemCode);
        updateBindingAfterSave(batchUpdateResult, accessPoint, externalSystemCode, extSyncsQueueItem);
    }
}
