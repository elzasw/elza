package cz.tacr.elza.service.cam;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cz.tacr.cam.client.ApiException;
import cz.tacr.cam.schema.cam.BatchEntityRecordRevXml;
import cz.tacr.cam.schema.cam.BatchInfoXml;
import cz.tacr.cam.schema.cam.BatchUpdateResultXml;
import cz.tacr.cam.schema.cam.BatchUpdateSavedXml;
import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityRecordRevInfoXml;
import cz.tacr.cam.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.LongStringXml;
import cz.tacr.cam.schema.cam.UpdatesFromXml;
import cz.tacr.cam.schema.cam.UpdatesXml;
import cz.tacr.cam.schema.cam.UuidXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.connector.CamConnector;
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
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
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
import cz.tacr.elza.service.RevisionService;
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
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private ApStateRepository stateRepository;

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
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ExtSyncsQueueItemRepository extSyncsQueueItemRepository;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private RevisionService revisionService;

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
                itemRepository,
                this);
    }

    public List<ApState> createAccessPoints(final ProcessingContext procCtx,
                                            final List<EntityXml> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }

        ApChange apChange = procCtx.getApChange();
        if (apChange == null) {
            apChange = apDataService.createChange(ApChange.Type.AP_CREATE);
            procCtx.setApChange(apChange);
        }

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.createEntities(procCtx, entities);

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
        stateRepository.save(state);
        ApState stateNew = accessPointService.copyState(state, apChange);
        stateNew.setApType(type);
        stateNew.setStateApproval(ApState.StateApproval.NEW);
        stateNew = stateRepository.save(stateNew);

        EntityDBDispatcher ec = createEntityDBDispatcher();
        ec.connectEntity(procCtx, stateNew, entity, replace, false);
    }


    // method is synchronized with synchronizeAccessPointsForExternalSystem
    // only one of then can run due to manipulation with queue
    @AuthMethod(permission = { UsrPermission.Permission.AP_EXTERNAL_WR })
    synchronized public void disconnectAccessPoint(ApAccessPoint accessPoint, String externalSystemCode) {
        ApExternalSystem apExternalSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);

        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint,
                                                                                                apExternalSystem);
        ApBinding binding = bindingState.getBinding();
        // Odstraneni ze synchronizacni fronty
        int numDeleted = extSyncsQueueItemRepository.deleteByAccessPoint(accessPoint);
        numDeleted += extSyncsQueueItemRepository.deleteByBinding(binding);
        if (numDeleted > 0) {
            extSyncsQueueItemRepository.flush();
        }

        dataRecordRefRepository.disconnectBinding(binding);
        bindingItemRepository.deleteByBinding(binding);
        bindingStateRepository.deleteByBinding(binding);
        bindingRepository.delete(binding);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    /**
     * Vytvoreni novych propojeni (binding) pro vztahy
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
        log.debug("Creating binding for rel, dataId: {}, value: {}, extSystem: {}",
                  dataRecordRef.getDataId(), value, procCtx.getApExternalSystem().getCode());

        ApBinding refBinding = externalSystemService.findByValueAndExternalSystem(value,
                                                                                 procCtx.getApExternalSystem());

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
     */
    @Transactional
    public void updateBinding(ExtSyncsQueueItem extSyncsQueueItem,
                              BatchUpdateSavedXml batchUpdateSaved,
                              Map<Integer, String> itemUuidMap,
                              Map<Integer, String> partUuidMap,
                              Map<Integer, String> stateMap) {
        ApState state = accessPointService.getStateInternal(extSyncsQueueItem.getAccessPointId());
        ApAccessPoint accessPoint = state.getAccessPoint();
        ApExternalSystem apExternalSystem = externalSystemService.getExternalSystemInternal(extSyncsQueueItem.getExternalSystemId());

        BatchEntityRecordRevXml batchEntityRecordRev = batchUpdateSaved.getRevisions().get(0);

        String camApState = stateMap.get(extSyncsQueueItem.getAccessPointId());
        if(camApState==null) {
        	camApState = EntityRecordStateXml.ERS_NEW.toString();
        }

        ApChange change = apDataService.createChange(ApChange.Type.AP_SYNCH);
        ApBindingState bindingState = bindingStateRepository.findByAccessPointAndExternalSystem(accessPoint,
                                                                                                apExternalSystem);
        ApBinding binding;
        if(bindingState!=null) {
            binding = bindingState.getBinding();
            bindingState = externalSystemService.createBindingState(bindingState, change, camApState,
                                                                    batchEntityRecordRev.getRev().getValue(),
                                                                    bindingState.getExtUser(),
                                                                    bindingState.getExtReplacedBy(),
                                                                    SyncState.SYNC_OK,
                                                                    accessPoint.getPreferredPart(),
                                                                    state.getApType());
        } else {
            UsrUser user = userService.getUserInternal(extSyncsQueueItem.getUserId());
            String userName = user == null ? "admin" : user.getUsername();

            binding = externalSystemService.createApBinding(Long.toString(batchEntityRecordRev.getEid().getValue()),
                                                            apExternalSystem, true);
            bindingState = externalSystemService.createBindingState(binding, accessPoint, change, camApState,
                                                                    batchEntityRecordRev.getRev().getValue(),
                                                                    userName, null, SyncState.SYNC_OK,
                                                                    accessPoint.getPreferredPart(),
                                                                    state.getApType());
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

        setQueueItemState(extSyncsQueueItem, ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_OK, OffsetDateTime.now(), null);

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
        if(!ceb.build(partList, itemMap)) {
        	return null;
        }
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
        		this.externalSystemService,
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

    /**
     * Create batch info
     * @param externalSystem External system where to send data
     * @param user who to send data
     * @return
     */
    private BatchInfoXml createBatchInfo(ApExternalSystem externalSystem, UsrUser user) {
        BatchInfoXml batchInfo = new BatchInfoXml();
        batchInfo.setBatchUserInfo(new LongStringXml(createUserInfo(externalSystem.getUserInfo(), user)));
        batchInfo.setBid(new UuidXml(UUID.randomUUID().toString()));
        return batchInfo;
    }

    /**
     * Vytváření informací o uživateli na základě šablony
     *
     * @param userInfo šablona
     * @param user uživatel
     * @return
     */
    public String createUserInfo(String userInfo, UsrUser user) {
    	String userName;
    	String userId;
    	String prefName;
    	if(user==null) {
    		userName = "admin";
    		userId = "0";
    		prefName = "Admin";
    	} else {
    		userName = user.getUsername();
    		userId = Integer.toString(user.getUserId());
    		prefName = accessPointService.findPreferredPartDisplayName(user.getAccessPoint());
    	}
    	if(userInfo==null) {
    		return userName;
    	}

        return userInfo.replaceAll("%i", userId)
                .replaceAll("%u", userName)
                .replaceAll("%n", prefName);
    }

    /**
     * Regular entity synchronization
     *
     * @param code
     */
    @Transactional
    synchronized public void synchronizeAccessPointsForExternalSystem(final String code) {
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(code);

        ApBindingSync apBindingSync = bindingSyncRepository.findByApExternalSystem(externalSystem);
        if (apBindingSync == null) {
            apBindingSync = createApBindingSync(externalSystem);
        }

        String lastTransaction;

        try {
        	List<EntityRecordRevInfoXml> entityRecordRevInfoXmls;
            UpdatesFromXml updatesFromXml = camConnector.getUpdatesFrom(apBindingSync.getLastTransaction(), externalSystem);
            if (updatesFromXml.getUps() != null && CollectionUtils.isNotEmpty(updatesFromXml.getUps().getRevisions())) {
                entityRecordRevInfoXmls = updatesFromXml.getUps().getRevisions();
                lastTransaction = updatesFromXml.getInf().getTo().getValue();
            } else {
                entityRecordRevInfoXmls = new ArrayList<>();
                lastTransaction = updatesFromXml.getInf().getTo().getValue();
                int count = updatesFromXml.getInf().getCnt().getValue().intValue();
                log.debug("Total entity count for update: {}, last transaction: {}", count, lastTransaction);
                int page = 1;

                while (count > 0) {
                	log.debug("Requesting entity info, page: {}, pageSize: {}", page, PAGE_SIZE);

                    UpdatesXml updatesXml = camConnector.getUpdatesFromTo(apBindingSync.getLastTransaction(), lastTransaction, page, PAGE_SIZE, externalSystem);
                    entityRecordRevInfoXmls.addAll(updatesXml.getRevisions());

                    page++;
                    count = count - PAGE_SIZE;
                }
            }
            prepareApsForSync(externalSystem, entityRecordRevInfoXmls);
        } catch (ApiException e) {
        	if (e.getCode() == 404) {
        		// Transaction not found, check if autorestart is enabled
        		log.error("Transaction not found, transaction={}, resetting transaction.", apBindingSync.getLastTransaction(), e);
        		lastTransaction =  TRANSACTION_UUID;
        	} else {
        		throw prepareSystemException(e);
        	}
        }

        apBindingSync.setLastTransaction(lastTransaction);
        bindingSyncRepository.save(apBindingSync);

        // kontrola datové struktury
        if (checkDb) {
            entityManager.flush();
            accessPointService.checkConsistency();
        }
    }

    /**
     * Prepare entities for synchronization
     *
     * @param externalSystem
     * @param entityRecordRevInfoXmls entity info list
     */
    private void prepareApsForSync(ApExternalSystem externalSystem,
    		List<EntityRecordRevInfoXml> entityRecordRevInfoXmls) {
        if (CollectionUtils.isEmpty(entityRecordRevInfoXmls)) {
            return;
        }
        log.debug("Preparing APs for synchronization from external system, count: {}", entityRecordRevInfoXmls.size());

        // Prepare keys
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
            Validate.isTrue(prevInfo==null, "Record with same key already process, %s", id);
        }

        List<ApBinding> bindings = externalSystemService.findBindings(keyList, externalSystem);
        final Map<String, ApBinding> bindingMap = bindings.stream()
                .collect(Collectors.toMap(p -> p.getValue(), p -> p));

        Map<Integer, ApBindingState> bindingStateMap;
        if (bindings.size() > 0) {
            List<ApBindingState> bindingStateList = externalSystemService.findBindingStates(bindings);
            bindingStateMap = bindingStateList.stream()
                    .collect(Collectors.toMap(p -> p.getBindingId(), p -> p));
        } else {
            bindingStateMap = Collections.emptyMap();
        }

        int recNo = 0;

        UsrUser user = userService.getLoggedUser();
        List<ExtSyncsQueueItem> extSyncsQueueItems = new ArrayList<>();
        for (String recordCode : keyList) {
        	recNo++;
        	if(log.isDebugEnabled()) {
        		if(recNo%100 == 0) {
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
                ExtSyncsQueueItem extSyncsQueue = new ExtSyncsQueueItem();
                if (ap != null) {
                    extSyncsQueue.setAccessPoint(ap);
                    extSyncsQueue.setState(ExtAsyncQueueState.UPDATE);
                } else {
                    extSyncsQueue.setState(ExtAsyncQueueState.IMPORT_NEW);
                }
                extSyncsQueue.setBinding(binding);
                extSyncsQueue.setExternalSystem(externalSystem);
                extSyncsQueue.setDate(OffsetDateTime.now());
                extSyncsQueue.setUser(user);
                extSyncsQueueItems.add(extSyncsQueue);
            }
        }
        if (!extSyncsQueueItems.isEmpty()) {
            extSyncsQueueItemRepository.saveAll(extSyncsQueueItems);
            // flush all items to the DB
            extSyncsQueueItemRepository.flush();
        }
    	if(log.isDebugEnabled()) {
    		if(recNo%100 != 0) {
    			log.debug("Prepared records for sync: [{}-{}]", ((recNo+99)/100-1)*100+1, recNo);
    		}
    	}
        log.debug("APs prepared for synchronization from external system");
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
     *            při volání z fronty:
     *            - lokálně smazaná entita není obnovena (změna stavu)
     *
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx, ApState state,
                                       ApBindingState bindingState,
                                       ApBinding binding,
                                       EntityXml entity, boolean syncQueue) {

        if (binding == null) {
            Validate.notNull(bindingState);

            binding = bindingState.getBinding();
        }
        // Mozne stavy synchronizace
        // ApState | ApBindingState  | syncQueue
        // ---------------------------------------
        // null    | null            | false
        // null    | null            | true
        // ex      | null            | false -> vytvoreni bindingState
        // ex      | null            | true  -> vytvoreni bindingState
        // ex      | ex              | false
        // ex      | ex              | true
        log.debug("Entity synchronization request, bindingId: {}, value: {}, revId: {}, apState: {}, bindingState: {}",
                  binding.getBindingId(),
                  binding.getValue(), entity.getRevi().getRid().getValue(),
                  state, bindingState);

        ApChange apChange = null;
        ApBindingState origBindingState = bindingState;
        if (bindingState == null) {
            apChange = apDataService.createChange(ApChange.Type.AP_SYNCH);

            ApAccessPoint accessPoint;
            if (state == null) {
                // Kontrola na zalozeni nove entity
                // overeni existence UUID
                accessPoint = apAccessPointRepository.findAccessPointByUuid(entity.getEuid().getValue());
                if (accessPoint != null) {
                    // we can assign ap to the binding
                    log.warn("Entity with uuid:{} already exists (id={}), automatically connected with external entity",
                             entity.getEuid().getValue(), accessPoint.getAccessPointId());
                }
            } else {
                // special case when bindingState is null, binding exists and state is valid
                // new bindingState has to be created
                accessPoint = state.getAccessPoint();
                bindingState = bindingStateRepository.findLastByAccessPointAndExternalSystem(state.getAccessPoint(),
                                                                                             procCtx.getApExternalSystem());
                if (state.getDeleteChangeId() != null) {
                    // pokud state smazan && bindingState == null mohlo by jít o obnovení neplatné entity
                    state = accessPointService.copyState(state, apChange);
                }
            }

            if (bindingState == null && accessPoint != null) {
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
                    return;
                }
            }
        }

        // Pokud je state!=null, tak musi byt vzdy bindingState!=null
        if (state != null && bindingState != null) {
            if (state.getStateApproval().equals(StateApproval.TO_APPROVE)
                    || state.getStateApproval().equals(StateApproval.REV_PREPARED)) {
                if (syncQueue) {
                    if (!SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())) {
                        bindingState.setSyncOk(SyncState.NOT_SYNCED);
                        bindingStateRepository.save(bindingState);
                        accessPointCacheService.createApCachedAccessPoint(state.getAccessPointId());
                    }
                    return;
                } else {
                	throw new SystemException("Entitu v tomto stavu nelze aktualizovat z externího systému", BaseCode.INVALID_STATE)
                		.set("accessPointId", state.getAccessPointId())
                		.set("state", state.getStateApproval());
                }
            }

            // Nelze změnit stav archivní entity, která má revizi
            ApRevision revision = revisionService.findRevisionByState(state);
            boolean modifiedPartOrItem = hasModifiedPartOrItem(state, bindingState);

            // do not sync deleted aps, mark as not synced
            if (state.getDeleteChangeId() != null ||
            // check if synced or not
                    (syncQueue &&
                            (modifiedPartOrItem || revision != null ||
                                    SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())))) {
                if (!SyncState.NOT_SYNCED.equals(bindingState.getSyncOk())) {
                    bindingState.setSyncOk(SyncState.NOT_SYNCED);
                    bindingStateRepository.save(bindingState);
                    accessPointCacheService.createApCachedAccessPoint(state.getAccessPointId());
                }
                return;
            }
            if (!modifiedPartOrItem) {
                // check if any update is needed
                if (SyncState.SYNC_OK.equals(bindingState.getSyncOk()) &&
                        origBindingState != null &&
                        Objects.equals(origBindingState.getExtRevision(), entity.getRevi().getRid().toString())) {
                    // binding already exists and no local changes are detected
                    // -> nothing to synchronize -> return
                    return;
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
            }
        } else {
            ec.synchronizeAccessPoint(procCtx, state, bindingState, entity, syncQueue);
        }

        procCtx.setApChange(null);
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
        List<ApPart> partList = partService.findNewerPartsByAccessPoint(state.getAccessPoint(),
                                                                        bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(partList)) {
            return true;
        }
        List<ApItem> itemList = itemRepository.findNewerValidItemsByAccessPoint(state.getAccessPoint(), bindingState.getSyncChange().getChangeId());
        if (CollectionUtils.isNotEmpty(itemList)) {
            return true;
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
            xmlBuilder = createEntityUpdateBuilder(accessPoint, bindingState, entity,
                                                                           externalSystem);
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
            if (bindingState == null) {
                binding = bindingRepository.findById(queueItem.getBindingId()).get();
                bindingValue = binding.getValue();
            } else {
                bindingValue = bindingState.getBinding().getValue();
                binding = null;
            }
            scope = state.getScope();
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
            entity = camConnector.getEntity(bindingValue, externalSystem);
        } catch (ApiException e) {
            // if ApiException and code >=400 && <500 =-> it means we connected server and it is logical failure
            if (e.getCode() >= 400 && e.getCode() < 500) {
                setQueueItemState(queueItem,
                                  ExtSyncsQueueItem.ExtAsyncQueueState.ERROR,
                                  OffsetDateTime.now(),
                                  e.getMessage());
                log.error("Failed to synchronize items, code: {}, body: {}", e.getCode(), e.getResponseBody(), e);
                return true;
            } else {
                // other exception -> retry later
                setQueueItemState(queueItem,
                                  queueItem.getState(),
                                  OffsetDateTime.now(),
                                  e.getMessage());
                return false;
            }
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
                          ExtSyncsQueueItem.ExtAsyncQueueState.IMPORT_OK,
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

        EntitiesXml entities = camConnector.getEntities(bindingValues, externalSystem);

        importNew(externalSystem, entities, bindingMap);

        setQueueItemState(queueItems,
                         ExtSyncsQueueItem.ExtAsyncQueueState.IMPORT_OK,
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

    @AuthMethod(permission = {UsrPermission.Permission.AP_EXTERNAL_WR})
    public void createExtSyncsQueueItem(Integer accessPointId, String externalSystemCode) {
    	ApExternalSystem extSystem = externalSystemService.findApExternalSystemByCode(externalSystemCode);
        // check AP state
        ApState apState = accessPointService.getApState(accessPointId);
        switch(apState.getStateApproval()) {
        case APPROVED:
        	break;
        case NEW:
        case TO_AMEND:
        	// Kontrola pripustnosti stavu
        	if(extSystem.getPublishOnlyApproved()==null||
        		!extSystem.getPublishOnlyApproved()) {
        		// pokud neni omezeni definovano nebo neni nastaveno
        		//  -> lze publikovat
        		break;
        	}
        default:
        	throw new BusinessException("Entita v tomto stavu nemůže být předána do externího systému.", BaseCode.INVALID_STATE)
                .set("accessPointId", apState.getAccessPointId())
                .set("state", apState.getStateApproval());
        }

        ApAccessPoint accessPoint = apState.getAccessPoint();

        // check ext_sync_queue
        if (extSyncsQueueItemRepository.countByAccesPointAndExternalSystemAndState(accessPoint, extSystem, ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW) != 0) {
            throw new BusinessException("Entita již čeká na zpracování ve frontě.", BaseCode.INVALID_STATE)
                .set("accessPointId", apState.getAccessPointId())
                .set("externalSystemCode", externalSystemCode);
        }

        UsrUser user = userService.getLoggedUser();
        ExtSyncsQueueItem extSyncsQueueItem = createExtSyncsQueueItem(accessPoint, extSystem, null,
                ExtSyncsQueueItem.ExtAsyncQueueState.EXPORT_NEW, OffsetDateTime.now(), user);
        extSyncsQueueItemRepository.save(extSyncsQueueItem);
    }

    private ExtSyncsQueueItem createExtSyncsQueueItem(final ApAccessPoint accessPoint,
                                                     final ApExternalSystem apExternalSystem,
                                                     final String stateMessage,
                                                     final ExtSyncsQueueItem.ExtAsyncQueueState state,
                                                     final OffsetDateTime date,
                                                     final UsrUser user) {
        ExtSyncsQueueItem extSyncsQueueItem = new ExtSyncsQueueItem();
        extSyncsQueueItem.setAccessPoint(accessPoint);
        extSyncsQueueItem.setExternalSystem(apExternalSystem);
        extSyncsQueueItem.setStateMessage(stateMessage);
        extSyncsQueueItem.setState(state);
        extSyncsQueueItem.setDate(date);
        extSyncsQueueItem.setUser(user);
        return extSyncsQueueItem;
    }

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
