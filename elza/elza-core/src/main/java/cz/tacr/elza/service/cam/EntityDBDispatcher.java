package cz.tacr.elza.service.cam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.cam.schema.cam.CodeXml;
import cz.tacr.cam.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ItemBinaryXml;
import cz.tacr.cam.schema.cam.ItemBooleanXml;
import cz.tacr.cam.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.schema.cam.ItemEnumXml;
import cz.tacr.cam.schema.cam.ItemIntegerXml;
import cz.tacr.cam.schema.cam.ItemLinkXml;
import cz.tacr.cam.schema.cam.ItemStringXml;
import cz.tacr.cam.schema.cam.ItemUnitDateXml;
import cz.tacr.cam.schema.cam.ItemsXml;
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApState.StateApproval;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.DeletedItems;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.MultipleApChangeContext;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.cache.AccessPointCacheService;
import cz.tacr.elza.service.cam.ItemUpdates.ChangedBindedItem;

/**
 * Create or update entities
 *
 * Dispatcher is single threaded and can be used multiple times
 */
public class EntityDBDispatcher {

    final static Logger log = LoggerFactory.getLogger(EntityDBDispatcher.class);

    final static String SCHEMA_UNKNOWN = "UNKNOWN";

    static class EntityStatus {
        private final EntityXml entityXml;
        private ApState apState;

        public EntityStatus(final EntityXml entityXml) {
            this.entityXml = entityXml;
        }

        public EntityXml getEntityXml() {
            return entityXml;
        }

        public void setState(ApState aps) {
            this.apState = aps;
        }

        public ApState getState() {
            return apState;
        }
    }

    List<ApState> createdEntities = new ArrayList<>();

    /**
     * Mapping between partUuid and ApBindingItem
     * 
     * Valid during synchronization
     */
    Map<String, ApBindingItem> bindingPartLookup;

    Map<Integer, Map<String, ApBindingItem>> bindingItemsByPart;

    /**
     * Parts without binding
     */
    List<ApPart> partsWithoutBinding;

    final private ApAccessPointRepository accessPointRepository;

    final private ApStateRepository stateRepository;

    final private ApBindingRepository bindingRepository;

    final private ApBindingItemRepository bindingItemRepository;

    final private DataRecordRefRepository dataRecordRefRepository;

    final private ExternalSystemService externalSystemService;

    final private AccessPointService accessPointService;

    final private AccessPointItemService accessPointItemService;

    final private PartService partService;

    final private AsyncRequestService asyncRequestService;

    final private ApItemRepository itemRepository;

    //final private AccessPointDataService accessPointDataService;

    final private CamService camService;

    final private AccessPointCacheService accessPointCacheService;

    private ProcessingContext procCtx;

    // Newly created binding state for last processed entity
    private ApBindingState bindingState;

    public EntityDBDispatcher(final ApAccessPointRepository accessPointRepository,
                              final ApStateRepository stateRepository,
                              final ApBindingRepository bindingRepository,
                              final ApBindingItemRepository bindingItemRepository,
                              final DataRecordRefRepository dataRecordRefRepository,
                              final ExternalSystemService externalSystemService,
                              final AccessPointService accessPointService,
                              final AccessPointItemService accessPointItemService,
                              final AsyncRequestService asyncRequestService,
                              final PartService partService,
                              final AccessPointCacheService accessPointCacheService,
                              final ApItemRepository itemRepository,
                              final CamService camService) {
        this.accessPointRepository = accessPointRepository;
        this.stateRepository = stateRepository;
        this.bindingRepository = bindingRepository;
        this.bindingItemRepository = bindingItemRepository;
        this.dataRecordRefRepository = dataRecordRefRepository;
        this.externalSystemService = externalSystemService;
        this.accessPointService = accessPointService;
        this.accessPointItemService = accessPointItemService;
        this.asyncRequestService = asyncRequestService;
        this.camService = camService;
        this.partService = partService;
        this.accessPointCacheService = accessPointCacheService;
        this.itemRepository = itemRepository;
    }

    /**
     * Method to create entities
     * 
     * Method should not be called from async queues.
     * Method will fail if other entity with same key value exists
     * 
     * @param procCtx
     * @param entities
     */
    public void createEntities(ProcessingContext procCtx,
                               List<EntityXml> entities) {

        ApExternalSystem apExternalSystem = procCtx.getApExternalSystem();
        if (procCtx.getApChange() == null) {
            throw new BusinessException("Change not set", BaseCode.INVALID_STATE);
        }
        this.procCtx = procCtx;

        Function<EntityXml, ApBinding> prepareBinding;

        // prepare list of already used ids
        Map<String, EntityStatus> idEsMap = entities.stream()
                .collect(Collectors.toMap(e -> CamHelper.getEntityUuid(e),
                                          e -> new EntityStatus(e)));

        List<ApAccessPoint> existingAps = accessPointRepository.findApAccessPointsByUuids(idEsMap.keySet());
        if (existingAps.size() > 0) {
            List<ApState> apStates = stateRepository.findLastByAccessPoints(existingAps);
            Validate.isTrue(apStates.size() == existingAps.size(),
                            "Unexpected number of ApStates, count: %i, expected: %i",
                            apStates.size(), existingAps.size());
            apStates.forEach(aps -> idEsMap.get(aps.getAccessPoint().getUuid()).setState(aps));
        }

        // Read existing binding from DB
        // added all uuid for looking for by uuid 
        List<String> uuids = CamHelper.getEuids(entities);
        List<ApBinding> bindingList = bindingRepository.findByValuesAndExternalSystemType(uuids, ApExternalSystemType.CAM_UUID);

        switch (apExternalSystem.getType()) {
        case CAM:
        case CAM_COMPLETE:
            List<String> values = CamHelper.getEids(entities);
            prepareBinding = (entity) -> {
                String bindingValue = CamHelper.getEntityId(entity);
                ApBinding binding = procCtx.getBindingByValue(bindingValue);
                if (binding == null) {
                    // try to find by uuid
                    String srcUuid = CamHelper.getEntityUuid(entity);
                    binding = procCtx.getBindingByValue(srcUuid);
                    if (binding == null) {
                        binding = externalSystemService.createApBinding(bindingValue, apExternalSystem, true);
                        procCtx.addBinding(binding);
                    }
                }
                return binding;
            };

            List<ApBinding> bindingByValues = bindingRepository.findByValuesAndExternalSystem(values, apExternalSystem);
            if(CollectionUtils.isNotEmpty(bindingByValues)) {
                bindingList = new ArrayList<>(bindingList);
                bindingList.addAll(bindingByValues);
            }
            break;

        case CAM_UUID:
            prepareBinding = (entity) -> {
                String bindingValue = CamHelper.getEntityUuid(entity);
                ApBinding binding = procCtx.getBindingByValue(bindingValue);
                if (binding == null) {
                    binding = externalSystemService.createApBinding(bindingValue, apExternalSystem, true);
                    procCtx.addBinding(binding);
                }
                return binding;
            };
            break;

        default:
            throw new IllegalStateException("Unkonw external system type: " + apExternalSystem.getType());
        }

        procCtx.addBindings(bindingList);
        // get list of connected records
        List<ArrDataRecordRef> dataRecordRefList = dataRecordRefRepository.findByBindingIn(bindingList);

        for (EntityXml entity : entities) {
            ApBinding binding = prepareBinding.apply(entity);

            // prepare uuid - we are directly using uuid from external system
            String srcUuid = CamHelper.getEntityUuid(entity);

            ApState state;
            EntityStatus entityInfo = idEsMap.get(srcUuid);
            if (entityInfo != null && entityInfo.getState() != null) {
                // update entity if deleted
                state = entityInfo.getState();
                if (state.getDeleteChangeId() == null) {
                    throw new BusinessException("Accespoint already exists", ExternalCode.ALREADY_IMPORTED);
                }
                state = restoreAccessPoint(entity, binding, state.getAccessPoint(), false);
            } else {
                state = createAccessPoint(entity, binding, srcUuid, false);
            }
            accessPointService.publishAccessPointCreateEvent(state.getAccessPoint());
            createdEntities.add(state);
            accessPointService.setAccessPointInDataRecordRefs(state.getAccessPoint(), dataRecordRefList, binding);
        }

        dataRecordRefRepository.saveAll(dataRecordRefList);
        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            List<Integer> accessPointIds = ObjectListIterator.findIterable(dataRecordRefList,
                                                                           accessPointRepository::findAccessPointIdsByRefData);
            if (CollectionUtils.isNotEmpty(accessPointIds)) {
                ObjectListIterator.forEachPage(accessPointIds, accessPointRepository::updateToInit);
                asyncRequestService.enqueue(accessPointIds);
            }
        }

