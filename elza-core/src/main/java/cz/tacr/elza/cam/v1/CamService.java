package cz.tacr.elza.cam.v1;

import static cz.tacr.elza.cam.v1.CamException.prepareExtSystemException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.cam.v1.client.ApiException;
import cz.tacr.cam.v1.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.v1.schema.cam.BatchInfoXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateErrorXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.v1.schema.cam.BatchUpdateXml;
import cz.tacr.cam.v1.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.cam.v1.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.v1.schema.cam.EntityXml;
import cz.tacr.cam.v1.schema.cam.LongStringXml;
import cz.tacr.cam.v1.schema.cam.UpdatesFromXml;
import cz.tacr.cam.v1.schema.cam.UpdatesXml;
import cz.tacr.cam.v1.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.BindingSyncInfo;
import cz.tacr.elza.cam.CamUserService;
import cz.tacr.elza.cam.ProcessingContext;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApBindingSync;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.SysExternalSystemProperty;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApBindingSyncRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.repository.ExtSyncsQueueItemRepository;
import cz.tacr.elza.repository.SysExternalSystemPropertyRepository;
import cz.tacr.elza.service.AccessPointConnectorService;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.DataService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RevisionService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.UserService;
import cz.tacr.elza.service.cache.AccessPointCacheService;

@Service
public class CamService {

    static private final Logger log = LoggerFactory.getLogger(CamService.class);

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;
    
    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingSyncRepository bindingSyncRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointConnectorService apConnectService;
    
    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private DataService dataService;

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
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;
    
