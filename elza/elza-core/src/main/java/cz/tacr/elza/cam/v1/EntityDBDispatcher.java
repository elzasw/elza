package cz.tacr.elza.cam.v1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import cz.tacr.elza.common.db.HibernateUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.locationtech.jts.geom.Geometry;


import cz.tacr.cam.v1.schema.cam.CodeXml;
import cz.tacr.cam.v1.schema.cam.EntityRecordRefXml;
import cz.tacr.cam.v1.schema.cam.EntityRecordStateXml;
import cz.tacr.cam.v1.schema.cam.EntityXml;
import cz.tacr.cam.v1.schema.cam.ItemBinaryXml;
import cz.tacr.cam.v1.schema.cam.ItemBooleanXml;
import cz.tacr.cam.v1.schema.cam.ItemEntityRefXml;
import cz.tacr.cam.v1.schema.cam.ItemEnumXml;
import cz.tacr.cam.v1.schema.cam.ItemIntegerXml;
import cz.tacr.cam.v1.schema.cam.ItemLinkXml;
import cz.tacr.cam.v1.schema.cam.ItemStringXml;
import cz.tacr.cam.v1.schema.cam.ItemUnitDateXml;
import cz.tacr.cam.v1.schema.cam.ItemsXml;
import cz.tacr.cam.v1.schema.cam.PartXml;
import cz.tacr.cam.v1.schema.cam.PartsXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.cam.AbstractEntityDBDispatcher;
import cz.tacr.elza.cam.ItemUpdates;
import cz.tacr.elza.cam.ItemUpdates.ChangedBindedItem;
import cz.tacr.elza.cam.ProcessingContext;
import cz.tacr.elza.cam.ReceivedItem;
import cz.tacr.elza.cam.ReceivedPart;
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
import cz.tacr.elza.domain.converter.CalendarConverter;
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
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SyncImpossibleException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointItemService.ReferencedEntities;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.MultipleApChangeContext;
import cz.tacr.elza.service.PartService;
import cz.tacr.elza.service.RuleService;
import cz.tacr.elza.service.cache.AccessPointCacheService;

/**
 * Create or update entities
 *
 * Dispatcher is single threaded and can be used multiple times
 */
public class EntityDBDispatcher extends AbstractEntityDBDispatcher {

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