        this.procCtx = null;
    }

    public void connectEntity(ProcessingContext procCtx,
                              ApState state,
                              EntityXml entity, boolean replace,
                              boolean async) {
        Validate.notNull(procCtx.getApChange());

        this.procCtx = procCtx;

        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = procCtx.getApChange();

        if (replace) {
            partService.deleteParts(accessPoint, apChange);
        }

        ApBinding binding = externalSystemService.createApBinding(Long.toString(entity.getEid().getValue()),
                                                                  procCtx.getApExternalSystem(), true);

        createPartsFromEntityXml(entity, accessPoint, apChange, state, binding, async);

        accessPointService.publishAccessPointUpdateEvent(accessPoint);

        this.procCtx = null;
    }

    /**
     * Run existing AP sync
     * 
     * @param procCtx
     * @param state
     * @param prevBindingState
     * @param entity
     * @param syncQueue
     *            True if called from sync queue (without UI and direct user
     *            feedback)
     */
    public void synchronizeAccessPoint(ProcessingContext procCtx,
                                       ApState state,
                                       @Nonnull final ApBindingState prevBindingState,
                                       EntityXml entity,
                                       boolean syncQueue) {
        Validate.notNull(procCtx.getApChange());
        Validate.notNull(prevBindingState);


        this.procCtx = procCtx;

        
        // Flag if entity is deleted
        // Deleted entity has to be retained as deleted if 
        // synQueue is true.
        boolean deletedEntity = (state.getDeleteChangeId()!=null);
        ApState stateNew = null;

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApAccessPoint accessPoint = state.getAccessPoint();
        MultipleApChangeContext mcc = new MultipleApChangeContext(); 

        readBindingItems(prevBindingState.getBinding(), accessPoint);
        // check if exists subparts without binding
        // in such case we cannot run synchronization - subparts has to be resolve first
        if (syncQueue && CollectionUtils.isNotEmpty(partsWithoutBinding)) {
            for (ApPart partWithoutBinding : partsWithoutBinding) {
                if (partWithoutBinding.getParentPart() != null) {
                    // sub part without item and running in background
                    // -> sync failed
                    this.bindingState = externalSystemService.createBindingState(prevBindingState,
                                                                                 procCtx.getApChange(),
                                                                                 entity.getEns().value(),
                                                                                 entity.getRevi().getRid().getValue(),
                                                                                 entity.getRevi().getUsr().getValue(),
                                                                                 null,
                                                                                 SyncState.NOT_SYNCED,
                                                                                 accessPoint.getPreferredPart(),
                                                                                 state.getApType());
                    this.procCtx = null;
                    return;
                }
            }
        }

        // check s AP class/subclass was cha
        ApType apType = sdp.getApTypeByCode(entity.getEnt().getValue());
        if (!state.getApTypeId().equals(apType.getApTypeId())) {
            //změna třídy entity
            if(!deletedEntity) {
                state.setDeleteChange(procCtx.getApChange());
                state = stateRepository.save(state);
            }
            stateNew = accessPointService.copyState(state, procCtx.getApChange());
            if(deletedEntity&&syncQueue) {
                // retain deleted state
                stateNew.setDeleteChange(state.getDeleteChange());
            }
            stateNew.setApType(apType);
            state = stateRepository.save(stateNew);
        }

        String extReplacedBy = (entity.getReid() != null) ? Long.toString(entity.getReid().getValue()) : null;

        SynchronizationResult syncRes = synchronizeParts(procCtx, entity, prevBindingState.getBinding(), accessPoint, syncQueue);
        // při synchronizaci dochází ke změně objektu accessPoint, je nutné používat vrácený
        accessPoint = syncRes.getAccessPoint();
        //vytvoření nového stavu propojení
        this.bindingState = externalSystemService.createBindingState(prevBindingState, procCtx.getApChange(),
                                                                     entity.getEns().value(),
                                                                     entity.getRevi().getRid().getValue(),
                                                                     entity.getRevi().getUsr().getValue(),
                                                                     extReplacedBy,
                                                                     SyncState.SYNC_OK,
                                                                     accessPoint.getPreferredPart(),
                                                                     state.getApType());

        switch (entity.getEns()) {
        case ERS_REPLACED:
            // entita je nahrazena v CAM -> musíme nahradit v ELZA
            ApBinding binding = bindingRepository.findByValueAndExternalSystem(extReplacedBy, procCtx.getApExternalSystem());
            if (binding != null) {
                Optional<ApBindingState> replacedBindingState = externalSystemService.getBindingState(binding);
                if (replacedBindingState.isPresent()) {
                    ApAccessPoint replacedBy = replacedBindingState.get().getAccessPoint();
                    ApState replacementState = stateRepository.findLastByAccessPointId(replacedBy.getAccessPointId());
                    accessPointService.replace(state, replacementState, bindingState.getApExternalSystem(), mcc);
                    state.setReplacedBy(replacedBy);
                }
            }
            // delete AP
            state = accessPointService.deleteAccessPoint(state, accessPoint, procCtx.getApChange());
            break;

        case ERS_INVALID:
            // odstranění entity, která v CAM označena jako neplatná
            state = accessPointService.deleteAccessPoint(state, accessPoint, procCtx.getApChange());
            break;

        default:
            StateApproval newStateApproval = camService.convertStateXmlToStateApproval(entity.getEns());
            // synchronizace stavu entit
            // pokud je entita lokalne smazana a jedna se o pozadavek z fronty
            // musi entita zustat smazana
            if (syncQueue && state.getDeleteChangeId() != null) {
                break;
            } else {
                // kontrola shody stavu
                if (!newStateApproval.equals(state.getStateApproval())) {
                    if (stateNew == null) {
                        state.setDeleteChange(procCtx.getApChange());
                        state = stateRepository.save(state);
                        stateNew = accessPointService.copyState(state, procCtx.getApChange());
                    }
                    stateNew.setStateApproval(newStateApproval);
                    state = stateRepository.save(stateNew);
                }
            }

            break;
        }

        accessPointService.updateAndValidate(accessPoint, state, syncRes.getParts(), syncRes.getItemMap(), syncQueue);
        mcc.add(accessPoint.getAccessPointId());
        for (Integer apId : mcc.getModifiedApIds()) {
            accessPointCacheService.createApCachedAccessPoint(apId);
        }

        this.procCtx = null;
    }

    /**
     * Restore access point which was alreay deleted
     * 
     * @param procCtx
     * @param entity
     * @param binding
     * @param accessPoint
     * @param async
     * @return
     */
    private ApState restoreAccessPoint(EntityXml entity, ApBinding binding, ApAccessPoint accessPoint, boolean async) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        StateApproval state = camService.convertStateXmlToStateApproval(entity.getEns());
        accessPoint = accessPointService.saveWithLock(accessPoint);
        ApState apState = accessPointService.createAccessPointState(accessPoint, procCtx.getScope(), type, state, apChange);

        createPartsFromEntityXml(entity, accessPoint, apChange, apState, binding, async);

        return apState;
    }

    void createPartsFromEntityXml(
                                  final EntityXml entity,
                                  final ApAccessPoint accessPoint,
                                  final ApChange apChange,
                                  final ApState apState,
                                  final ApBinding binding,
                                  boolean async) {
        Validate.notNull(binding);

        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

        List<ReferencedEntities> dataRefList = new ArrayList<>();

        for (PartXml partXml : entity.getPrts().getList()) {
            ApPart parentPart = findParentPart(partXml, accessPoint, binding);
            ApBindingItem bindingPart = createPart(partXml, parentPart, accessPoint, binding);
            ApPart apPart = bindingPart.getPart();

            List<ApItem> itemList = createItems(partXml, apPart, apChange, binding, dataRefList);

            itemMap.put(apPart.getPartId(), itemList);
            partList.add(apPart);
        }

        camService.createBindingForRel(dataRefList, procCtx);

        ApPart prefPart = accessPointService.findPreferredPart(partList);
        accessPoint.setPreferredPart(prefPart);

        this.bindingState = externalSystemService.createBindingState(binding, accessPoint, apChange,
                                                 entity.getEns().value(),
                                                 entity.getRevi().getRid() != null ? entity.getRevi().getRid()
                                                         .getValue() : null,
                                                 entity.getRevi().getUsr() != null ? entity.getRevi().getUsr()
                                                         .getValue() : null,
                                                 entity.getReid() != null ? entity.getReid().getValue() : null,
                                                 SyncState.SYNC_OK,
                                                 prefPart,
                                                 apState.getApType());

        accessPointService.updateAndValidate(accessPoint, apState, partList, itemMap, async);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    public ApBindingState getBindingState() {
        return bindingState;
    }

    /**
     * Create new part from XML part
     * 
     * Part is without items
     * 
     * @param part
     * @param parentPart
     * @param accessPoint
     * @param binding
     * @return
     */
    private ApBindingItem createPart(PartXml part,
                                     ApPart parentPart,
                                     ApAccessPoint accessPoint,                              
                                     ApBinding binding) {
        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
        ApChange apChange = procCtx.getApChange();

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
        ApBindingItem bindingPart = externalSystemService.createApBindingItem(binding, apChange, part.getPid()
                .getValue(), apPart, null);

        return bindingPart;
    }

    /**
     * Find parent ApPart for PartXml
     * 
     * @param partXml
     * @param accessPoint
     * @param binding
     * @return
     */
    private ApPart findParentPart(PartXml partXml, ApAccessPoint accessPoint, ApBinding binding) {
        if (partXml.getPrnt() != null) {
            ApPart parentPart = accessPointService.findParentPart(binding, partXml.getPrnt().getValue());
            if (parentPart == null) {
                throw new SystemException("Missing parent part", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("parentValue", partXml.getPrnt().getValue())
                        .set("bindingId", binding.getBindingId())
                        .set("accessPointId", accessPoint.getAccessPointId());
            }
            return parentPart;
        }
        return null;
    }

    /**
     * Vytvoření nového ApState a ApAccessPoint
     *  
     * @param procCtx
     * @param entity 
     * @param binding
     * @return ApState
     */
    public ApState createAccessPoint(ProcessingContext procCtx, EntityXml entity, ApBinding binding, boolean async) {
        Validate.notNull(procCtx.getApChange());
        this.procCtx = procCtx;

        return createAccessPoint(entity, binding, entity.getEuid().getValue(), async);
    }

    private ApState createAccessPoint(final EntityXml entity, ApBinding binding, String uuid, boolean async) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        String apTypeCode = entity.getEnt().getValue();
        ApType type = sdp.getApTypeByCode(apTypeCode);
        if (type == null) {
        	Validate.notNull(type, "Invalid apTypeCode, value: %s, uuid: %s", apTypeCode, uuid);
        }
        StateApproval state = camService.convertStateXmlToStateApproval(entity.getEns());
        ApState apState = accessPointService.createAccessPoint(procCtx.getScope(), type, state, apChange, uuid);
        ApAccessPoint accessPoint = apState.getAccessPoint();        
        
        createPartsFromEntityXml(entity, accessPoint, apChange, apState, binding, async);

        // TODO kontrola a aktualizace odkazů arr_data_record_ref
        List<ApItem> items = itemRepository.findUnbindedItemByBinding(binding);
        for (ApItem item : items) {
            ArrDataRecordRef dataRef = (ArrDataRecordRef) item.getData();
            if (dataRef.getRecord() == null) {
                dataRef.setRecord(accessPoint);
                dataRecordRefRepository.save(dataRef);
                ApPart part = item.getPart();
                accessPointService.updatePartValue(apState, part);
                if (part.getParentPartId() != null) {
                    // Item was in some cases dettached proxy
                    // we have to fetch part from DB
                    ApPart parentPart = this.partService.getPart(part.getParentPartId());
                    Validate.notNull(parentPart, "Failed to read parent part, partId: ", part.getParentPartId());
                    accessPointService.updatePartValue(apState, parentPart);
                }
                accessPointCacheService.createApCachedAccessPoint(part.getAccessPointId());
            }
        }

        return apState;
    }

    public List<ApState> getApStates() {
        return createdEntities;
    }

    static class SynchronizationResult {
        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();
        private ApAccessPoint accessPoint;

        SynchronizationResult() {

        }

        public Map<Integer, List<ApItem>> getItemMap() {
            return itemMap;
        }

        List<ApPart> getParts() {
            return partList;
        }

        public void addPart(ApPart part) {
            partList.add(part);

        }

        public void addPartItems(ApPart part, List<ApItem> itemList) {
            itemMap.put(part.getPartId(), itemList);
        }

        public void setAccessPoint(ApAccessPoint saveAp) {
            this.accessPoint = saveAp;
        }

        public ApAccessPoint getAccessPoint() {
            return accessPoint;
        }
    }


    /**
     * Synchronizace částí přístupového bodu z externího systému
     * 
     * Metoda mění odkaz na aktuální podobu preferovaného označení.
     * Dochází k uložení entity, další metody by měly používat aktualizovanou
     * entitu.
     * 
     * Nedochazi k validaci entity.
     *
     * @param procCtx
     *            context
     * @param entity
     *            entita z externího systému
     * @param binding
     *            propojení s externím systémem
     * @param accessPoint
     *            přístupový bod
     * @param syncQueue
     *            True if called from sync queue (without UI and direct user
     *            feedback)
     */
    private SynchronizationResult synchronizeParts(final ProcessingContext procCtx,
                                                   final EntityXml entity,
                                                   final ApBinding binding,
                                                   ApAccessPoint accessPoint, boolean syncQueue) {
        log.debug("Synchronizing parts, accessPointId: {}, version: {}", accessPoint.getAccessPointId(), accessPoint.getVersion());

        Integer accessPointId = accessPoint.getAccessPointId();
        PartsXml partsXml = entity.getPrts();
        if (partsXml == null) {
            log.error("Element parts is empty, accessPointId: {}, entityUuid: {}",
                      accessPointId,
                      entity.getEuid().toString());
            throw new BusinessException("Element parts is empty, accessPointId: " + accessPoint.getAccessPointId(),
                    BaseCode.INVALID_STATE)
                            .set("accessPointId", accessPoint.getAccessPointId());
        }

        log.debug("Synchronizing parts, accessPointId: {}, number of parts: {}", accessPointId,
                  partsXml.getList().size());

        List<ApItem> itemsByAp = accessPointItemService.findItems(accessPoint);
        Map<Integer, List<ApItem>> itemsMap = itemsByAp.stream().collect(Collectors.groupingBy(ApItem::getPartId));

        ApChange apChange = procCtx.getApChange();

        /*        
        List<ApBindingItem> bindingParts = bindingItemRepository.findPartsByBinding(binding);
        List<ApBindingItem> newBindingParts = new ArrayList<>();
        Map<Integer, List<ApBindingItem>> bindingItemMap = bindingItemRepository.findItemsByBinding(binding).stream()
                .collect(Collectors.groupingBy(i -> i.getItem().getPartId()));
        */
        List<ReferencedEntities> dataRefList = new ArrayList<>();

        SynchronizationResult syncResult = new SynchronizationResult();

        ApPart preferredName = null;

        for (PartXml partXml : partsXml.getList()) {
            log.debug("Synchronizing part, accessPointId: {}, part uuid: {}, parent uuid: {}, type: {}",
                      accessPointId, partXml.getPid().getValue(),
                      partXml.getPrnt() != null ? partXml.getPrnt().getValue() : null,
                      partXml.getT());

            ApBindingItem partBinding = bindingPartLookup.remove(partXml.getPid().getValue());
            ApPart part;
            List<ApItem> itemList;
            if (partBinding != null) {
                log.debug("Part with required binding was found, updating existing binding, accessPointId: {}, bindingItemId: {}",
                          accessPointId,
                          partBinding.getBindingItemId());
                part = partBinding.getPart();
                // Binding found -> update
                itemList = updatePart(partXml, part, itemsMap.get(part.getPartId()), binding, dataRefList);
            } else {
                log.debug("Part with binding does not exists, creating new binding, accessPointId: {}", accessPointId);

                ApPart parentPart = findParentPart(partXml, accessPoint, binding);

                // check if exists same other part without binding                
                ReceivedPart receivedPart = findSamePartWithoutBinding(partXml, parentPart, itemsMap);

                // if the same part not found -> create part
                if (receivedPart == null) {
                    partBinding = createPart(partXml, parentPart, accessPoint, binding);
                    part = partBinding.getPart();
                    itemList = createItems(partXml, part, apChange, binding, dataRefList);
                } else {
                    part = receivedPart.getPart();
                    Map<Integer, ReceivedItem> itemMap = receivedPart.getItems().stream()
                            .collect(Collectors.toMap(i -> i.getItemId(), i -> i));
                    itemList = itemsMap.get(part.getPartId());
                    // create bindings for item(s)
                    for (ApItem item : itemList) {
                        externalSystemService.createApBindingItem(binding, apChange, itemMap.get(item.getItemId()).getUuid(), null, item);
                    }
                    // create binding for found part
                    externalSystemService.createApBindingItem(binding, apChange, partXml.getPid().getValue(), part, null);
                }
            }
            syncResult.addPartItems(part, itemList);
            syncResult.addPart(part);

            if (preferredName == null) {
                if (StaticDataProvider.DEFAULT_PART_TYPE.equals(part.getPartType().getCode())) {
                    preferredName = part;
                }
            }
        }
        
        // smazání partů dle externího systému
        // mažou se zbývající party
        deletePartsInLookup(apChange, accessPoint, syncQueue);

        // smazání zbývajících nezpracovaných item
        Collection<ApBindingItem> remainingBindingItems = bindingItemsByPart.values().stream()
                .flatMap(m -> m.values().stream())
                .collect(Collectors.toCollection(ArrayList::new));
        if (remainingBindingItems.size() > 0) {
            List<ApItem> items = remainingBindingItems.stream().map(ApBindingItem::getItem)
                    .collect(Collectors.toList());
            deleteItems(items, apChange);
        }

        // delete empty map(s) from bindingItemsByPart
        Iterator<Map.Entry<Integer, Map<String, ApBindingItem>>> iterator = bindingItemsByPart.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Map<String, ApBindingItem>> entry = iterator.next();
            if (entry.getValue().isEmpty()) { 
                iterator.remove();
            }
        }

        if (bindingItemsByPart.size() > 0) {
            log.error("Exists unresolved bindings (items), accessPointId: {}, partIds: {}",
                      accessPoint.getAccessPointId(),
                      bindingItemsByPart.keySet());
            throw new BusinessException("Exists unresolved bindings, accessPointId: " + accessPointId + ", count: " + bindingItemsByPart.size(),
                    BaseCode.DB_INTEGRITY_PROBLEM).set("accessPointId", accessPointId);
        }
        if (bindingPartLookup.size() > 0) {
            log.error("Exists unresolved bindings (parts), accessPointId: {}, items: {}",
                      accessPointId,
                      bindingPartLookup.keySet());
            throw new BusinessException("Exists unresolved bindings (parts), accessPointId: " +
                    accessPoint.getAccessPointId() + ", count: " +
                    bindingPartLookup.size(), BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("accessPointId", accessPointId);
        }

        //nastavení odkazů na entitu
        camService.createBindingForRel(dataRefList, procCtx);

        //změna preferováného jména
        Validate.notNull(preferredName, "Missing preferredName");
        accessPoint = accessPointService.setPreferName(accessPoint, preferredName);        
        syncResult.setAccessPoint(accessPoint);

        log.debug("Parts were updated, accessPointId: {}, version: {}",
                  syncResult.getAccessPoint().getAccessPointId(),
                  syncResult.getAccessPoint().getVersion());

        bindingPartLookup = null;
        bindingItemsByPart = null;

        return syncResult;
    }

    /**
     * Najít odpovídající ApPart v seznamu
     * 
     * @param partXml
     * @param parentPart
     * @param itemsMap
     * @return
     */
    private ReceivedPart findSamePartWithoutBinding(PartXml partXml, ApPart parentPart, Map<Integer, List<ApItem>> itemsMap) {
        List<ReferencedEntities> dataRefList = new ArrayList<>();
        List<ReceivedItem> itemsFromXml;
        ItemsXml itms = partXml.getItms();
        if (itms != null) {
            itemsFromXml = itms.getItems().stream().map(i -> createReveivedItem(i, dataRefList)).collect(Collectors.toList());
        } else {
            itemsFromXml  = Collections.emptyList();
        }

        for (ApPart part : partsWithoutBinding) {
            if (comparePart(partXml, itemsFromXml, parentPart, part, itemsMap.get(part.getPartId()))) {
                return new ReceivedPart(part, itemsFromXml);
            }
        }
        return null;
    }

    /**
     * Porovnání PartXml z ApPart včetně ApItem(s)
     * 
     * @param partXml
     * @param itemsXml
     * @param parentPart
     * @param part
     * @param items
     * @return
     */
    private boolean comparePart(final PartXml partXml, final List<ReceivedItem> itemsXml, final ApPart parentPart, final ApPart part, final List<ApItem> items) {
        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        RulPartType partType = sdp.getPartTypeByCode(partXml.getT().value());

        // compare partType
        if (!partType.getPartTypeId().equals(part.getPartTypeId())) {
            return false;
        }

        // check on the parentPart
        if (parentPart == null) {
            if (part.getParentPart() != null) {
                return false;
            }
        } else {
            if (!parentPart.getPartId().equals(part.getParentPartId())) {
                return false;
            }
        }

        // compare items
        return compareItems(itemsXml, items);
    }

    /**
     * Porovnání seznamu items PartXml se sezmanen ApItem
     * 
     * @param itemsXml
     * @param items
     * @return
     */
    private boolean compareItems(final List<ReceivedItem> itemsXml, final List<ApItem> items) {
        if (itemsXml.size() != items.size()) {
            return false;
        }

        for (ReceivedItem item : itemsXml) {
            if (!item.contains(items)) {
                return false;
            }
        }
        return true;
    }

    private void deletePartsInLookup(ApChange apChange, ApAccessPoint accessPoint, boolean syncQueue) {
        if (bindingPartLookup.isEmpty()) {
            return;
        }

        Collection<ApBindingItem> partsBinding = bindingPartLookup.values();

        // získání seznamu podřízených ApPart
        List<ApPart> parts = partService.findPartsByAccessPoint(accessPoint);
        List<ApPart> subParts = parts.stream().filter(p -> p.getParentPartId() != null).collect(Collectors.toList());

        List<ApPart> partList = new ArrayList<>();
        for (ApBindingItem partBinding : partsBinding) {
            ApPart part = partBinding.getPart();
            partList.add(part);
            partBinding.setDeleteChange(apChange);
            log.debug("Deleting part binding, bindingItemId: {}, partId: {}", partBinding.getBindingItemId(), part.getPartId());
        }
        bindingItemRepository.saveAll(partsBinding);
        bindingItemRepository.flush();

        // získání seznamu ID, která odstraníme
        Set<Integer> deletedPartIds = partList.stream().map(p -> p.getPartId()).collect(Collectors.toSet());

        for (ApPart subPart : subParts) {
            if (subPart.getParentPartId() != null
                    && deletedPartIds.contains(subPart.getParentPartId())
                    && !deletedPartIds.contains(subPart.getPartId())) {
                if (syncQueue) {
                    log.error("Removed part has subordinate part(s), accessPointId: {}, partId: {}", 
                              accessPoint.getAccessPointId(), 
                              subPart.getParentPartId());
                    throw new BusinessException("Removed part has subordinate part(s), accessPointId: " + 
                              accessPoint.getAccessPointId() + ", partId: " + subPart.getParentPartId(), BaseCode.EXPORT_FAILED)
                        .set("accessPointId", accessPoint.getAccessPointId())
                        .set("partId", subPart.getParentPartId());
                } else {
                    // pokud pochází z uživatelského rozhraní - musi odstranit i subPart
                    partList.add(subPart);
                }
            }
        }

        // clear lookup
        bindingPartLookup.clear();

        List<ApItem> items = accessPointItemService.findItemsByParts(partList);
        deleteItems(items, apChange);

        partService.deleteParts(partList, apChange);
    }

    private void deleteItems(List<ApItem> items, ApChange apChange) {
        // delete items in parts
        DeletedItems deletedItems = accessPointItemService.deleteItems(items, apChange);

        // delete bindings from bindingItemsByPart
        for (ApBindingItem bindingItem : deletedItems.getBindings()) {
            for (Integer partId : bindingItemsByPart.keySet()) {
                Map<String, ApBindingItem> bindingItemsPart = bindingItemsByPart.get(partId);
                bindingItemsPart.remove(bindingItem.getValue());
            }
        }
    }

    /**
     * Update part
     * 
     * Return list of items in part
     * 
     * @param partXml
     * @param apPart
     * @param srcItems
     * @param binding
     * @param dataRefList
     * @return
     */
    private List<ApItem> updatePart(PartXml partXml, ApPart apPart, List<ApItem> srcItems,
                                    ApBinding binding, List<ReferencedEntities> dataRefList) {

        List<Object> itemsXml;

        if (partXml.getItms() != null) {
            itemsXml = partXml.getItms().getItems();
        } else {
            itemsXml = Collections.emptyList();
        }

        Map<Integer, ApItem> srcItemsMap = new HashMap<>();
        if (srcItems != null) {
            srcItemsMap = srcItems.stream().collect(Collectors.toMap(i -> i.getItemId(), i -> i));
        }

        ItemUpdates itemUpdates = findNewOrChangedItems(apPart, itemsXml);
        Validate.notNull(itemUpdates);

        List<ApItem> result = new ArrayList<>(itemUpdates.getItemCount());
        // remove unchanged items from binding lookup and add to result
        for (ApBindingItem notChangeItem : itemUpdates.getNotChangeItems()) {
            Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.get(apPart.getPartId());
            ApBindingItem removedItem = bindingItemLookup.remove(notChangeItem.getValue());
            if (removedItem == null) {
                throw new SystemException("Missing item in lookup.").set("missingValue", notChangeItem.getValue());
            }
            result.add(removedItem.getItem());
            srcItemsMap.remove(removedItem.getItem().getItemId());
        }

        List<ChangedBindedItem> changedItems = itemUpdates.getChangedItems();
        if (CollectionUtils.isNotEmpty(changedItems)) {
            // drop old bindings
            List<ApBindingItem> bindedItems = changedItems.stream().map(ChangedBindedItem::getBindingItem)
                    .collect(Collectors.toList());
            deleteBindedItems(apPart, bindedItems, procCtx.getApChange());

            List<Object> xmlItems = changedItems.stream().map(ChangedBindedItem::getXmlItem)
                    .collect(Collectors.toList());
            // import changed items
            result.addAll(createItems(xmlItems, apPart, procCtx.getApChange(), binding, dataRefList));
            // remove processed items from srcItemMap
            for (ChangedBindedItem changedItem : itemUpdates.getChangedItems()) {
                srcItemsMap.remove(changedItem.getBindingItem().getItemId());
            }
        }

        // check last items with binding in srcItemMap
        // if item has binding -> remove it from srcItemsMap 
        Map<String, ApBindingItem> bindingItemsPart = bindingItemsByPart.get(apPart.getPartId());
        if (bindingItemsPart != null) {
            for (ApBindingItem bindingItem : bindingItemsPart.values()) {
                srcItemsMap.remove(bindingItem.getItemId());
            }
        }

        // added all items without binding
        result.addAll(srcItemsMap.values());

        List<Object> newItems = itemUpdates.getNewItems();
        if (CollectionUtils.isNotEmpty(newItems)) {
            result.addAll(createItems(newItems, apPart, procCtx.getApChange(), binding, dataRefList));
        }
        return result;
    }

    private void deleteBindedItems(ApPart part, List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isEmpty(bindingItemsInPart)) {
            return;
        }
        Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.get(part.getPartId());
        Validate.notNull(bindingItemLookup);
        for (ApBindingItem bindingItem : bindingItemsInPart) {
            bindingItemLookup.remove(bindingItem.getValue());
        }

        accessPointItemService.deleteBindnedItems(bindingItemsInPart, apChange);
    }

    public List<ApItem> createItems(final PartXml partXml,
                                    final ApPart apPart, final ApChange change,
                                    final ApBinding binding,
                                    final List<ReferencedEntities> dataRefList) {
        
        if (partXml.getItms() == null) {
            return Collections.emptyList();
        }
        return createItems(partXml.getItms().getItems(), apPart, change, binding, dataRefList);
    }

    public List<ApItem> createItems(final List<Object> createItems,
                                    final ApPart apPart, final ApChange change,
                                    final ApBinding binding,
                                    final List<ReferencedEntities> dataRefList) {
        List<ApItem> itemsCreated = new ArrayList<>(createItems.size());
        Map<Integer, List<ApItem>> typeIdItemsMap = new HashMap<>();

        for (Object createItem : createItems) {
            ApItem itemCreated = createItem(apPart, createItem, change, typeIdItemsMap, binding, dataRefList);
            itemsCreated.add(itemCreated);
        }
        return itemsCreated;
    }

    private ApItem createItem(final ApPart part,
                              final Object createItem,
                              final ApChange change,
                              final Map<Integer, List<ApItem>> typeIdItemsMap,
                              final ApBinding binding,
                              final List<ReferencedEntities> dataRefList) {
        ReceivedItem receivedItem = createReveivedItem(createItem, dataRefList);  
        RulItemType itemType = receivedItem.getItemType();
        RulItemSpec itemSpec = receivedItem.getItemSpec();
        String uuid = receivedItem.getUuid();
        ArrData data = receivedItem.getData();

        List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

        ApItem itemCreated = accessPointItemService.createItemWithSave(part, data, itemType, itemSpec, change,
                                                                       existsItems,
                                                                       binding, uuid);
        existsItems.add(itemCreated);
        return itemCreated;
    }

    private ReceivedItem createReveivedItem(final Object createItem, final List<ReferencedEntities> dataRefList) {
        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        RulItemType itemType;
        RulItemSpec itemSpec;
        String uuid;
        ArrData data;
        if (createItem instanceof ItemBinaryXml) {
            ItemBinaryXml itemBinary = (ItemBinaryXml) createItem;

            itemType = sdp.getItemType(itemBinary.getT().getValue());
            itemSpec = itemBinary.getS() == null ? null : sdp.getItemSpec(itemBinary.getS().getValue());
            uuid = CamHelper.getUuid(itemBinary.getUuid());

            ArrDataCoordinates dataCoordinates = new ArrDataCoordinates();
            dataCoordinates.setValue(GeometryConvertor.convertWkb(itemBinary.getValue().getValue()));
            dataCoordinates.setDataType(DataType.COORDINATES.getEntity());
            data = dataCoordinates;
        } else if (createItem instanceof ItemBooleanXml) {
            ItemBooleanXml itemBoolean = (ItemBooleanXml) createItem;

            itemType = sdp.getItemType(itemBoolean.getT().getValue());
            itemSpec = itemBoolean.getS() == null ? null : sdp.getItemSpec(itemBoolean.getS().getValue());
            uuid = CamHelper.getUuid(itemBoolean.getUuid());

            ArrDataBit dataBit = new ArrDataBit();
            dataBit.setBitValue(itemBoolean.getValue().isValue());
            dataBit.setDataType(DataType.BIT.getEntity());
            data = dataBit;
        } else if (createItem instanceof ItemEntityRefXml) {
            ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) createItem;

            itemType = sdp.getItemType(itemEntityRef.getT().getValue());
            itemSpec = itemEntityRef.getS() == null ? null : sdp.getItemSpec(itemEntityRef.getS().getValue());
            uuid = CamHelper.getUuid(itemEntityRef.getUuid());

            ArrDataRecordRef dataRecordRef = new ArrDataRecordRef();
            dataRecordRef.setDataType(DataType.RECORD_REF.getEntity());

            String extIdent = CamHelper.getEntityIdorUuid(itemEntityRef);
            ReferencedEntities dataRef = new ReferencedEntities(dataRecordRef, extIdent);
            dataRefList.add(dataRef);
            
            data = dataRecordRef;
        } else if (createItem instanceof ItemEnumXml) {
            ItemEnumXml itemEnum = (ItemEnumXml) createItem;

            itemType = sdp.getItemType(itemEnum.getT().getValue());
            itemSpec = itemEnum.getS() == null ? null : sdp.getItemSpec(itemEnum.getS().getValue());
            uuid = CamHelper.getUuid(itemEnum.getUuid());

            ArrDataNull dataNull = new ArrDataNull();
            dataNull.setDataType(DataType.ENUM.getEntity());
            data = dataNull;
        } else if (createItem instanceof ItemIntegerXml) {
            ItemIntegerXml itemInteger = (ItemIntegerXml) createItem;

            itemType = sdp.getItemType(itemInteger.getT().getValue());
            itemSpec = itemInteger.getS() == null ? null : sdp.getItemSpec(itemInteger.getS().getValue());
            uuid = CamHelper.getUuid(itemInteger.getUuid());

            ArrDataInteger dataInteger = new ArrDataInteger();
            dataInteger.setIntegerValue(itemInteger.getValue().getValue().intValue());
            dataInteger.setDataType(DataType.INT.getEntity());
            data = dataInteger;
        } else if (createItem instanceof ItemLinkXml) {
            ItemLinkXml itemLink = (ItemLinkXml) createItem;

            itemType = sdp.getItemType(itemLink.getT().getValue());
            itemSpec = itemLink.getS() == null ? null : sdp.getItemSpec(itemLink.getS().getValue());
            uuid = CamHelper.getUuid(itemLink.getUuid());

            ArrDataUriRef dataUriRef = new ArrDataUriRef();
            dataUriRef.setUriRefValue(itemLink.getUrl().getValue());
            dataUriRef.setDescription(itemLink.getNm().getValue());
            String schema = ArrDataUriRef.createSchema(itemLink.getUrl().getValue());
            if (schema == null) {
                log.info("Schema URL: {} is null, will be set {}", itemLink.getUrl().getValue(), SCHEMA_UNKNOWN);
                schema = SCHEMA_UNKNOWN;
            }
            dataUriRef.setSchema(schema);
            dataUriRef.setArrNode(null);
            dataUriRef.setDataType(DataType.URI_REF.getEntity());
            data = dataUriRef;
        } else if (createItem instanceof ItemStringXml) {
            ItemStringXml itemString = (ItemStringXml) createItem;

            itemType = sdp.getItemType(itemString.getT().getValue());
            itemSpec = itemString.getS() == null ? null : sdp.getItemSpec(itemString.getS().getValue());
            uuid = CamHelper.getUuid(itemString.getUuid());

            RulDataType dataType = itemType.getDataType();
            String code = dataType.getCode();
            DataType dt = DataType.fromCode(code);
            if (dt == null) {
                throw new IllegalStateException("Neznámý datový typ " + code);
            }
            switch (dt) {
            case STRING:
                ArrDataString dataString = new ArrDataString();
                dataString.setStringValue(itemString.getValue().getValue());
                dataString.setDataType(DataType.STRING.getEntity());
                data = dataString;
                break;
            case TEXT:
                ArrDataText dataText = new ArrDataText();
                dataText.setTextValue(itemString.getValue().getValue());
                dataText.setDataType(DataType.TEXT.getEntity());
                data = dataText;
                break;
            case COORDINATES:
                ArrDataCoordinates dataCoordinates = new ArrDataCoordinates();
                dataCoordinates.setValue(GeometryConvertor.convert(itemString.getValue().getValue()));
                dataCoordinates.setDataType(DataType.COORDINATES.getEntity());
                data = dataCoordinates;
                break;
            default:
                throw new IllegalStateException("Nepodporovaný datový typ uložen jako řetězec: " + code +
                        ", itemType:" + itemString.getT().getValue());
            }

        } else if (createItem instanceof ItemUnitDateXml) {
            ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) createItem;

            itemType = sdp.getItemType(itemUnitDate.getT().getValue());
            itemSpec = itemUnitDate.getS() == null ? null : sdp.getItemSpec(itemUnitDate.getS().getValue());
            uuid = CamHelper.getUuid(itemUnitDate.getUuid());

            ArrDataUnitdate dataUnitDate = new ArrDataUnitdate();
            dataUnitDate.setValueFrom(itemUnitDate.getF().trim());
            dataUnitDate.setValueFromEstimated(itemUnitDate.isFe());
            if (dataUnitDate.getValueFromEstimated() == null) {
                dataUnitDate.setValueFromEstimated(false);
            }
            dataUnitDate.setFormat(itemUnitDate.getFmt());
            dataUnitDate.setValueTo(itemUnitDate.getTo().trim());
            dataUnitDate.setValueToEstimated(itemUnitDate.isToe());
            if (dataUnitDate.getValueToEstimated() == null) {
                dataUnitDate.setValueToEstimated(false);
            }
            if (itemUnitDate.getF() != null) {
                dataUnitDate.setNormalizedFrom(CalendarConverter.toSeconds(LocalDateTime.parse(itemUnitDate
                        .getF().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedFrom(Long.MIN_VALUE);
            }

            if (itemUnitDate.getTo() != null) {
                dataUnitDate.setNormalizedTo(CalendarConverter.toSeconds(LocalDateTime.parse(itemUnitDate
                        .getTo().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedTo(Long.MAX_VALUE);
            }

            dataUnitDate.setDataType(DataType.UNITDATE.getEntity());
            data = dataUnitDate;
        } else {
            throw new IllegalArgumentException("Invalid item type");
        }

        // check specification (if correctly used)
        Boolean useSpec = itemType.getUseSpecification();
        if (useSpec != null && useSpec) {
            if (itemSpec == null) {
                throw new BusinessException("Received item without specification, itemType: " + itemType.getName(),
                        BaseCode.PROPERTY_IS_INVALID)
                                .set("itemType", itemType.getCode())
                                .set("itemTypeName", itemType.getName());
            }
        } else {
            if (itemSpec != null) {
                throw new BusinessException("Received item with unexpected specification, itemType: " + itemType
                        .getName(), BaseCode.PROPERTY_IS_INVALID)
                                .set("itemType", itemType.getCode())
                                .set("itemTypeName", itemType.getName());
            }
        }

        return new ReceivedItem(itemType, itemSpec, uuid, data);
    }

    private void readBindingItems(ApBinding binding, ApAccessPoint accessPoint) {
        List<ApBindingItem> bindingItems = this.externalSystemService.getBindingItems(binding);

        Map<Integer, ApBindingItem> partIdBindingMap = new HashMap<>();
        bindingPartLookup = new HashMap<>();        
        bindingItemsByPart = new HashMap<>();
        
        partsWithoutBinding = partService.findPartsByAccessPoint(accessPoint);

        for (ApBindingItem bindingItem : bindingItems) {
            if (bindingItem.getPart() != null) {
                bindingPartLookup.put(bindingItem.getValue(), bindingItem);
                partIdBindingMap.put(bindingItem.getPart().getPartId(), bindingItem);
                partsWithoutBinding.remove(bindingItem.getPart());
            } else if (bindingItem.getItem() != null) {
                Integer partId = bindingItem.getItem().getPartId();

                Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.computeIfAbsent(partId, id -> new HashMap<>());
                bindingItemLookup.put(bindingItem.getValue(), bindingItem);
            } else {
                throw new IllegalStateException();
            }
        }

        // safety check 
        // binded item should belong to some part in lookup
        for (Integer partId: bindingItemsByPart.keySet()) {
            ApBindingItem parentBinding = partIdBindingMap.get(partId);
            if (parentBinding == null) {
                log.error("Item with binding, but part is not binded, partId: {}", partId);
                throw new SystemException("Item with binding, but part is not binded",
                        BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("partId", partId);
            }
        }

    }

    /**
     * Try to map received items to existing items
     * @param part
     * @param items
     * @return
     */
    public ItemUpdates findNewOrChangedItems(ApPart part, List<Object> items) {
        Map<String, ApBindingItem> bindingItemLookup = bindingItemsByPart.getOrDefault(part.getPartId(), Collections.emptyMap());

    	ItemUpdates result = new ItemUpdates();
        for (Object item : items) {
            if (item instanceof ItemBinaryXml) {
                ItemBinaryXml itemBinary = (ItemBinaryXml) item;                
                prepareBinaryUpdate(bindingItemLookup, itemBinary, result);                
            } else if (item instanceof ItemBooleanXml) {
                ItemBooleanXml itemBoolean = (ItemBooleanXml) item;
                prepareBooleanUpdate(bindingItemLookup, itemBoolean, result);
            } else if (item instanceof ItemEntityRefXml) {
                ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) item;
                prepareEntityRefUpdate(bindingItemLookup, itemEntityRef, result);
            } else if (item instanceof ItemEnumXml) {
                ItemEnumXml itemEnum = (ItemEnumXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemEnum.getUuid().getValue());

                if (bindingItem == null) {
                	result.addNewItem(itemEnum);
                } else {
                    ApItem ie = bindingItem.getItem();
                    if (!(ie.getItemType().getCode().equals(itemEnum.getT().getValue()) &&
                            compareItemSpec(ie.getItemSpec(), itemEnum.getS()))) {

                    	result.addChanged(bindingItem, itemEnum);
                    } else {
                    	result.addNotChanged(bindingItem);
                    }
                }
            } else if (item instanceof ItemIntegerXml) {
                ItemIntegerXml itemInteger = (ItemIntegerXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemInteger.getUuid().getValue());

                if (bindingItem == null) {
                	result.addNewItem(itemInteger);
                } else {
                    ApItem ii = bindingItem.getItem();
                    ArrDataInteger dataInteger = (ArrDataInteger) ii.getData();
                    if (!(ii.getItemType().getCode().equals(itemInteger.getT().getValue()) &&
                            compareItemSpec(ii.getItemSpec(), itemInteger.getS()) &&
                            dataInteger.getIntegerValue().equals(itemInteger.getValue().getValue().intValue()))) {

                    	result.addChanged(bindingItem, itemInteger);
                    } else {
                    	result.addNotChanged(bindingItem);
                    }
                }
            } else if (item instanceof ItemLinkXml) {
                ItemLinkXml itemLink = (ItemLinkXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemLink.getUuid().getValue());

                if (bindingItem == null) {
                	result.addNewItem(itemLink);
                } else {
                    ApItem il = bindingItem.getItem();
                    ArrDataUriRef dataUriRef = (ArrDataUriRef) il.getData();
                    if (!(il.getItemType().getCode().equals(itemLink.getT().getValue()) &&
                            compareItemSpec(il.getItemSpec(), itemLink.getS()) &&
                            dataUriRef.getUriRefValue().equals(itemLink.getUrl().getValue()) &&
                            dataUriRef.getDescription().equals(itemLink.getNm().getValue()))) {

                    	result.addChanged(bindingItem, itemLink);
                    } else {
                    	result.addNotChanged(bindingItem);
                    }
                }
            } else if (item instanceof ItemStringXml) {
                ItemStringXml itemString = (ItemStringXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemString.getUuid().getValue());

                if (bindingItem == null) {
                	result.addNewItem(itemString);
                } else {
                    ApItem is = bindingItem.getItem();
                    String value;
                    switch (DataType.fromCode(is.getItemType().getDataType().getCode())) {
                    case STRING:
                        ArrDataString dataString = (ArrDataString) is.getData();
                        value = dataString.getStringValue();
                        break;
                    case TEXT:
                        ArrDataText dataText = (ArrDataText) is.getData();
                        value = dataText.getTextValue();
                        break;
                    case COORDINATES:
                        ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) is.getData();
                        value = GeometryConvertor.convert(dataCoordinates.getValue());
                        break;
                    default:
                        throw new IllegalStateException("Neznámý datový typ " + is.getItemType().getDataType()
                                .getCode());
                    }
                    if (!(is.getItemType().getCode().equals(itemString.getT().getValue()) &&
                            compareItemSpec(is.getItemSpec(), itemString.getS()) &&
                            value.equals(itemString.getValue().getValue()))) {

                    	result.addChanged(bindingItem, itemString);
                    } else {
                    	result.addNotChanged(bindingItem);
                    }
                }
            } else if (item instanceof ItemUnitDateXml) {
                ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemUnitDate.getUuid().getValue());

                if (bindingItem == null) {
                    result.addNewItem(itemUnitDate);
                } else {
                    ApItem iud = bindingItem.getItem();
                    ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) iud.getData();
                    if (compareUnitDate(iud, dataUnitdate, itemUnitDate)) {
                        result.addNotChanged(bindingItem);
                    } else {
                    	result.addChanged(bindingItem, itemUnitDate);
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid item type");
            }
        }
        return result;
    }

	private void prepareBooleanUpdate(Map<String, ApBindingItem> bindingItemLookup, ItemBooleanXml itemBoolean, ItemUpdates result) {
		ApBindingItem bindingItem = bindingItemLookup.get(itemBoolean.getUuid().getValue());
        if (bindingItem == null) {
        	result.addNewItem(itemBoolean);
        } else {
            ApItem ib = bindingItem.getItem();
            ArrDataBit dataBit = (ArrDataBit) ib.getData();
            if (!(ib.getItemType().getCode().equals(itemBoolean.getT().getValue()) &&
                    compareItemSpec(ib.getItemSpec(), itemBoolean.getS()) &&
                    dataBit.isBitValue().equals(itemBoolean.getValue().isValue()))) {
            	result.addChanged(bindingItem, itemBoolean);
            } else {
            	result.addNotChanged(bindingItem);
            }
        }
	}

	private boolean matchItemType(ApItem is, CodeXml t, CodeXml s) {
        RulItemType itemType = this.procCtx.getStaticDataProvider().getItemType(t.getValue());
        if (!Objects.equals(itemType.getItemTypeId(), is.getItemTypeId())) {
            return false;
        }
        // check if we have any spec
        if (is.getItemSpecId() == null && (s == null || s.getValue() == null)) {
            return true;
        }
        // check if we have spec from CAM
        if (s == null || s.getValue() == null) {
            // spec is empty -> difference
            return false;
        }
        RulItemSpec itemSpec = this.procCtx.getStaticDataProvider().getItemSpec(s.getValue());
        if (itemSpec == null) {
            // spec not found
            return false;
        }
        if (!Objects.equals(itemSpec.getItemSpecId(), is.getItemSpecId())) {
            return false;
        }
        return true;
    }
    
    private void prepareBinaryUpdate(Map<String, ApBindingItem> bindingItemLookup, ItemBinaryXml itemBinary, ItemUpdates result) {
    	ApBindingItem bindingItem = bindingItemLookup.get(itemBinary.getUuid().getValue());
        if (bindingItem == null) {
        	result.addNewItem(itemBinary);
        } else {
            ApItem is = bindingItem.getItem();
            boolean processed = false;
            if (matchItemType(is, itemBinary.getT(), itemBinary.getS())) {
                ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) is.getData();
                Geometry value = dataCoordinates.getValue();
                Geometry xmlValue = GeometryConvertor.convertWkb(itemBinary.getValue().getValue());
                // try to compare coordinates
                try {
                    if (xmlValue.equals(value)) {
                    	result.addNotChanged(bindingItem);
                        processed = true;
                    }
                } catch (Exception e) {
                    log.error("Failed to compare received coordinates. Item will be updated as changed.", e);
                }
            }
            if (!processed) {
            	result.addChanged(bindingItem, itemBinary);
            }
        }		

	}

	private void prepareEntityRefUpdate(Map<String, ApBindingItem> bindingItemLookup, ItemEntityRefXml itemEntityRef, ItemUpdates result) {
        ApBindingItem bindingItem = bindingItemLookup.get(itemEntityRef.getUuid().getValue());

        if (bindingItem == null) {
        	result.addNewItem(itemEntityRef);
        } else {
        	// we found mapping
            ApItem ier = bindingItem.getItem();
            ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) ier.getData();
            EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
            String entityRefId = CamHelper.getEntityIdorUuid(entityRecordRef);
            // get binding for record
            ApAccessPoint ap = dataRecordRef.getRecord();
            ApBinding binding = null;
            if(ap!=null) {
            	ApBindingState bindingState = externalSystemService.findByAccessPointAndExternalSystem(ap, procCtx.getApExternalSystem());
            	if(bindingState!=null) {
            		binding = bindingState.getBinding();
            	}
            } else {
            	binding = dataRecordRef.getBinding();
            }
            if (!(ier.getItemType().getCode().equals(itemEntityRef.getT().getValue()) &&
                    compareItemSpec(ier.getItemSpec(), itemEntityRef.getS()) &&
                    binding!=null&&
                    binding.getValue().equals(entityRefId))) {
            	result.addChanged(bindingItem, itemEntityRef);
            } else {
            	result.addNotChanged(bindingItem);
            }
        }		
	}

	private boolean compareUnitDate(ApItem iud, ArrDataUnitdate dataUnitdate, ItemUnitDateXml itemUnitDate) {
	    // porovnání typu
	    if (!iud.getItemType().getCode().equals(itemUnitDate.getT().getValue()) ||
	            !compareItemSpec(iud.getItemSpec(), itemUnitDate.getS())) {
	       return false;
	    }
	    // porovnání hodnot
	    if (!dataUnitdate.getValueFrom().equals(itemUnitDate.getF().trim()) ||
	            !dataUnitdate.getValueTo().equals(itemUnitDate.getTo().trim()) ||
	            !dataUnitdate.getFormat().equals(itemUnitDate.getFmt())) {
	        return false;
	    }
	    // porovnání zda jde o odhad
	    Boolean fromEstimated = (itemUnitDate.isFe() == null) ? false : itemUnitDate.isFe();
	    Boolean toEstimated = (itemUnitDate.isToe() == null) ? false : itemUnitDate.isToe();
	    if (!dataUnitdate.getValueFromEstimated().equals(fromEstimated) ||
	            !dataUnitdate.getValueToEstimated().equals(toEstimated)) {
	        return false;
	    }
	    return true;
	}

    static private boolean compareItemSpec(RulItemSpec itemSpec, CodeXml itemSpecCode) {
        if (itemSpec == null) {
            return itemSpecCode == null;
        } else {
            return itemSpec.getCode().equals(itemSpecCode.getValue());
        }
    }
}
