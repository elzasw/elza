package cz.tacr.elza.service.cam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


import cz.tacr.elza.service.cache.AccessPointCacheService;
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
import cz.tacr.cam.schema.cam.PartXml;
import cz.tacr.cam.schema.cam.PartsXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.CalendarType;
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
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.CreateFunction;
import cz.tacr.elza.service.AccessPointItemService.DeletedItems;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.PartService;
import liquibase.pro.packaged.bi;
import ma.glasnost.orika.impl.mapping.strategy.InstantiateByDefaultAndUseCustomMapperStrategy;

/**
 * Create or update entities
 *
 * Dispatcher is single threaded and can be used multiple times
 */
public class EntityDBDispatcher {

    final static Logger log = LoggerFactory.getLogger(EntityDBDispatcher.class);

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
    Map<String, ApBindingItem> bindingItemLookup;

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

    //final private AccessPointDataService accessPointDataService;

    final private CamService camService;

    final private AccessPointCacheService accessPointCacheService;

    private ProcessingContext procCtx;

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
    }

    public void createEntities(ProcessingContext procCtx,
                               List<EntityXml> entities) {

        ApExternalSystem apExternalSystem = procCtx.getApExternalSystem();
        if (procCtx.getApChange() == null) {
            throw new BusinessException("Change not set", BaseCode.INVALID_STATE);
        }
        this.procCtx = procCtx;

        Function<EntityXml, String> idGetter;

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
                state = restoreAccessPoint(entity, binding, state.getAccessPoint());
            } else {
                state = createAccessPoint(entity, binding, srcUuid);
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
                              EntityXml entity, boolean replace) {
        Validate.notNull(procCtx.getApChange());

        this.procCtx = procCtx;

        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = procCtx.getApChange();

        if (replace) {
            partService.deleteParts(accessPoint, apChange);
        }

        ApBinding binding = externalSystemService.createApBinding(procCtx.getScope(),
                                                                  Long.toString(entity.getEid().getValue()),
                                                                  procCtx.getApExternalSystem());

        createPartsFromEntityXml(entity, accessPoint, apChange, state, binding);

        accessPointService.publishAccessPointUpdateEvent(accessPoint);

        this.procCtx = null;
    }

    public void synchronizeAccessPoint(ProcessingContext procCtx,
                                       ApState state,
                                       ApBindingState bindingState,
                                       EntityXml entity) {
        Validate.notNull(procCtx.getApChange());

        this.procCtx = procCtx;

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApAccessPoint accessPoint = state.getAccessPoint();

        ApType apType = sdp.getApTypeByCode(entity.getEnt().getValue());
        if (!state.getApTypeId().equals(apType.getApTypeId())) {
            //změna třídy entity
            state.setDeleteChange(procCtx.getApChange());
            stateRepository.save(state);
            ApState stateNew = accessPointService.copyState(state, procCtx.getApChange());
            stateNew.setApType(apType);
            state = stateRepository.save(stateNew);
        }

        //vytvoření nového stavu propojení
        bindingState = externalSystemService.createNewApBindingState(bindingState, procCtx.getApChange(),
                                                                     entity.getEns().value(),
                                                                     entity.getRevi().getRid().getValue(),
                                                                     entity.getRevi().getUsr().getValue(),
                                                                     entity.getReid() != null ? entity.getReid()
                                                                             .getValue() : null);

        SynchronizationResult syncRes = synchronizeParts(procCtx, entity, bindingState, accessPoint);

        accessPointService.generateSync(accessPoint.getAccessPointId(), state,
                                        syncRes.getParts(), syncRes.getItemMap());
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());

        this.procCtx = null;
    };

    /**
     * Restore access point which was alreay deleted
     * 
     * @param procCtx
     * @param entity
     * @param binding
     * @param accessPoint
     * @return
     */
    private ApState restoreAccessPoint(EntityXml entity, ApBinding binding, ApAccessPoint accessPoint) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        accessPoint = accessPointService.saveWithLock(accessPoint);
        ApState apState = accessPointService.createAccessPointState(accessPoint, procCtx.getScope(), type, entity.getEns(), apChange);

        createPartsFromEntityXml(entity, accessPoint, apChange, apState, binding);

        return apState;
    }

    void createPartsFromEntityXml(
                                  final EntityXml entity,
                                  final ApAccessPoint accessPoint,
                                  final ApChange apChange,
                                  final ApState apState,
                                  final ApBinding binding) {
        Validate.notNull(binding);

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        externalSystemService.createApBindingState(binding, accessPoint, apChange,
                                                   entity.getEns().value(),
                                                   entity.getRevi().getRid() != null ? entity.getRevi().getRid()
                                                           .getValue() : null,
                                                   entity.getRevi().getUsr() != null ? entity.getRevi().getUsr()
                                                           .getValue() : null,
                                                   entity.getReid() != null ? entity.getReid().getValue() : null);
        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

        List<ReferencedEntities> dataRefList = new ArrayList<>();

        for (PartXml part : entity.getPrts().getList()) {
            ApBindingItem bindingPart = createPart(part, accessPoint, binding);
            ApPart apPart = bindingPart.getPart();

            List<ApItem> itemList = createItems(part.getItms().getItems(), apPart, apChange, binding, dataRefList);

            itemMap.put(apPart.getPartId(), itemList);
            partList.add(apPart);
        }

        camService.createBindingForRel(dataRefList, procCtx);

        accessPoint.setPreferredPart(accessPointService.findPreferredPart(partList));

        accessPointService.generateSync(accessPoint.getAccessPointId(), apState, partList, itemMap);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
    }

    /**
     * Create new part from XML part
     * 
     * Part is without items
     * 
     * @param accessPoint
     * @param apChange
     * @param binding
     * @param part
     * @return
     */
    ApBindingItem createPart(PartXml part,
                             ApAccessPoint accessPoint,                              
                             ApBinding binding) {
        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
        ApChange apChange = procCtx.getApChange();
        
        ApPart parentPart;
        if(part.getPrnt() != null) {
            parentPart = accessPointService.findParentPart(binding, part.getPrnt().getValue());
            if(parentPart==null) {
                throw new SystemException("Missing parent part", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("parentValue", part.getPrnt().getValue())
                        .set("bindingId", binding.getBindingId())
                        .set("accessPointId", accessPoint.getAccessPointId());
            }
        } else {
            parentPart = null; 
        }

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
        ApBindingItem bindingPart = externalSystemService.createApBindingItem(binding, apChange, part.getPid()
                .getValue(), apPart, null);

        return bindingPart;
    }

    /**
     * Vytvoření nového ApState a ApAccessPoint
     *  
     * @param procCtx
     * @param entity 
     * @param binding
     * @return ApState
     */
    public ApState createAccessPoint(ProcessingContext procCtx, EntityXml entity, ApBinding binding) {
        Validate.notNull(procCtx.getApChange());
        this.procCtx = procCtx;

        return createAccessPoint(entity, binding, entity.getEuid().getValue());
    }

    private ApState createAccessPoint(final EntityXml entity, ApBinding binding, String uuid) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        ApState apState = accessPointService.createAccessPoint(procCtx.getScope(), type, entity.getEns(), apChange, uuid);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        createPartsFromEntityXml(entity, accessPoint, apChange, apState, binding);

        return apState;
    }

    public List<ApState> getApStates() {
        return createdEntities;
    }

    static class SynchronizationResult {
        List<ApPart> partList = new ArrayList<>();
        Map<Integer, List<ApItem>> itemMap = new HashMap<>();

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
    }


    /**
     * Synchronizace částí přístupového bodu z externího systému
     *
     * @param procCtx
     *            context
     * @param entity
     *            entita z externího systému
     * @param bindingState
     *            stav propojení s externím systémem
     * @param apChange
     *            změna
     * @param accessPoint
     *            přístupový bod
     * @param partList
     *            přidané nebo změněné části
     * @param itemMap
     *            prvky popisu přidaných nebo změněných částí
     */
    private SynchronizationResult synchronizeParts(final ProcessingContext procCtx,
                                                   final EntityXml entity,
                                                   final ApBindingState bindingState,
                                                   final ApAccessPoint accessPoint) {
        Integer accessPointId = accessPoint.getAccessPointId();
        PartsXml partsXml = entity.getPrts();
        if(partsXml==null) {
            log.error("Element parts is empty, accessPointId: {}, entityUuid: {}",
                      accessPointId,
                      entity.getEuid().toString());
            throw new BusinessException("Element parts is empty, accessPointId: " + accessPoint.getAccessPointId(),
                    BaseCode.INVALID_STATE)
                            .set("accessPointId", accessPoint.getAccessPointId());
        }
        
        log.debug("Synchronizing parts, accessPointId: {}, number of parts: {}", accessPointId,
                  partsXml.getList().size());

        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        ApChange apChange = procCtx.getApChange();
        ApBinding binding = bindingState.getBinding();
        readBindingItems(binding);

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
            
            ApBindingItem bindingItem = bindingPartLookup.remove(partXml.getPid().getValue());
            List<ApItem> itemList;
            if (bindingItem != null) {
                log.debug("Part with required binding was found, updating existing binding, accessPointId: {}, bindingItemId: {}",
                          accessPointId,
                          bindingItem.getBindingItemId());
                // Binding found -> update
                itemList = updatePart(partXml, bindingItem.getPart(), binding, dataRefList);
            } else {
                log.debug("Part with binding does not exists, creating new binding, accessPointId: {}", accessPointId);

                bindingItem = createPart(partXml, accessPoint, binding);
                itemList = createItems(partXml.getItms().getItems(),
                                                    bindingItem.getPart(), apChange, binding,
                                       dataRefList);
            }
            syncResult.addPartItems(bindingItem.getPart(), itemList);
            syncResult.addPart(bindingItem.getPart());

            if (preferredName == null) {
                ApPart lastPart = bindingItem.getPart();
                if (StaticDataProvider.DEFAULT_PART_TYPE.equals(lastPart.getPartType().getCode())) {
                    preferredName = lastPart;
                }
            }
        }

        // smazání partů dle externího systému
        // mažou se zbývající party
        deletePartsInLookup(apChange);

        // smazání zbývajících nezpracovaných item
        Collection<ApBindingItem> remainingBindingItems = bindingItemLookup.values();
        if (remainingBindingItems.size() > 0) {
            List<ApItem> items = remainingBindingItems.stream().map(ApBindingItem::getItem)
                    .collect(Collectors.toList());
            deleteItems(items, apChange);
        }
        if (bindingItemLookup.size() > 0) {
            log.error("Exists unresolved bindings (items), accessPointId: {}, items: {}",
                      accessPoint.getAccessPointId(),
                      bindingItemLookup.keySet());
            throw new BusinessException("Exists unresolved bindings, accessPointId: " + accessPointId +
                    ", count: " + bindingItemLookup.size(),
                    BaseCode.DB_INTEGRITY_PROBLEM)
                            .set("accessPointId", accessPointId);
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
        accessPoint.setPreferredPart(preferredName); //.findPreferredPart(entity.getPrts().getList(), newBindingParts));

        bindingPartLookup = null;
        bindingItemLookup = null;

        return syncResult;
    }

    private void deletePartsInLookup(ApChange apChange) {
        if (bindingPartLookup.isEmpty()) {
            return;
        }

        Collection<ApBindingItem> partsBinding = bindingPartLookup.values();
        List<ApPart> partList = new ArrayList<>();
        for (ApBindingItem partBinding : partsBinding) {
            ApPart part = partBinding.getPart();
            log.debug("Deleting part binding, bindingItemId: {}, partId: {}",
                      partBinding.getBindingItemId(),
                      part.getPartId());
            partList.add(part);
            partBinding.setDeleteChange(apChange);
        }
        bindingItemRepository.saveAll(partsBinding);

        // clear lookup
        bindingPartLookup.clear();

        List<ApItem> items = accessPointItemService.findItemsByParts(partList);
        deleteItems(items, apChange);

        partService.deleteParts(partList, apChange);
    }

    private void deleteItems(List<ApItem> items, ApChange apChange) {
        // delete items in parts
        DeletedItems deletedItems = accessPointItemService.deleteItems(items, apChange);
        
        // delete bindings from lookup
        for (ApBindingItem bindingItem : deletedItems.getBindings()) {
            bindingItemLookup.remove(bindingItem.getValue());
        }
    }

    class ChangedBindedItem {
        final ApBindingItem bindingItem;
        final Object xmlItem;

        public ChangedBindedItem(ApBindingItem bindingItem, Object xmlItem) {
            super();
            this.bindingItem = bindingItem;
            this.xmlItem = xmlItem;
        }

        ApBindingItem getBindingItem() {
            return bindingItem;
        }

        Object getXmlItem() {
            return xmlItem;
        }
    };

    private List<ApItem> updatePart(PartXml partXml, ApPart apPart,
                            ApBinding binding, List<ReferencedEntities> dataRefList) {

        List<Object> itemsXml;

        if (partXml.getItms() != null) {
            itemsXml = partXml.getItms().getItems();
        } else {
            itemsXml = Collections.emptyList();
        }

        List<ApBindingItem> notChangeItems = new ArrayList<>();
        List<ChangedBindedItem> changedItems = new ArrayList<>();
        List<Object> newItems = findNewOrChangedItems(itemsXml,
                                                      changedItems,
                                                      notChangeItems);

        List<ApItem> result = new ArrayList<>(newItems.size() + changedItems.size() + notChangeItems.size());
        // remove unchanged items from binding lookup and add to result
        for (ApBindingItem notChangeItem : notChangeItems) {
            ApBindingItem removedItem = bindingItemLookup.remove(notChangeItem.getValue());
            if (removedItem == null) {
                throw new SystemException("Missing item in lookup.")
                        .set("missingValue", notChangeItem.getValue());
            }
            result.add(removedItem.getItem());
        }

        if (CollectionUtils.isNotEmpty(changedItems)) {
            // drop old bindings
            List<ApBindingItem> bindedItems = changedItems.stream().map(ChangedBindedItem::getBindingItem)
                    .collect(Collectors.toList());
            deleteBindedItems(bindedItems, procCtx.getApChange());
            
            List<Object> xmlItems = changedItems.stream().map(ChangedBindedItem::getXmlItem)
                    .collect(Collectors.toList());
            // import changed items
            result.addAll(createItems(xmlItems, apPart, procCtx.getApChange(), binding, dataRefList));
        }

        if (CollectionUtils.isNotEmpty(newItems)) {

            result.addAll(createItems(newItems, apPart, procCtx.getApChange(), binding, dataRefList));
        }
        return result;
    }

    private void deleteBindedItems(List<ApBindingItem> bindingItemsInPart, ApChange apChange) {
        if (CollectionUtils.isEmpty(bindingItemsInPart)) {
            return;
        }
        for (ApBindingItem bindingItem : bindingItemsInPart) {
            bindingItemLookup.remove(bindingItem.getValue());
        }

        accessPointItemService.deleteBindnedItems(bindingItemsInPart, apChange);
    }

    public List<ApItem> createItems(final List<Object> createItems,
                                    ApPart apPart, final ApChange change,
                                    final ApBinding binding,
                                    final List<ReferencedEntities> dataRefList) {
        List<ApItem> itemsCreated = new ArrayList<>();
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

            String extIdent = CamHelper.getEntityIdorUuid(itemEntityRef);

            ReferencedEntities dataRef = new ReferencedEntities(dataRecordRef, extIdent);
            dataRefList.add(dataRef);

            dataRecordRef.setDataType(DataType.RECORD_REF.getEntity());
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
            dataUriRef.setSchema(ArrDataUriRef.createSchema(itemLink.getUrl().getValue()));
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

            CalendarType calType = CalendarType.GREGORIAN;
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
                dataUnitDate.setNormalizedFrom(CalendarConverter.toSeconds(calType, LocalDateTime.parse(itemUnitDate
                        .getF().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedFrom(Long.MIN_VALUE);
            }

            if (itemUnitDate.getTo() != null) {
                dataUnitDate.setNormalizedTo(CalendarConverter.toSeconds(calType, LocalDateTime.parse(itemUnitDate
                        .getTo().trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                dataUnitDate.setNormalizedTo(Long.MAX_VALUE);
            }

            dataUnitDate.setCalendarType(calType.getEntity());
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

        List<ApItem> existsItems = typeIdItemsMap.computeIfAbsent(itemType.getItemTypeId(), k -> new ArrayList<>());

        ApItem itemCreated = accessPointItemService.createItemWithSave(part, data, itemType, itemSpec, change,
                                                                       existsItems,
                                                                       binding, uuid);

        existsItems.add(itemCreated);
        return itemCreated;
    }

    private void readBindingItems(ApBinding binding) {
        List<ApBindingItem> bindingItems = this.externalSystemService.getBindingItems(binding);

        Map<Integer, ApBindingItem> partIdBindingMap = new HashMap<>();
        bindingPartLookup = new HashMap<>();
        bindingItemLookup = new HashMap<>();
        
        for (ApBindingItem bindingItem : bindingItems) {
            if (bindingItem.getPart() != null) {
                bindingPartLookup.put(bindingItem.getValue(), bindingItem);
                partIdBindingMap.put(bindingItem.getPart().getPartId(), bindingItem);
            } else if (bindingItem.getItem() != null) {
                bindingItemLookup.put(bindingItem.getValue(), bindingItem);
            } else {
                throw new IllegalStateException();
            }
        }

        // safety check 
        // binded item should belong to some part in lookup
        for (ApBindingItem bitem : bindingItemLookup.values()) {
            ApItem apItem = bitem.getItem();
            Integer partId = apItem.getPartId();
            ApBindingItem parentBinding = partIdBindingMap.get(partId);
            if (parentBinding == null) {
                log.error("Item with binding, but part is not binded, bindingItemId: {}, apItemId: {}, partId: {}",
                          bitem.getBindingItemId(), apItem.getItemId(), partId);
                throw new SystemException("Item with binding, but part is not binded",
                        BaseCode.DB_INTEGRITY_PROBLEM)
                                .set("bindingItemId", bitem.getBindingItemId())
                                .set("apItemId", apItem.getItemId())
                                .set("partId", partId);
            }
        }

    }

    public List<Object> findNewOrChangedItems(List<Object> items,
                                              List<ChangedBindedItem> changedItems,
                                              List<ApBindingItem> notChangeItems) {
        List<Object> newItems = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof ItemBinaryXml) {
                ItemBinaryXml itemBinary = (ItemBinaryXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemBinary.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemBinary);
                } else {
                    ApItem is = bindingItem.getItem();
                    ArrDataCoordinates dataCoordinates = (ArrDataCoordinates) is.getData();
                    //String value = GeometryConvertor.convert(dataCoordinates.getValue());
                    Geometry value = dataCoordinates.getValue();
                    Geometry xmlValue = GeometryConvertor.convertWkb(itemBinary.getValue().getValue());
                    if (!(is.getItemType().getCode().equals(itemBinary.getT().getValue()) &&
                            compareItemSpec(is.getItemSpec(), itemBinary.getS()) &&
                            xmlValue.equals(value))) {
                        changedItems.add(new ChangedBindedItem(bindingItem, itemBinary));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemBooleanXml) {
                ItemBooleanXml itemBoolean = (ItemBooleanXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemBoolean.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemBoolean);
                } else {
                    ApItem ib = bindingItem.getItem();
                    ArrDataBit dataBit = (ArrDataBit) ib.getData();
                    if (!(ib.getItemType().getCode().equals(itemBoolean.getT().getValue()) &&
                            compareItemSpec(ib.getItemSpec(), itemBoolean.getS()) &&
                            dataBit.isBitValue().equals(itemBoolean.getValue().isValue()))) {
                        changedItems.add(new ChangedBindedItem(bindingItem, itemBoolean));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemEntityRefXml) {
                ItemEntityRefXml itemEntityRef = (ItemEntityRefXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemEntityRef.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemEntityRef);
                } else {
                    ApItem ier = bindingItem.getItem();
                    ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) ier.getData();
                    EntityRecordRefXml entityRecordRef = (EntityRecordRefXml) itemEntityRef.getRef();
                    String entityRefId = CamHelper.getEntityIdorUuid(entityRecordRef);
                    if (!(ier.getItemType().getCode().equals(itemEntityRef.getT().getValue()) &&
                            compareItemSpec(ier.getItemSpec(), itemEntityRef.getS()) &&
                            dataRecordRef.getBinding().getValue().equals(entityRefId))) {

                        changedItems.add(new ChangedBindedItem(bindingItem, itemEntityRef));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemEnumXml) {
                ItemEnumXml itemEnum = (ItemEnumXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemEnum.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemEnum);
                } else {
                    ApItem ie = bindingItem.getItem();
                    if (!(ie.getItemType().getCode().equals(itemEnum.getT().getValue()) &&
                            compareItemSpec(ie.getItemSpec(), itemEnum.getS()))) {

                        changedItems.add(new ChangedBindedItem(bindingItem, itemEnum));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemIntegerXml) {
                ItemIntegerXml itemInteger = (ItemIntegerXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemInteger.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemInteger);
                } else {
                    ApItem ii = bindingItem.getItem();
                    ArrDataInteger dataInteger = (ArrDataInteger) ii.getData();
                    if (!(ii.getItemType().getCode().equals(itemInteger.getT().getValue()) &&
                            compareItemSpec(ii.getItemSpec(), itemInteger.getS()) &&
                            dataInteger.getIntegerValue().equals(itemInteger.getValue().getValue().intValue()))) {

                        changedItems.add(new ChangedBindedItem(bindingItem, itemInteger));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemLinkXml) {
                ItemLinkXml itemLink = (ItemLinkXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemLink.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemLink);
                } else {
                    ApItem il = bindingItem.getItem();
                    ArrDataUriRef dataUriRef = (ArrDataUriRef) il.getData();
                    if (!(il.getItemType().getCode().equals(itemLink.getT().getValue()) &&
                            compareItemSpec(il.getItemSpec(), itemLink.getS()) &&
                            dataUriRef.getUriRefValue().equals(itemLink.getUrl().getValue()) &&
                            dataUriRef.getDescription().equals(itemLink.getNm().getValue()))) {

                        changedItems.add(new ChangedBindedItem(bindingItem, itemLink));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemStringXml) {
                ItemStringXml itemString = (ItemStringXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemString.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemString);
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

                        changedItems.add(new ChangedBindedItem(bindingItem, itemString));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else if (item instanceof ItemUnitDateXml) {
                ItemUnitDateXml itemUnitDate = (ItemUnitDateXml) item;
                ApBindingItem bindingItem = bindingItemLookup.get(itemUnitDate.getUuid().getValue());

                if (bindingItem == null) {
                    newItems.add(itemUnitDate);
                } else {
                    ApItem iud = bindingItem.getItem();
                    ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) iud.getData();
                    if (!(iud.getItemType().getCode().equals(itemUnitDate.getT().getValue()) &&
                            compareItemSpec(iud.getItemSpec(), itemUnitDate.getS()) &&
                            dataUnitdate.getValueFrom().equals(itemUnitDate.getF().trim()) &&
                            dataUnitdate.getValueFromEstimated().equals(itemUnitDate.isFe()) &&
                            dataUnitdate.getFormat().equals(itemUnitDate.getFmt()) &&
                            dataUnitdate.getValueTo().equals(itemUnitDate.getTo().trim()) &&
                            dataUnitdate.getValueToEstimated().equals(itemUnitDate.isToe()))) {

                        changedItems.add(new ChangedBindedItem(bindingItem, itemUnitDate));
                    } else {
                        notChangeItems.add(bindingItem);
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid item type");
            }
        }
        return newItems;
    }

    private boolean compareItemSpec(RulItemSpec itemSpec, CodeXml itemSpecCode) {
        if (itemSpec == null) {
            return itemSpecCode == null;
        } else {
            return itemSpec.getCode().equals(itemSpecCode.getValue());
        }
    }
}
