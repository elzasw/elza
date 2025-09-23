package cz.tacr.elza.cam.v2;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.cam.v2.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.v2.schema.cam.EntityXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.ProcessingContext;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RevisionService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import jakarta.validation.constraints.NotNull;

@Service("camServiceV2")
public class CamService {

    static private final Logger log = LoggerFactory.getLogger(CamService.class);

    @Autowired
    private ApAccessPointRepository apAccessPointRepository;
    
    @Autowired
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private AccessPointDataService apDataService;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    @Autowired
    private DataRecordRefRepository dataRecordRefRepository;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private AccessPointService accessPointService;

    @Autowired
    private AccessPointItemService accessPointItemService;

    @Autowired
    private AsyncRequestService asyncRequestService;

    @Autowired
    private PartService partService;

    @Autowired
    private AccessPointItemService apItemService;

    @Autowired
    private AccessPointCacheService accessPointCacheService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private RevisionService revisionService;

    /**
     * Příjem a vytváření AccessPoints z xml-dat
     * 
     * @param procCtx
     * @param entities
     * @return
     */
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
     * @throws  SyncImpossibleException
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx,
                                       @NotNull ApBinding binding,
                                       @NotNull EntityXml entity, boolean syncQueue) throws SyncImpossibleException {
    	Objects.requireNonNull(binding);
    	Objects.requireNonNull(entity);

        log.debug("Entity synchronization request, bindingId: {}, value: {}, revId: {}",
                  binding.getBindingId(), binding.getValue(), entity.getRevision().getRev().getValue());

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
            accessPoint = apAccessPointRepository.findAccessPointByUuid(entity.getEntityUuid().getValue());
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
                         entity.getEntityUuid().getValue(), accessPoint.getAccessPointId());
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
                                                                        entity.getState().name(),
                                                                        entity.getRevision().getRev().getValue(),
                                                                        entity.getRevision().getExternalUser().toString(),
                                                                        null, syncState,
                                                                        // We do not know yet prefPart and type
                                                                        // It is Ok for not synced AP
                                                                        null, null);
                // if async(syncQueue) -> has local changes -> mark as not synced
                if (syncQueue) {
                    accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
                    return;
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
                return;
            }
            if (!modifiedPartOrItem) {
                // check if any update is needed
                if (SyncState.SYNC_OK.equals(bindingState.getSyncOk()) &&
                        origBindingState != null &&
                        Objects.equals(origBindingState.getExtRevision(), entity.getRevision().getRev().getValue())) {
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
            if (entity.getState().equals(EntityRecordStateXml.ERS_NEW)
                    || entity.getState().equals(EntityRecordStateXml.ERS_APPROVED)) {

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

    @AuthMethod(permission = {UsrPermission.Permission.AP_EXTERNAL_WR})
    public void connectAccessPoint(final ApState state, 
    		                       final EntityXml entity,
                                   final ProcessingContext procCtx,
                                   final boolean replace) {
        ApChange apChange = apDataService.createChange(ApChange.Type.AP_UPDATE);
        procCtx.setApChange(apChange);

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApType type = sdp.getApTypeByCode(entity.getEntityType().getValue());

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
     * Vytvoreni novych propojeni (binding) pro vztahy
     *
     * @param dataRefList
     * @param procCtx
     */
	public void createBindingForRel(List<ReferencedEntities> dataRefList, ProcessingContext procCtx) {
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
        Validate.isTrue(referencedAp != null || refBinding != null, "Failed to prepare referenced record.");

        dataRecordRef.setRecord(referencedAp);
        dataRecordRef.setBinding(refBinding);
        dataRecordRefRepository.save(dataRecordRef);
    }

    private EntityDBDispatcher createEntityDBDispatcher() {
        return new EntityDBDispatcher(accessPointRepository,
                stateRepository,
                bindingRepository,
                bindingItemRepository,
                dataRecordRefRepository,
                externalSystemService,
                accessPointService,
                accessPointItemService,
                asyncRequestService,
                partService,
                accessPointCacheService,
                ruleService,
                this);
    }
}