    private final CamService camService;

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
                              final RuleService ruleService,
                              final CamService camService) {
        super(accessPointRepository, stateRepository, bindingRepository, bindingItemRepository,
              dataRecordRefRepository, externalSystemService, accessPointService, accessPointItemService,
              asyncRequestService, partService, accessPointCacheService, ruleService,
              V1XmlAdapters.INSTANCE);
        this.camService = camService;
    }

    /**
     * Method to take entities
     *
     * Method should not be called from async queues.
     * Method will fail if other entity with same key value exists
     *
     * @param procCtx
     * @param entities
     * @throws SyncImpossibleException 
     */
    public void takeEntities(ProcessingContext procCtx, List<EntityXml> entities) {
    	log.debug("Take entities, count: {}", entities.size());

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
                    // entity exists and not deleted -> synchronization entity with current records
                	if (bindingState == null) {
                		bindingState = externalSystemService.createBindingState(binding, state.getAccessPoint(), 
                				procCtx.getApChange(),
                				entity.getEns().value(),
                				entity.getRevi().getRid() != null ? entity.getRevi().getRid().getValue() : null,
                				entity.getRevi().getUsr() != null ? entity.getRevi().getUsr().getValue() : null,
                				entity.getReid() != null ? entity.getReid().getValue() : null,
                				SyncState.SYNC_OK,
                                state.getAccessPoint().getPreferredPart(),
                                state.getApType());
                	}
                    state = synchronizeAccessPoint(procCtx, state, bindingState, entity, false);
                    createdEntities.add(state);
                } else {
                    state = restoreAccessPoint(entity, binding, state.getAccessPoint(), false);
                    accessPointService.publishAccessPointRestoreEvent(state.getAccessPoint());
                    createdEntities.add(state);
                }
            } else {
                state = createAccessPoint(entity, binding, srcUuid, false);
                accessPointService.publishAccessPointCreateEvent(state.getAccessPoint());
                createdEntities.add(state);
            }
            accessPointService.setAccessPointInDataRecordRefs(state.getAccessPoint(), dataRecordRefList, binding);
        }

        if (CollectionUtils.isNotEmpty(dataRecordRefList)) {
            dataRecordRefRepository.saveAll(dataRecordRefList);
            List<Integer> accessPointIds = ObjectListIterator.findIterable(dataRecordRefList,
                                                                           accessPointRepository::findAccessPointIdsByRefData);
            if (CollectionUtils.isNotEmpty(accessPointIds)) {
                asyncRequestService.enqueueAp(accessPointIds);
            }
        }

        this.procCtx = null;
    }

    /**
     * Connecting to an existing entity
     * 
     * @param procCtx
     * @param state
     * @param entity
     * @param replace
     * @param async
     */
    public void connectEntity(ProcessingContext procCtx, ApState state, EntityXml entity, boolean replace, boolean async) {
    	log.debug("Connect entity, accessPointId: {}, entityId: {}", state.getAccessPointId(), entity.getEid().getValue());

    	Objects.requireNonNull(procCtx.getApChange());

    	this.procCtx = procCtx;

        ApAccessPoint accessPoint = state.getAccessPoint();
        ApChange apChange = procCtx.getApChange();

        if (replace) {
            partService.deleteParts(accessPoint, apChange);
        }

        String bindingValue = Long.toString(entity.getEid().getValue());
        ApBinding binding = procCtx.getBindingByValue(bindingValue);
        if (binding == null) {
        	binding = externalSystemService.createApBinding(bindingValue, procCtx.getApExternalSystem(), true);
        }

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
     * @return ApState
     */
    public ApState synchronizeAccessPoint(ProcessingContext procCtx,
                                       ApState state,
                                       @Nonnull final ApBindingState prevBindingState,
                                       EntityXml entity,
                                       boolean syncQueue) {
    	Objects.requireNonNull(procCtx.getApChange());
    	Objects.requireNonNull(prevBindingState);

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
        if (syncQueue) {
            boolean syncFailed = false;
            // check if exists subparts without binding
            // in such case we cannot run synchronization - subparts has to be resolve first
            if (CollectionUtils.isNotEmpty(partsWithoutBinding)) {
                for (ApPart partWithoutBinding : partsWithoutBinding) {
                    if (partWithoutBinding.getParentPart() != null) {
                        syncFailed = true;
                        break;
                    }
                }
            }
            // delete AP
            if (!syncFailed && entity.getEns() == EntityRecordStateXml.ERS_INVALID) {
                try {
                    accessPointService.checkDeletion(accessPoint);
                } catch (Exception e) {
                    // entity cannot be deleted -> has to be mark as not synchronized
                    // -> sync failed
                    syncFailed = true;
                }
            }

            if (syncFailed) {
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
                accessPointCacheService.createApCachedAccessPoint(state.getAccessPointId());
                this.procCtx = null;
                return state;
            }
        }

        // check s AP class/subclass was changed
        ApType apType = sdp.getApTypeByCode(entity.getEnt().getValue());        
        if (!state.getApTypeId().equals(apType.getApTypeId())) {
            log.debug("Změna třídy (typu) entity: typeId={} -> newTypeId={}", state.getApTypeId(), apType.getApTypeId());
        	if (entity.getEns() != EntityRecordStateXml.ERS_REPLACED && entity.getEns() != EntityRecordStateXml.ERS_INVALID) {
                // změna třídy (typu) entity
                if (!deletedEntity) {
                    state.setDeleteChange(procCtx.getApChange());
                    state = stateRepository.save(state);
                    stateRepository.flush();
                }
                stateNew = accessPointService.copyState(state, procCtx.getApChange());
                if (deletedEntity && syncQueue) {
               		// retain deleted state
               		stateNew.setDeleteChange(procCtx.getApChange());
                }
                stateNew.setApType(apType);
                state = stateRepository.save(stateNew);
        	} else {
        		// if entity will be deleted and class is changed
        		// -> create unversioned change of class
        		state.setApType(apType);
        	}
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
        StateApproval oldStateApproval = null;
        StateApproval newStateApproval = null;
        // Flags to determine if arch. desc have to be revalidated
        boolean wasDeleted = (state.getDeleteChangeId()!=null), willBeDeleted = false;
        switch (entity.getEns()) {
        case ERS_REPLACED:
            // entita je nahrazena v CAM -> musíme nahradit v ELZA
            ApBinding binding = bindingRepository.findByValueAndExternalSystem(extReplacedBy, procCtx.getApExternalSystem());
            if (binding != null) {
                Optional<ApBindingState> replacedBindingState = externalSystemService.getBindingState(binding);
                if (replacedBindingState.isPresent()) {
                    ApAccessPoint replacedBy = replacedBindingState.get().getAccessPoint();
                    ApState replacementState = stateRepository.findLastByAccessPointId(replacedBy.getAccessPointId());
                    try {
						accessPointService.replace(state, replacementState, bindingState.getApExternalSystem(), mcc, syncQueue);
					} catch (SyncImpossibleException e) {
			            log.error("Replacement error, accessPointId: {}, replacedAccessPointId: {}",
			            		  state.getAccessPointId(),
			            		  replacementState.getAccessPointId());
			            throw new BusinessException("Replacement error, accessPointId: " + state.getAccessPointId()
			                      + ", replacedAccessPointId: " + replacementState.getAccessPointId(), e,
			                      BaseCode.INVALID_STATE)
			                      .set("accessPointId", state.getAccessPointId())
			                      .set("replacedAccessPointId", replacementState.getAccessPointId());
					}
                    state.setReplacedBy(replacedBy);
                }
            }
            state = accessPointService.invalidateAccessPoint(state, accessPoint, procCtx.getApChange());
            willBeDeleted = true;
            break;

        case ERS_INVALID:
            // odstranění entity, která v CAM označena jako neplatná
            state = accessPointService.invalidateAccessPoint(state, accessPoint, procCtx.getApChange());
            willBeDeleted = true;
            break;

        default:
            oldStateApproval = state.getStateApproval();
            newStateApproval = camService.convertStateXmlToStateApproval(entity.getEns());
            // synchronizace stavu entit
            // pokud je entita lokalne smazana a jedna se o pozadavek z fronty
            // musi entita zustat smazana
            if (syncQueue && state.getDeleteChangeId() != null) {            	
            	// If system is NOT CAM_COMPLETE 
            	// -> we respect previous state and entity is retained as deleted 
            	if(!procCtx.getApExternalSystem().getType().equals(ApExternalSystemType.CAM_COMPLETE)) {
            		break;
            	}
            }

            // check new state or if entity was deleted
			if (!newStateApproval.equals(state.getStateApproval())||state.getDeleteChangeId() != null) {
				// prepare new state if not already prepared
				if (stateNew == null) {
					state.setDeleteChange(procCtx.getApChange());
					state = stateRepository.save(state);
					stateRepository.flush();
					stateNew = accessPointService.copyState(state, procCtx.getApChange());
				}
				if(state.getDeleteChangeId() != null) {
	        		// entity is return to non deleted state
	        		log.info("Deleted entity is restored to non deleted state, ap id: {}, ext. entity id: {}", state.getAccessPointId(), 
	        				entity.getEid()!=null?entity.getEid().getValue():"");
	        		stateNew.setDeleteChange(null);
				}

				stateNew.setStateApproval(newStateApproval);
				state = stateRepository.save(stateNew);
			}

            break;
        }

        accessPointService.updatePartsIndexesAndValidate(accessPoint, state, syncRes.getParts(), syncRes.getItemMap(), syncQueue);
        if (accessPointService.isArchDescRevalidationRequired(oldStateApproval, newStateApproval, wasDeleted, willBeDeleted)) {
            ruleService.revalidateNodesWithApRef(accessPoint.getAccessPointId());
        }
        mcc.add(accessPoint.getAccessPointId());
        for (Integer apId : mcc.getModifiedApIds()) {
            accessPointCacheService.createApCachedAccessPoint(apId);
        }

        // enqueue dependent APs for async revalidation (index regeneration)
        // Groovy scripts may include data from referenced APs (e.g. names)
        // so when this AP changes, dependent APs' indexes may become stale
        List<Integer> refDataIds = dataRecordRefRepository.findIdsByRecord(accessPoint);
        if (!refDataIds.isEmpty()) {
            List<Integer> dependentApIds = accessPointRepository.findAccessPointIdsByRefDataId(refDataIds);
            dependentApIds.removeAll(mcc.getModifiedApIds());
            if (!dependentApIds.isEmpty()) {
                asyncRequestService.enqueueAp(dependentApIds);
            }
        }

        this.procCtx = null;

        return state;
    }

    /**
     * Restore access point which was alreay deleted
     *
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

    private void createPartsFromEntityXml(final EntityXml entity,
                                  final ApAccessPoint accessPoint,
                                  final ApChange apChange,
                                  final ApState apState,
                                  final ApBinding binding,
                                  boolean async) {
    	Objects.requireNonNull(binding);

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

        accessPointService.updatePartsIndexesAndValidate(accessPoint, apState, partList, itemMap, async);
        accessPointCacheService.createApCachedAccessPoint(accessPoint.getAccessPointId());
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
    private ApBindingItem createPart(PartXml part, ApPart parentPart, ApAccessPoint accessPoint, ApBinding binding) {
        StaticDataProvider sdp = procCtx.getStaticDataProvider();
        RulPartType partType = sdp.getPartTypeByCode(part.getT().value());
        ApChange apChange = procCtx.getApChange();

        ApPart apPart = partService.createPart(partType, accessPoint, apChange, parentPart);
        ApBindingItem bindingPart = externalSystemService.createApBindingItem(binding, apChange, part.getPid().getValue(), apPart, null);

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
    	Objects.requireNonNull(procCtx.getApChange());
        this.procCtx = procCtx;

        return createAccessPoint(entity, binding, entity.getEuid().getValue(), async);
    }

    private ApState createAccessPoint(final EntityXml entity, ApBinding binding, String uuid, boolean async) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        log.debug("Creating entity, changeId: {}, bindingId: {}, uuid: {}, entityId: {}",
                  apChange.getChangeId(), binding != null ? binding.getBindingId() : null, uuid,
                  entity.getEnt() != null ? entity.getEnt().getValue() : null);

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

        // update records referencing newly created AP (arr_data_record_ref)
        List<ApItem> items = accessPointItemService.findUnbindedItemByBinding(binding);
        Set<Integer> updatedApIds = new HashSet<Integer>(); 
        for (ApItem item : items) {
            ArrDataRecordRef dataRef = HibernateUtils.unproxy(item.getData());
            if (dataRef.getRecord() == null) {
                dataRef.setRecord(accessPoint);
                dataRecordRefRepository.save(dataRef);
                ApPart part = item.getPart();
                accessPointService.updatePartIndexes(apState, part);
                updatedApIds.add(part.getAccessPointId());                
            }
        }
        // regeneration cache of the updated entities
        updatedApIds.forEach(accessPointId -> accessPointCacheService.createApCachedAccessPoint(accessPointId));

        return apState;
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

        List<ApItem> itemsByAp = accessPointItemService.findValidItemsByAccessPoint(accessPoint);
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
            itemsFromXml = itms.getItems().stream().map(i -> createReceivedItem(i, dataRefList)).collect(Collectors.toList());
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
        Objects.requireNonNull(itemUpdates);

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

    public List<ApItem> createItems(final PartXml partXml,
                                    final ApPart apPart, final ApChange change,
                                    final ApBinding binding,
                                    final List<ReferencedEntities> dataRefList) {

        if (partXml.getItms() == null) {
            return Collections.emptyList();
        }
        return createItems(partXml.getItms().getItems(), apPart, change, binding, dataRefList);
    }

}