    @Autowired
    private SysExternalSystemPropertyRepository extSysPropRepository;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private UserService userService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private CamUserService camUserService;

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private PlatformTransactionManager txManager;

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
                ruleService,
                this);
    }

    public List<ApState> takeAccessPoints(final ProcessingContext procCtx, final List<EntityXml> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }

        ApChange apChange = procCtx.getApChange();
        if (apChange == null) {
            apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
            procCtx.setApChange(apChange);
        }

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.takeEntities(procCtx, entities);

        return ec.getApStates();
    }

    @AuthMethod(permission = {UsrPermission.Permission.AP_EXTERNAL_WR})
    public void connectAccessPoint(final ApState state, final EntityXml entity,
                                   final ProcessingContext procCtx, final boolean replace) {
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        procCtx.setApChange(apChange);

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());

        state.setDeleteChange(apChange);
        var stateSaved = stateRepository.save(state);
        stateRepository.flush();
        ApState stateNew = accessPointService.copyState(stateSaved, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(ApState.StateApproval.NEW);
        stateNew = stateRepository.save(stateNew);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.connectEntity(procCtx, stateNew, entity, replace, false);
    }

    /**
     * Vytvoreni novych propojeni (binding) pro vztahy
     *
     * @param dataRefList
     * @param procCtx
     */
    public void createBindingForRel(final List<ReferencedEntities> dataRefList, final ProcessingContext procCtx) {
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
        log.debug("Creating binding for rel, dataId: {}, value: {}, extSystem: {}",
                  dataRecordRef.getDataId(), value, procCtx.getApExternalSystem().getCode());

        ApBinding refBinding = externalSystemService.findByValueAndExternalSystem(value, procCtx.getApExternalSystem());

        ApAccessPoint referencedAp = null;
        if (refBinding == null) {
            // check if item should be lookup also by UUID
            if (ApExternalSystemType.CAM_UUID.equals(procCtx.getApExternalSystem().getType())) {
                referencedAp = this.apAccessPointRepository.findAccessPointByUuid(value);
                // finding by UUID
                log.debug("Finding connected AP by UUID, accessPointId: {}",
                          referencedAp != null ? referencedAp.getAccessPointId() : null);
            } else {
                // check if not in the processing context
                refBinding = procCtx.getBindingByValue(value);
                // looking in procCtx
                log.debug("Finding connected AP in processing context, bindingId: {}",
                          refBinding != null ? refBinding.getBindingId() : null);
            }
               if (referencedAp == null && refBinding == null) {
                   // we can create new - last resort
                refBinding = externalSystemService.createApBinding(value, procCtx.getApExternalSystem(), true);
                procCtx.addBinding(refBinding);

                log.debug("Prepared new binding, bindingId: {}", refBinding.getBindingId());
               }
        } else {
            log.debug("Found existing binding, bindingId: {}", refBinding.getBindingId());
            // try to find access point for binding
            Optional<ApBindingState> bindingStateOpt = bindingStateRepository.findActiveByBinding(refBinding);
            if(bindingStateOpt.isPresent()) {
                ApBindingState bindingState = bindingStateOpt.get();
                log.debug("Found existing bindingState, bindingStateId: {}, accessPointId: {}",
                          bindingState.getBindingId(),
                          bindingState.getAccessPointId());
                referencedAp = bindingState.getAccessPoint();
            }
        }
        Validate.isTrue(referencedAp!=null||refBinding!=null, "Failed to prepare referenced record.");

        dataRecordRef.setRecord(referencedAp);
        dataRecordRef.setBinding(refBinding);
        dataRecordRefRepository.save(dataRecordRef);
    }

    /**
     * Update entity status after successful transfer to external system
     * Transfer: Elza -> ES
     *
     * @param extSyncsQueueItem
     * @param batchUpdateSaved
     * @param itemUuidMap
     * @param partUuidMap
     * @param stateMap
     * @param batchInfoXml 
     */
    @Transactional
    public void updateBinding(ExtSyncsQueueItem extSyncsQueueItem,
                              BatchUpdateSavedXml batchUpdateSaved,
                              Map<Integer, String> itemUuidMap,
                              Map<Integer, String> partUuidMap,
                              Map<Integer, String> stateMap, 
                              BatchInfoXml batchInfoXml) {
        log.debug("Updating binding, extSyncsQueueItemId: {}, accessPointId: {}", extSyncsQueueItem.getExtSyncsQueueItemId(), extSyncsQueueItem.getAccessPointId());
        
        ApState state = accessPointService.getStateInternal(extSyncsQueueItem.getAccessPointId());
        ApAccessPoint accessPoint = state.getAccessPoint();
        ApExternalSystem apExternalSystem = externalSystemService.getExternalSystemInternal(extSyncsQueueItem.getExternalSystemId());

        BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

        String camApState = stateMap.get(extSyncsQueueItem.getAccessPointId());
        if (camApState == null) {
            camApState = EntityRecordStateXml.ERS_NEW.toString();
        }

        ApChange change = apDataService.createChange(ApChange.Type.AP_SYNCH);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint,
                                                                                                apExternalSystem);
        ApBinding binding;
        LongStringXml userName = (LongStringXml) batchInfoXml.getBatchUserInfo();

        if (bindingState != null) {
            binding = bindingState.getBinding();
            bindingState = externalSystemService.createBindingState(bindingState, change, camApState,
                                                                    batchEntityRecordRev.getRev().getValue(),
                                                                    userName.getValue(),
                                                                    bindingState.getExtReplacedBy(),
                                                                    SyncState.SYNC_OK,
                                                                    accessPoint.getPreferredPart(),
                                                                    state.getApType());
        } else {
            String bindingValue = Long.toString(batchEntityRecordRev.getEid().getValue());
            // check if binding exists
            binding = externalSystemService.findByValueAndExternalSystem(bindingValue, apExternalSystem);
            if (binding == null) {
                // binding does not exist
                binding = externalSystemService.createApBinding(bindingValue, apExternalSystem, true);
            } else {
                log.debug("Found existing binding, bindingId: {}, disconnecting existing binding", binding.getBindingId());
                // invalidate existing binding state
                // this usually happen in test environment where binding is used in multiple access points
                // in production it should not happen
                // we can add some condition to control this behavior
                var curBindingState = bindingStateRepository.findActiveByBinding(binding);
                if (curBindingState.isPresent()) {
                    log.info("Deleting previouse binding state, bindingStateId: {}", curBindingState.get().getBindingStateId());
                    curBindingState.get().setDeleteChange(change);                    
                    bindingStateRepository.saveAndFlush(curBindingState.get());
                }
                var bindingItems = bindingItemRepository.findByBinding(binding);
                for(var bindingItem : bindingItems) {
                    log.info("Deleting previouse binding item, bindingItemId: {}, value: {}", bindingItem.getBindingId(), bindingItem.getValue());
                    bindingItem.setDeleteChange(change);
                    bindingItemRepository.saveAndFlush(bindingItem);
                }
            }
            bindingState = externalSystemService.createBindingState(binding, accessPoint, change, camApState,
                                                                    batchEntityRecordRev.getRev().getValue(),
                                                                    userName.getValue(), null, SyncState.SYNC_OK,
                                                                    accessPoint.getPreferredPart(),
                                                                    state.getApType());
        }

        // Create bindings
        var finalBinding = binding;
        itemUuidMap.forEach((itemId, value) -> {
            ApItem item = entityManager.getReference(ApItem.class, itemId);
            this.externalSystemService.createApBindingItem(finalBinding, change, value, null, item);
        });
        partUuidMap.forEach((partId, value) -> {
            ApPart part = entityManager.getReference(ApPart.class, partId);
            this.externalSystemService.createApBindingItem(finalBinding, change, value, part, null);
        });

        apConnectService.setQueueItemState(Collections.singletonList(extSyncsQueueItem), ExtAsyncQueueState.EXPORT_OK, null, null, null, null);

        accessPointCacheService.createApCachedAccessPoint(extSyncsQueueItem.getAccessPointId());
    }

    public CreateEntityBuilder createNewEntityBuilder(final ApAccessPoint accessPoint,
                                                      final ApState state,
                                                      final ApExternalSystem apExternalSystem) {
        // TODO: rework to use ap_cached_access_point
        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = apItemService.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        CreateEntityBuilder ceb = new CreateEntityBuilder(this.externalSystemService,
                this.staticDataService.getData(),
                accessPoint,
                state,
                apExternalSystem,
                this.groovyService,
                this.dataService,
                state.getScope());
        if(!ceb.build(partList, itemMap)) {
            return null;
        }
        return ceb;
    }

    public UpdateEntityBuilder createEntityUpdateBuilder(final ApAccessPoint accessPoint,
                                                         final ApBindingState bindingState,
                                                         final EntityXml entityXml,
                                                         final ApExternalSystem apExternalSystem) throws ApiException {
        ApState state = accessPointService.getStateInternal(accessPoint);

        List<ApPart> partList = partService.findPartsByAccessPoint(state.getAccessPoint());
        Map<Integer, List<ApItem>> itemMap = apItemService.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));

        UpdateEntityBuilder ueb = new UpdateEntityBuilder(
                this.externalSystemService,
                this.bindingItemRepository,
                this.staticDataService.getData(),
                state,
                bindingState,
                this.groovyService,
                this.dataService,
                state.getScope(),
                apExternalSystem);

        List<Object> changes = ueb.build(entityXml, partList, itemMap);

        if (CollectionUtils.isEmpty(changes)) {
            log.error("Empty list of changes");
            return null;
        }
        return ueb;
    }

    /**
     * Create batch info
     * @param externalSystem External system where to send data
     * @param user who to send data
     * @return
     */
    private BatchInfoXml createBatchInfo(ApExternalSystem externalSystem, UsrUser user) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(createUserInfo(externalSystem.getUserInfo(), user));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    /**
     * Wrap the rendered user info string into the v1 API type.
     *
     * @param userInfo template
     * @param user sending user
     * @return user info XML value
     */
    private LongStringXml createUserInfo(String userInfo, UsrUser user) {
        return new LongStringXml(camUserService.buildUserInfo(userInfo, user));
    }

    /**
     * Regular entity synchronization
     *
     * @param extSysCode
     */
    synchronized public void synchronizeAccessPointsForExternalSystem(final String extSysCode) {
        BindingSyncInfo bindingSync = externalSystemService.getBindingSync(extSysCode, TRANSACTION_UUID);
        try {
            String lastTransaction = bindingSync.getLastTransaction();
            String toTransaction = null;
            Integer count = null;
            Integer page = 0;
            if (bindingSync.getToTransaction() == null || bindingSync.getPage() == null || bindingSync.getCount() == null) {
                // get next updates and count of changes
            	UpdatesFromXml updatesFromXml = camConnector.getUpdatesFrom(bindingSync.getLastTransaction(), bindingSync.getExternalSystemId());

                if (updatesFromXml.getUps() != null && CollectionUtils.isNotEmpty(updatesFromXml.getUps().getRevisions())) {
                    // We received all updated items
                    List<EntityRecordRevInfoXml> entityRecordRevInfoXmls = updatesFromXml.getUps().getRevisions();
                    
                    new TransactionTemplate(txManager).executeWithoutResult(status -> 
                    	prepareApsForSync(bindingSync.getId(), entityRecordRevInfoXmls, updatesFromXml.getInf().getTo().getValue(), null, null, null)
                    );
                } else {
                    // Musí být uloženo po přečtení plné dávky dat.
                    toTransaction = updatesFromXml.getInf().getTo().getValue();
                    count = updatesFromXml.getInf().getCnt().getValue().intValue();
                }
            } else {
                toTransaction = bindingSync.getToTransaction();
                count = bindingSync.getCount();
                // Lot of changes -> have to read with pagination
                Integer lastPage = bindingSync.getPage();
                page = (lastPage != null)? lastPage :  0;
            }
            log.debug("Total entity count for update: {}, last transaction: {}", count, toTransaction);

            while (count != null && count > 0) {
                page++;

                log.debug("Requesting entity info, page: {}, pageSize: {}", page, PAGE_SIZE);
                UpdatesXml updatesXml = camConnector.getUpdatesFromTo(lastTransaction, toTransaction, page, PAGE_SIZE, bindingSync.getExternalSystemId());

                count -= updatesXml.getRevisions().size();
                log.debug("Received entity revisions, page: {}, count: {}", page, updatesXml.getRevisions().size());

                // při zpracování poslední stránky musíme upravit hodnoty
                if (count <= 0 || updatesXml.getRevisions().size() < PAGE_SIZE) {
                    lastTransaction = toTransaction;
                    toTransaction = null;
                    page = null;
                    count = null;
                }
                
                var paramLastTransaction = lastTransaction;
                var paramToTransaction = toTransaction;
                var paramPage = page;
                var paramCount = count;

                new TransactionTemplate(txManager).executeWithoutResult(status ->
                	prepareApsForSync(bindingSync.getId(), updatesXml.getRevisions(), paramLastTransaction, paramToTransaction, paramPage, paramCount)
                	);
            }
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                // Transaction not found, check if autorestart is enabled
                log.error("Transaction not found, transaction={}, resetting transaction.", bindingSync.getLastTransaction(), e);
                externalSystemService.resetTransaction(bindingSync.getId(), TRANSACTION_UUID);
            } else {
                log.error("Failed to send data to external system, responseCode: {}, responseBode: {}", e.getCode(), e.getResponseBody(), e);
                throw prepareExtSystemException(e);
            }
        }
    }

    /**
     * Prepare entities for synchronization
     *
     * @param bindingSyncId
     * @param entityRecordRevInfoXmls entity info list
     * @param lastTransaction
     * @param toTransaction
     * @param page
     * @param count
     */
    @Transactional
    public void prepareApsForSync(Integer bindingSyncId, List<EntityRecordRevInfoXml> entityRecordRevInfoXmls,
                                  String lastTransaction, String toTransaction,
                                  Integer page, Integer count) {
        log.debug("Preparing APs for synchronization from external system, count: {}", entityRecordRevInfoXmls.size());

        // Prepare keys
        ApBindingSync bindingSync = bindingSyncRepository.getOneCheckExist(bindingSyncId);
        ApExternalSystem externalSystem = bindingSync.getApExternalSystem();
        List<String> keyList = new ArrayList<>(entityRecordRevInfoXmls.size());
        Map<String, EntityRecordRevInfoXml> recordCodesMap = new HashMap<>();
        Function<EntityRecordRevInfoXml, String> idGetter;
        if (externalSystem.getType().equals(ApExternalSystemType.CAM_UUID)) {
            idGetter = (x) -> x.getEuid().getValue();
        } else {
            idGetter = (x) -> Long.toString(x.getEid().getValue());
        }
        for (EntityRecordRevInfoXml entityRecordRevInfoXml : entityRecordRevInfoXmls) {
            String id = idGetter.apply(entityRecordRevInfoXml);
            keyList.add(id);
            EntityRecordRevInfoXml prevInfo = recordCodesMap.put(id, entityRecordRevInfoXml);
            Validate.isTrue(prevInfo == null, "Record with same key already process, %s", id);
        }

        List<ApBinding> bindings = externalSystemService.findBindings(keyList, externalSystem);
        final Map<String, ApBinding> bindingMap = bindings.stream().collect(Collectors.toMap(p -> p.getValue(), p -> p));

        Map<Integer, ApBindingState> bindingStateMap;
        if (bindings.size() > 0) {
            List<ApBindingState> bindingStateList = externalSystemService.findBindingStates(bindings);
            bindingStateMap = bindingStateList.stream().collect(Collectors.toMap(p -> p.getBindingId(), p -> p));
        } else {
            bindingStateMap = Collections.emptyMap();
        }

        int recNo = 0;

        UsrUser user = userService.getLoggedUser();
        for (String recordCode : keyList) {
            recNo++;
            if (log.isDebugEnabled()) {
                if (recNo%100 == 0) {
                    log.debug("Prepared records for sync: [{}-{}]", ((recNo+99)/100-1)*100+1, recNo);
                }
            }

            ApBinding binding = bindingMap.get(recordCode);
            ApAccessPoint ap = null;
            if (binding == null) {
                // prepare binding for CAM Complete
                if (externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
                    // we are creating all bindings at once
                    // - will be flush to the DB at the end of this method
                    binding = externalSystemService.createApBinding(recordCode, externalSystem, false);
                }
            } else {
                ApBindingState bindingState = bindingStateMap.get(binding.getBindingId());
                EntityRecordRevInfoXml xmlRecordInfo = recordCodesMap.get(recordCode);
                if (bindingState != null) {
                    ap = bindingState.getAccessPoint();
                    // kontrola uuid revizi, pokud se rovná extRevizion(), pak aktualizace není potřeba
                    String uuidRev = xmlRecordInfo.getRev().getValue();
                    if (bindingState.getExtRevision().equals(uuidRev)) {
                        continue;
                    }
                }
                // entita mohla být smazána, hledáme ji jinak
                if (ap == null) {
                    String uuid = xmlRecordInfo.getEuid().getValue();
                    ap = apAccessPointRepository.findAccessPointByUuid(uuid);
                }
            }
            // update or add new items from CAM_COMPLETE
            if (ap != null || externalSystem.getType() == ApExternalSystemType.CAM_COMPLETE) {
            	externalSystemService.createExtSyncsQueueItem(ap, externalSystem, binding, null,
                                                              ap != null? ExtAsyncQueueState.UPDATE : ExtAsyncQueueState.IMPORT_NEW,
                                                              OffsetDateTime.now(),
                                                              user);
            }
        }
        if (log.isDebugEnabled()) {
            if (recNo%100 != 0) {
                log.debug("Prepared records for sync: [{}-{}]", ((recNo+99)/100-1)*100+1, recNo);
            }
        }
        log.debug("APs prepared for synchronization from external system");
        log.info("To queue ext_syncs_queue_item added {} records for sync.", recNo);

        // aktualizace dat
        bindingSync.setLastTransaction(lastTransaction);
        bindingSync.setToTransaction(toTransaction);
        bindingSync.setPage(page);
        bindingSync.setCount(count);
        bindingSyncRepository.saveAndFlush(bindingSync);
    }

    static public Function<EntityXml, String> getEntityIdGetter(final ApExternalSystem externalSystem) {
        if (externalSystem.getType().equals(ApExternalSystemType.CAM_UUID)) {
            return (x) -> x.getEuid().getValue();
        } else {
            return (x) -> Long.toString(x.getEid().getValue());
        }
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
     * binding musí být předán vždy
     *
     * Metoda musí mít nastaven securityContext pro aktivního uživatele
     *
     * @param procCtx   context
     * @param binding   vazba na externí entit
     * @param entity    entita z externího systému
     * @param syncQueue
     *            zda-li se jedná o volání z fronty
     *            při volání z fronty:
     *            - lokálně smazaná entita není obnovena (změna stavu)
     * @return accessPoint belonging to the binding after the sync, or {@code null}
     *         when the entity was skipped (e.g. INVALID/REPLACED state for a new
     *         import)
     * @throws  SyncImpossibleException
     */
    public ApAccessPoint synchronizeAccessPoint(ProcessingContext procCtx,
                                       @NotNull ApBinding binding,
                                       @NotNull EntityXml entity, boolean syncQueue) throws SyncImpossibleException {
        Objects.requireNonNull(binding);
        Objects.requireNonNull(entity);

        log.debug("Entity synchronization request, bindingId: {}, value: {}, revId: {}",
                  binding.getBindingId(), binding.getValue(), entity.getRevi().getRid().getValue());

        // Mozne stavy synchronizace
        // ApState | ApBindingState  | syncQueue
        // ---------------------------------------
        // null    | null            | false
        // null    | null            | true
        // ex      | null            | false -> vytvoreni bindingState
        // ex      | null            | true  -> vytvoreni bindingState
        // ex      | ex              | false
        // ex      | ex              | true
        ApAccessPoint accessPoint = null;
        ApState state = null;
        ApBindingState bindingState = externalSystemService.getBindingState(binding).orElse(null);
        ApChange apChange = null;
        if (bindingState != null) {
            // ap exists
            accessPoint = bindingState.getAccessPoint();
            state = accessPointService.getStateInternal(accessPoint);
        } else {            
            // Kontrola na zalozeni nove entity
            // overeni existence UUID
            accessPoint = apAccessPointRepository.findAccessPointByUuid(entity.getEuid().getValue());
            if (accessPoint != null) {
                // Check if entity has other binding state in the external system
                // if found throw SyncImpossibleException
                bindingState = externalSystemService.getBindingState(accessPoint, procCtx.getApExternalSystem());
                if (bindingState != null) {
                    throw new SyncImpossibleException("Found accesspoint by UUID but with different binding, accessPointId: " + accessPoint.getAccessPointId());
                }

                apChange = apDataService.createChange(ApChange.Type.AP_SYNCH);
                // we can assign ap to the binding
                log.warn("Entity with uuid:{} already exists (id={}), automatically connected with external entity",
                         entity.getEuid().getValue(), accessPoint.getAccessPointId());
                state = accessPointService.getStateInternal(accessPoint);
                if (state == null) {
                    // ap without apState -> this is DB inconsistency
                    throw new BusinessException("AccessPoint without state, accessPointId: " + accessPoint.getAccessPointId(), 
                                                BaseCode.DB_INTEGRITY_PROBLEM)
                                    .set("accessPointId", accessPoint.getAccessPointId());
                }
                if (state.getDeleteChangeId() != null) {                    
                    // pokud state smazan && bindingState == null mohlo by jít o obnovení neplatné entity
                    state = accessPointService.copyState(state, apChange);
                }

                SyncState syncState = syncQueue ? SyncState.NOT_SYNCED : SyncState.SYNC_OK;

                bindingState = externalSystemService.createBindingState(binding,
                                                                        accessPoint,
                                                                        apChange,
                                                                        entity.getEns().name(),
                                                                        entity.getRevi().getRid().getValue(),
                                                                        entity.getRevi().getUsr().getValue(),
                                                                        null, syncState,
                                                                        // We do not know yet prefPart and type
                                                                        // It is Ok for not synced AP
                                                                        null, null);
                // if async(syncQueue) -> has local changes -> mark as not synced
                if (syncQueue) {
                    accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
                    return accessPoint;
                }
            } else {
                // ap not found -> new import
            }
        }

        ApBindingState origBindingState = bindingState;
        // Pokud je state!=null, tak musi byt vzdy bindingState!=null
        if (state != null && bindingState != null) {
            if (state.getStateApproval().equals(StateApproval.TO_APPROVE)) {
                if (syncQueue) {
                    if (!SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())) {
                        bindingState.setSyncOk(SyncState.NOT_SYNCED);
                        bindingStateRepository.save(bindingState);
                        accessPointCacheService.createApCachedAccessPoint(state.getAccessPointId());
                    }
                    return accessPoint;
                } else {
                    throw new SystemException("Entitu v tomto stavu nelze aktualizovat z externího systému", BaseCode.INVALID_STATE)
                        .set("accessPointId", state.getAccessPointId())
                        .set("state", state.getStateApproval());
                }
            }

            // Nelze změnit stav archivní entity, která má revizi
            ApRevision revision = revisionService.findRevisionByState(state);
            boolean modifiedPartOrItem = hasModifiedPartOrItem(state, bindingState);

            // Check if state was locally modified?
            if (state.getDeleteChangeId() != null && state.getDeleteChangeId()>bindingState.getCreateChangeId()) {
                // entity was locally deleted -> mark as not for sync
                modifiedPartOrItem = true;
            }

            // Nesynchronizovat pokud se jedná o volání z fronty A
            //    (existují lokální změny NEBO existují revize NEBO entita ve stavu NOT_SYNCED)
            // jinak synchronizovat, i když entita je neplatná
            if (syncQueue && (modifiedPartOrItem || revision != null || SyncState.NOT_SYNCED.equals(bindingState.getSyncOk()))) {
                if (!SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())) {
                    bindingState.setSyncOk(SyncState.NOT_SYNCED);
                    bindingStateRepository.save(bindingState);
                    accessPointCacheService.createApCachedAccessPoint(state.getAccessPointId());
                }
                return accessPoint;
            }
            if (!modifiedPartOrItem) {
                // check if any update is needed
                if (SyncState.SYNC_OK.equals(bindingState.getSyncOk()) &&
                        origBindingState != null &&
                        Objects.equals(origBindingState.getExtRevision(), entity.getRevi().getRid().toString())) {
                    // binding already exists and no local changes are detected
                    // -> nothing to synchronize -> return
                    return accessPoint;
                }

            }

            if (revision != null) {
                throw new BusinessException("Nelze změnit stav archivní entity, která má revizi",
                        RegistryCode.CANT_CHANGE_STATE_ENTITY_WITH_REVISION);
            }
        }

        if (apChange == null) {
            apChange = apDataService.createChange(ApChange.Type.AP_SYNCH);
        }
        procCtx.setApChange(apChange);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        if (state == null) {
            // check received entity state, process NEW or ERS_APPROVED, skip INVALID and REPLACED
            if (entity.getEns().equals(EntityRecordStateXml.ERS_NEW)
                    || entity.getEns().equals(EntityRecordStateXml.ERS_APPROVED)) {

                // binding state is updated inside ec
                ec.createAccessPoint(procCtx, entity, binding, syncQueue);
                bindingState = ec.getBindingState();
                Validate.notNull(bindingState, "Missing binding state");
                accessPoint = bindingState.getAccessPoint();
            }
        } else {
            ec.synchronizeAccessPoint(procCtx, state, bindingState, entity, syncQueue);
        }

        procCtx.setApChange(null);
        return accessPoint;
    }

    /**
     * Kontrola, zda existuje lokální změna v části nebo prvku popisu
     *
     * @param state
     * @param bindingState
     * @return
     */
    public boolean hasModifiedPartOrItem(final ApState state,
                                      final ApBindingState bindingState) {
        List<ApPart> partList = partService.findNewerPartsByAccessPoint(state.getAccessPoint(), bindingState.getCreateChangeId());
        if (CollectionUtils.isNotEmpty(partList)) {
            return true;
        }
        List<ApItem> itemList = apItemService.findNewerValidItemsByAccessPoint(state.getAccessPoint(), bindingState.getCreateChangeId());
        if (CollectionUtils.isNotEmpty(itemList)) {
            return true;
        }

        return false;
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
        ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(accessPoint, externalSystem);
        UsrUser user = userService.getUserInternal(extSyncsQueueItem.getUserId());
        BatchUpdateXml batchUpdate = new BatchUpdateXml();        
        batchUpdate.setInf(createBatchInfo(externalSystem, user));
        BatchUpdateBuilder xmlBuilder;
        if (bindingState == null) {
            // create new item
            xmlBuilder = createNewEntityBuilder(accessPoint, state, externalSystem);
        } else {
            // update entity
            // TODO: try to prepare update without downloading current entity
            EntityXml entity = camConnector.getEntity(bindingState.getBinding().getValue(), externalSystem);
            // update existing item
            xmlBuilder = createEntityUpdateBuilder(accessPoint, bindingState, entity, externalSystem);
        }
        if (xmlBuilder == null) {
            return null;
        }
        xmlBuilder.storeChanges(batchUpdate);
        UpdateEntityWorker uew = new UpdateEntityWorker(extSyncsQueueItem,
                batchUpdate,
                xmlBuilder.getItemUuids(), xmlBuilder.getPartUuids(),
                xmlBuilder.getBindingStates());
        return uew;
    }

    /**
     * Synchronizace záznamů ELZA -> CAM
     *
     * @param extSyncsQueueItem
     * @param batchUpdateXml
     * @throws ApiException
     */
    public BatchUpdateResultXml upload(ExtSyncsQueueItem extSyncsQueueItem, BatchUpdateXml batchUpdateXml) throws ApiException {
        Integer externalSystemId = extSyncsQueueItem.getExternalSystemId();
        Integer userId = extSyncsQueueItem.getUserId();
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
        UsrUser user = userService.getUserInternal(userId);

        // check if sending user has some extra privileges
        log.debug("Upload to: {}(id: {}), user: {}(id: {}), batchId: {}", externalSystem.getName(), externalSystemId,
                  (user != null) ? user.getUsername() : null, userId,
                          batchUpdateXml.getInf().getBid().getValue());
        List<SysExternalSystemProperty> extSysProperties;
        String apikeyId = null, apikeyValue = null;
        if (user != null) {
            extSysProperties = extSysPropRepository.findByExternalSystemAndUser(externalSystem, user);
            for (SysExternalSystemProperty property : extSysProperties) {
                if (property.getName().equals(CamConnector.APIKEY_ID)) {
                    apikeyId = property.getValue();
                    log.debug("Found extra APIKEY_ID (propertyId: {}): {}", property.getExternalSystemPropertyId(),
                              apikeyId);
                }
                if (property.getName().equals(CamConnector.APIKEY_VALUE)) {
                    apikeyValue = property.getValue();
                    log.debug("Found extra APIKEY_VALUE (propertyId: {}): ****",
                              property.getExternalSystemPropertyId());
                }
            }
        }

        BatchUpdateResultXml batchUpdateResult = camConnector.postNewBatch(batchUpdateXml, externalSystem, apikeyId, apikeyValue);
        if(log.isDebugEnabled()) {
            if(batchUpdateResult instanceof BatchUpdateResultXml) {
                log.debug("Upload result success: {}(id: {}), batchId: {} ", externalSystem.getName(), externalSystemId,
                        batchUpdateXml.getInf().getBid().getValue());                
            } else {
                BatchUpdateErrorXml batchUpdateErrorXml = (BatchUpdateErrorXml) batchUpdateResult;
                log.debug("Upload result error: {}(id: {}), batchId: {}: {}", externalSystem.getName(), externalSystemId,
                          batchUpdateXml.getInf().getBid().getValue(), 
                          batchUpdateErrorXml.getMessages());
            }
        }
        return batchUpdateResult;
    }

    /**
     * Synchronizace seznamu záznamů CAM -> ELZA
     *
     * Metoda je volána bez nastavení credentials, nastavují se dle
     * jednotlivého záznamu z fronty.
     *
     * @param externalSystemId
     * @param entityXmlMap
     * @param queueItems
     */
    @Transactional
    public void importEntities(Integer externalSystemId,
                               Map<String, EntityXml> entityXmlMap,
                               List<Integer> queueItemIds) {
        // All objects have to be fully initialized,
        // no HibernateProxy objects are allowed!!!
        // EntityManager.clear() is called inside synchronizeAccessPoint
        ApExternalSystem externalSystem = externalSystemService.getExternalSystemInternal(externalSystemId);
        ApScope scope = externalSystem.getScope();
        scope = HibernateUtils.unproxy(scope);

        ProcessingContext procCtx = new ProcessingContext(scope, externalSystem, staticDataService);
        for (Integer queueItemId: queueItemIds) {
            ExtSyncsQueueItem queueItem = extSyncsQueueItemRepository.getOne(queueItemId);

            // set authorization
            Integer userId = queueItem.getUserId();
            SecurityContext secCtx;
            if (userId != null) {
                secCtx = userService.createSecurityContext(userId);
            } else {
                //
                // TODO: find better solution for userId==null
                //       use admin in such cases
                //
                secCtx = userService.createSecurityContextSystem();
            }
            SecurityContextHolder.setContext(secCtx);

            ApBinding binding;
            if (queueItem.getAccessPointId() != null) {
                ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(queueItem.getAccessPoint(), externalSystem);
                if (bindingState == null) {
                    if (queueItem.getBinding() != null) {
                        // this is quite weird case
                        // accessPoint and binding is set but bindingState does not exists
                        // can it happened? May by we can treat this case as an error?
                        log.info("Synchronization request with accessPointId: {}, bindingId: {} without bindingState.",
                                 queueItem.getAccessPointId(),
                                 queueItem.getBindingId());

                        binding = queueItem.getBinding();
                    } else {
                        throw new BusinessException("Missing bindingState for accessPoint", BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("queueItemId", queueItem.getExtSyncsQueueItemId());
                    }
                } else {
                    binding = bindingState.getBinding();
                }
            } else {
                binding = queueItem.getBinding();
                if (binding == null) {
                    throw new BusinessException("Missing binding for queueItem", BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("queueItemId", queueItem.getExtSyncsQueueItemId());
                }
            }

            // find related xmlEntity
            EntityXml entity = entityXmlMap.get(binding.getValue());
            if (entity == null) {
                // if batch size = 1, then mark queue item as invalid and stop trying
                if (queueItemIds.size() == 1) {
                    log.error("Missing requested entity, binding: {}, queueItemId: {}", binding.getValue(), queueItem.getExtSyncsQueueItemId());
                    apConnectService.setQueueItemState(queueItem,
                                                       ExtAsyncQueueState.ERROR,
                                                       "Error: entity not found in ES, binding: " + binding.getValue());
                    return;
                } else {
                    throw new BusinessException("Missing requested entity, binding: " + binding.getValue(),
                            ExternalCode.RECORD_NOT_FOUND)
                                    .set("bindingValue", binding.getValue())
                                    .set("queueItemId", queueItem.getExtSyncsQueueItemId());
                }
            }

            try {
                ApAccessPoint ap = synchronizeAccessPoint(procCtx, binding, entity, true);
                if (ap != null && queueItem.getAccessPointId() == null) {
                    queueItem.setAccessPoint(ap);
                }
                apConnectService.setQueueItemState(queueItem,
                                                   ExtAsyncQueueState.IMPORT_OK,
                                                   "Synchronized: ES -> ELZA");
            } catch (SyncImpossibleException e) {
                log.error("Synchronized impossible, accessPointId: {}, camId: {}, queueItemId: {}", queueItem.getAccessPointId(), binding.getValue(),
                          queueItem.getExtSyncsQueueItemId(), e);
                apConnectService.setQueueItemState(queueItem,
                                                   ExtAsyncQueueState.ERROR,
                                                   "Error: synchronized impossible: ES -> ELZA, " + e.getMessage());
            }
        }
    }

//    @Transactional
//    public void resetSynchronization(String code) {
//        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(code);
//
//        ApBindingSync bindingSync = bindingSyncRepository.findByApExternalSystem(externalSystem);
//        if (bindingSync == null) {
//            // nothing to reset
//            return;
//        }
//        if (TRANSACTION_UUID.equals(bindingSync.getLastTransaction())) {
//            log.debug("Accesspoint synchronization is already set to initial transaction, externalSystemId: {}.",
//                      externalSystem.getExternalSystemId());
//            return;
//        }
//        log.info("Resettting accesspoint synchronization (externalSystemId: {}) transaction from: {} to: {}",
//                 externalSystem.getExternalSystemId(),
//                 bindingSync.getLastTransaction(),
//                 TRANSACTION_UUID);
//        bindingSync.setLastTransaction(TRANSACTION_UUID);
//        bindingSyncRepository.save(bindingSync);
//    }

    public StateApproval convertStateXmlToStateApproval(EntityRecordStateXml state) {
        switch (state) {
        case ERS_APPROVED:
            return StateApproval.APPROVED;
        case ERS_NEW:
            return StateApproval.NEW;
        default:
            throw new BusinessException("Entita nemá žádný odpovídající status v ELZA.", BaseCode.INVALID_STATE)
                .set("state", state);
        }
    }
}
