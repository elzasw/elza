package cz.tacr.elza.service.cam;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.cam.schema.cam.EntityXml;
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
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
        ec.connectEntity(procCtx, stateNew, entity, replace, false);
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
        ApBinding refBinding = externalSystemService.findByValueAndExternalSystem(value,
                                                                                 procCtx.getApExternalSystem());
                
        ApAccessPoint referencedAp = null;
        if (refBinding == null) {
        	// check if item should be lookup also by UUID
        	if(ApExternalSystemType.CAM_UUID.equals(procCtx.getApExternalSystem().getType())) {
        		referencedAp = this.apAccessPointRepository.findApAccessPointByUuid(value);
        	} else {
                // check if not in the processing context
                refBinding = procCtx.getBindingByValue(value);
        	}
           	if (referencedAp == null && refBinding == null) {
           		// we can create new - last resort
           		refBinding = externalSystemService.createApBinding(value, procCtx.getApExternalSystem());
           		procCtx.addBinding(refBinding);        		        	
           	}
        } else {
            // try to find access point for binding
            Optional<ApBindingState> bindingState = bindingStateRepository.findActiveByBinding(refBinding);
            if(bindingState.isPresent()) {
            	referencedAp = bindingState.get().getAccessPoint();
            }
        }
        Validate.isTrue(referencedAp!=null||refBinding!=null, "Failed to prepare referenced record.");
        
        dataRecordRef.setRecord(referencedAp);
        dataRecordRef.setBinding(refBinding);
        dataRecordRefRepository.save(dataRecordRef);
    }

    @Transactional
    public void updateBinding(ExtSyncsQueueItem extSyncsQueueItem,
                              BatchUpdateSavedXml batchUpdateSaved, 
                              Map<Integer, String> itemUuidMap,
                              Map<Integer, String> partUuidMap) {
        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(extSyncsQueueItem.getAccessPointId());
        ApExternalSystem apExternalSystem = externalSystemService.getExternalSystemInternal(extSyncsQueueItem
                .getExternalSystemId());
        
        BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

        ApChange change = apDataService.createChange(ApChange.Type.AP_SYNCH);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint,
                                                                                                apExternalSystem);
        ApBinding binding;
        if(bindingState!=null) {
            binding = bindingState.getBinding();        
            bindingState = externalSystemService.createNewApBindingState(bindingState, change,
                                                                         batchEntityRecordRev.getRev().getValue());
        } else {
            ApState state = accessPointService.getStateInternal(accessPoint);
            binding = externalSystemService.createApBinding(Long.toString(batchEntityRecordRev.getEid().getValue()), apExternalSystem);
            bindingState = externalSystemService.createApBindingState(binding, accessPoint, change,
                                                                      state.getStateApproval().toString(),
                                                                      batchEntityRecordRev.getRev().getValue(),
                                                                      extSyncsQueueItem.getUsername(), null, SyncState.SYNC_OK);
        }
        
        // Create bindings        
        itemUuidMap.forEach((itemId, value) -> {
            ApItem item = entityManager.getReference(ApItem.class, itemId);
            this.externalSystemService.createApBindingItem(binding, change, value, null, item);
        });
        partUuidMap.forEach((partId, value) -> {
            ApPart part = entityManager.getReference(ApPart.class, partId);
            this.externalSystemService.createApBindingItem(binding, change, value, part, null);
        });

        setQueueItemState(extSyncsQueueItem, ExtSyncsQueueItem.ExtAsyncQueueState.OK, OffsetDateTime.now(), null);

        accessPointCacheService.createApCachedAccessPoint(extSyncsQueueItem.getAccessPointId());
    }

    private void setQueueItemState(ExtSyncsQueueItem item, ExtAsyncQueueState state,
                                   OffsetDateTime dateTime,
                                   String message) {
        setQueueItemState(Collections.singletonList(item), state, dateTime, message);
    }

    public void setQueueItemState(List<ExtSyncsQueueItem> items, ExtAsyncQueueState state,
                                   OffsetDateTime dateTime,
                                   String message) {
    	// check message length
    	if(StringUtils.isNotEmpty(message)) {
    		if(message.length()>StringLength.LENGTH_4000) {
    			log.error("Received very long error message, original message: {}", message);
    			message = message.substring(0, StringLength.LENGTH_4000-1);
    		}
    	}
    	for (ExtSyncsQueueItem item : items) {
            if (state != null) {
                item.setState(state);
            }
            item.setDate(dateTime);
            item.setStateMessage(message);
        }
        extSyncsQueueItemRepository.saveAll(items);
    }

    @Transactional
    public void setQueueItemStateTA(List<ExtSyncsQueueItem> items, ExtAsyncQueueState state,
                                    OffsetDateTime dateTime,
                                    String message) {
        setQueueItemState(items, state, dateTime, message);
    }

    public CreateEntityBuilder createNewEntityBuilder(final ApAccessPoint accessPoint,
                                                        final ApState state,
                                                      final ApExternalSystem apExternalSystem) {

        // TODO: rework to use ap_cached_access_point
        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint,
                state,
                apExternalSystem,
                this.groovyService,
                this.apDataService,
                state.getScope());
        ceb.build(partList, itemMap);
        return ceb;
    }

    public UpdateEntityBuilder createEntityUpdateBuilder(final ApAccessPoint accessPoint,
                                                        final ApBindingState bindingState,
                                                        final EntityXml entityXml,
                                                        final ApExternalSystem apExternalSystem) {
        ApState state = accessPointService.getStateInternal(accessPoint);

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        UpdateEntityBuilder ueb = new UpdateEntityBuilder(
                this.bindingItemRepository,
                this.staticDataService.getData(),
                state,
                bindingState,
                this.groovyService,
                this.apDataService,
                state.getScope(),
                apExternalSystem);

        List<Object> changes = ueb.build(entityXml, partList, itemMap);
        
        if (CollectionUtils.isEmpty(changes)) {
            log.error("Empty list of changes");
            return null;
        }
        return ueb;
    }

    private BatchInfoXml createBatchInfo(String userName) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(new LongStringXml(userName));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    /**
     * Regular entity synchronization
     * 
     * @param code
     */
    @Transactional
    public void synchronizeAccessPointsForExternalSystem(final String code) {
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(code);

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
        List<String> recordCodes = getRecordCodes(externalSystem, entityRecordRevInfoXmls);
        List<ApBinding> bindings = externalSystemService.findBindings(recordCodes, externalSystem);
        final Map<String, ApBinding> bindingMap = bindings.stream()
                .collect(Collectors.toMap(p -> p.getValue(), p -> p));
        
        Map<Integer, ApBindingState> bindingStateMap;
        if(bindings.size()>0) {
            List<ApBindingState> bindingStateList = externalSystemService.findBindingStates(bindings);
            bindingStateMap = bindingStateList.stream()
                    .collect(Collectors.toMap(p -> p.getBindingId(), p -> p));
        } else {
            bindingStateMap = Collections.emptyMap();
        }

        UsrUser user = userService.getLoggedUser();
        List<ExtSyncsQueueItem> extSyncsQueueItems = new ArrayList<>();
        for (String recordCode : recordCodes) {
            ApBinding binding = bindingMap.get(recordCode);
            ApAccessPoint ap = null;
            if(binding==null) {
                // prepare binding for CAM Complete
                if (externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                    binding = externalSystemService.createApBinding(recordCode, externalSystem);
                }
            } else {
                ApBindingState bindingState = bindingStateMap.get(binding.getBindingId());
                if (bindingState != null) {
                    ap = bindingState.getAccessPoint();
                }
            }
            // update or add new items from CAM_COMPLETE
            if (ap != null || externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                ExtSyncsQueueItem extSyncsQueue = new ExtSyncsQueueItem();
                if (ap != null) {
                    extSyncsQueue.setAccessPoint(ap);
                    extSyncsQueue.setState(ExtAsyncQueueState.UPDATE);
                } else {
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

    private List<String> getRecordCodes(final ApExternalSystem externalSystem,
                                        List<EntityRecordRevInfoXml> entityRecordRevInfoXmls) {
        if (CollectionUtils.isEmpty(entityRecordRevInfoXmls)) {
            return Collections.emptyList();
        }
        Function<EntityRecordRevInfoXml, String> idGetter;
        if (externalSystem.getType().equals(ApExternalSystemType.CAM_UUID)) {
            idGetter = (x) -> x.getEuid().getValue();
        } else {
            idGetter = (x) -> Long.toString(x.getEid().getValue());
        }

        List<String> recordCodes = new ArrayList<>(entityRecordRevInfoXmls.size());
        for (EntityRecordRevInfoXml entityRecordRevInfoXml : entityRecordRevInfoXmls) {
            recordCodes.add(idGetter.apply(entityRecordRevInfoXml));
        }
        return recordCodes;
    }

    static public Function<EntityXml, String> getEntityIdGetter(final ApExternalSystem externalSystem) {
        if (externalSystem.getType().equals(ApExternalSystemType.CAM_UUID)) {
            return (x) -> x.getEuid().getValue();
        } else {
            return (x) -> Long.toString(x.getEid().getValue());
        }
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
     * Synchronizace (vytvoření nového nebo aktualizace) přístupového bodu z
     * externího systému
     * 
     * binding nebo bindingState musí být předán vždy
     * 
     * @param procCtx
     *            context
     * @param state
     *            stav přístupového bodu
     * @param bindingState
     *            stav propojení s externím systémem
     * @param binding
     *            vazba na externí entit
     * @param entity
     *            entita z externího systému
     * @param syncQueue
     *            zda-li se jedná o volání z fronty
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state,
                                       ApBindingState bindingState,
                                       ApBinding binding,
                                       EntityXml entity, boolean syncQueue) {
        if (binding == null) {
            Validate.notNull(bindingState);

            binding = bindingState.getBinding();
        }
        if (bindingState == null) {
            Validate.isTrue(state == null);
        }
        log.debug("Entity synchronization request, bindingId: {}, value: {}, revId: {}, apState: {}, bindingState: {}",
                  binding.getBindingId(),
                  binding.getValue(), entity.getRevi().getRid().getValue(),
                  state, bindingState);

        // Mozne stavy synchronizace
        // ApState | ApBindingState  | syncQueue 
        // ---------------------------------------
        // null    | null            | false
        // null    | null            | true
        // ex      | null            | false -> vytvoreni bindingState
        // ex      | null            | true  -> vytvoreni bindingState
        // ex      | ex              | false
        // ex      | ex              | true

        ApChange apChange = null;
        ApBindingState origBindingState = bindingState;
        // Kontrola na zalozeni nove entity
        // overeni existence UUID
        if (bindingState == null && state == null) {
            ApAccessPoint accessPoint = apAccessPointRepository.findApAccessPointByUuid(entity.getEuid().getValue());
            if (accessPoint != null) {
                // entity exists
                apChange = apDataService.createChange(ApChange.Type.AP_SYNCH);
                // we can assign ap to the binding
                log.warn("Entity with uuid:{} already exists (id={}), automatically connected with external entity",
                        entity.getEuid().getValue(), accessPoint.getAccessPointId());

                SyncState syncState = syncQueue?SyncState.NOT_SYNCED : SyncState.SYNC_OK;
                bindingState = externalSystemService.createApBindingState(binding,
                                                                          accessPoint,
                                                                          apChange,
                                                                          entity.getEns().name(),
                                                                          entity.getRevi().getRid().getValue(),
                                                                          entity.getRevi().getUsr().getValue(),
                                                                          null, syncState);
                // if async -> has local changes -> mark as not synced
                if (syncQueue) {
                    accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
                    return;
                }
                // TODO: consider what to do and how to resolve this situation
            }
        }

        if (state != null && bindingState != null) {
            boolean localChanges = checkLocalChanges(state, bindingState);

            if (state.getDeleteChangeId() != null || // do not sync deleted aps, mark as not synced
            // check if synced or not
                    (syncQueue && (localChanges || SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())))) {
                if (!SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())) {
                    bindingState.setSyncOk(SyncState.NOT_SYNCED);
                    bindingStateRepository.save(bindingState);
                }
                return;
            }
            if (!localChanges) {
                // check if any update is needed
                if (SyncState.SYNC_OK.equals(bindingState.getSyncOk()) &&
                        origBindingState != null &&
                        Objects.equals(origBindingState.getExtRevision(), entity.getRevi().getRid().toString())) {
                    // binding already exists and no local changes are detected
                    // -> nothing to synchronize -> return
                    return;
                }

            }
        }

        if (apChange == null) {
            apChange = apDataService.createChange(ApChange.Type.AP_SYNCH);
            //apChange = apDataService.createChange(bindingState != null? ApChange.Type.AP_UPDATE : ApChange.Type.AP_CREATE);
        }
        procCtx.setApChange(apChange);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        if (state == null) {
            ec.createAccessPoint(procCtx, entity, binding, syncQueue);
        } else {
            ec.synchronizeAccessPoint(procCtx, state, bindingState, entity, syncQueue);
        }

        procCtx.setApChange(null);
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

    /**
     * Update accesspoint from WSDL/API
     * 
     * @param procCtx
     * @param syncRequests
     */
    public void updateAccessPoints(final ProcessingContext procCtx,
                                   final List<SyncEntityRequest> syncRequests) {
        if (CollectionUtils.isEmpty(syncRequests)) {
            return;
        }

        for (SyncEntityRequest syncReq : syncRequests) {
            Validate.notNull(syncReq.getBindingState());

            synchronizeAccessPoint(procCtx, syncReq.getState(),
                                   syncReq.getBindingState(),
                                   null,
                                   syncReq.getEntityXml(), false);
        }

        // kontrola datové struktury
        if (checkDb) {
            entityManager.flush();
            accessPointService.checkConsistency();
        }
    }

    /**
     * Příprava synchronizace Elza -> CAM
     * 
     * @param extSyncsQueueItem
     * @return
     * @throws ApiException
     */
    @Transactional
    public UploadWorker prepareUpload(ExtSyncsQueueItem extSyncsQueueItem) throws ApiException {
        Integer externalSystemId = extSyncsQueueItem.getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);

        ApAccessPoint accessPoint = accessPointService.getAccessPointInternal(extSyncsQueueItem.getAccessPointId());
        ApState state = accessPointService.getStateInternal(accessPoint);
        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint,
                                                                                               externalSystem);
        BatchUpdateXml batchUpdate = new BatchUpdateXml();
        batchUpdate.setInf(createBatchInfo(extSyncsQueueItem.getUsername()));
        if (bindingState == null) {
            // create new item
            CreateEntityBuilder xmlBuilder = createNewEntityBuilder(accessPoint, state, externalSystem);
            if (xmlBuilder == null) {
                return null;
            }
            batchUpdate.getChanges().add(xmlBuilder.getResult());

            UpdateEntityWorker uew = new UpdateEntityWorker(extSyncsQueueItem,
                    batchUpdate,
                    xmlBuilder.getItemUuids(), xmlBuilder.getPartUuids());
            return uew;

        } else {
            // update entity
            // TODO: try to prepare update without downloading current entity
            EntityXml entity = camConnector.getEntityById(bindingState.getBinding().getValue(), externalSystem);
            // update existing item
            UpdateEntityBuilder xmlBuilder = createEntityUpdateBuilder(accessPoint, bindingState, entity,
                                                                           externalSystem);
            if (xmlBuilder == null) {
                return null;
            }
            batchUpdate.getChanges().addAll(xmlBuilder.getResult());

            UpdateEntityWorker uew = new UpdateEntityWorker(extSyncsQueueItem,
                    batchUpdate,
                    xmlBuilder.getItemUuids(), xmlBuilder.getPartUuids());
            return uew;
        }
    }

    /**
     * Synchronizace záznamů ELZA -> CAM
     * 
     * @param extSyncsQueueItem
     * @param batchUpdateXml
     * @throws ApiException
     */
    public BatchUpdateResultXml upload(ExtSyncsQueueItem extSyncsQueueItem, BatchUpdateXml batchUpdateXml)
            throws ApiException {
        Integer externalSystemId = extSyncsQueueItem.getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);

        BatchUpdateResultXml batchUpdateResult = camConnector.postNewBatch(batchUpdateXml, externalSystem);
        return batchUpdateResult;
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
            log.error("Failed to synchronize access point, accessPointId: {}", state.getAccessPointId(), e);
            /*setQueueItemState(queueItem,
                              ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                              OffsetDateTime.now(),
                              e.getMessage());
              return true;
                              */
            throw e;

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
     * @param queueItems
     *            seznam, objekty nejsou připojeny k transakci a je nutné je znovu
     *            načíst
     * @throws ApiException
     */
    @Transactional
    public void importNew(List<ExtSyncsQueueItem> queueItems) throws ApiException {
        if (queueItems.isEmpty()) {
            return;
        }
        Integer externalSystemId = queueItems.get(0).getExternalSystemId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);        

        List<Integer> bindingIds = queueItems.stream().map(p -> p.getBindingId()).collect(Collectors.toList());
        List<ApBinding> bindings = bindingRepository.findAllById(bindingIds);
        Map<String, ApBinding> bindingMap = bindings.stream().collect(Collectors.toMap(x -> x.getValue(), x -> x));

        List<String> bindingValues = bindings.stream().map(p -> p.getValue()).collect(Collectors.toList());
        log.debug("Download entity from CAM, bindingValues: {} externalSystem: {}", bindingValues, externalSystem.getCode());

        EntitiesXml entities = camConnector.getEntitiesByIds(bindingValues, externalSystem.getCode());
        
        importNew(externalSystem, entities, bindingMap);

        setQueueItemState(queueItems,
                         ExtSyncsQueueItem.ExtAsyncQueueState.OK,
                         OffsetDateTime.now(),
                         "Synchronized: ES -> ELZA");
    }
    
    public void importNew(ApExternalSystem externalSystem, EntitiesXml entities, Map<String, ApBinding> bindingMap) {
    	ApScope scope = externalSystem.getScope();
    	
        ProcessingContext procCtx = new ProcessingContext(scope, externalSystem, staticDataService);
        for (EntityXml entity : entities.getList()) {
            String value = String.valueOf(entity.getEid().getValue());
            ApBinding binding = bindingMap.get(value);
            synchronizeAccessPoint(procCtx, null, null, binding, entity, true);
        }    	
    }


    @Transactional
    public void resetSynchronization(String code) {
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(code);

        ApBindingSync bindingSync = bindingSyncRepository.findByApExternalSystem(externalSystem);
        if (bindingSync == null) {
            // nothing to reset
            return;
        }
        if (TRANSACTION_UUID.equals(bindingSync.getLastTransaction())) {
            log.debug("Accesspoint synchronization is already set to initial transaction, externalSystemId: {}.",
                      externalSystem.getExternalSystemId());
            return;
        }
        log.info("Resettting accesspoint synchronization (externalSystemId: {}) transaction from: {} to: {}",
                 externalSystem.getExternalSystemId(),
                 bindingSync.getLastTransaction(),
                 TRANSACTION_UUID);
        bindingSync.setLastTransaction(TRANSACTION_UUID);
        bindingSyncRepository.save(bindingSync);
    }
}
