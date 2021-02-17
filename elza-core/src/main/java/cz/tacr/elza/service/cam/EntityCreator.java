package cz.tacr.elza.service.cam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ExternalCode;
import cz.tacr.elza.exception.codes.RegistryCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DataRecordRefRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.AsyncRequestService;
import cz.tacr.elza.service.ExternalSystemService;

/**
 * Class will create entities
 *
 */
public class EntityCreator {

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

    final private ApAccessPointRepository accessPointRepository;

    final private ApStateRepository stateRepository;

    final private ApBindingRepository bindingRepository;

    final private DataRecordRefRepository dataRecordRefRepository;

    final private ExternalSystemService externalSystemService;

    final private AccessPointService accessPointService;

    final private AsyncRequestService asyncRequestService;

    final private CamService camService;

    public EntityCreator(final ApAccessPointRepository accessPointRepository,
                         final ApStateRepository stateRepository,
                         final ApBindingRepository bindingRepository,
                         final DataRecordRefRepository dataRecordRefRepository,
                         final ExternalSystemService externalSystemService,
                         final AccessPointService accessPointService,
                         final AsyncRequestService asyncRequestService,
                         final CamService camService) {
        this.accessPointRepository = accessPointRepository;
        this.stateRepository = stateRepository;
        this.bindingRepository = bindingRepository;
        this.dataRecordRefRepository = dataRecordRefRepository;
        this.externalSystemService = externalSystemService;
        this.accessPointService = accessPointService;
        this.asyncRequestService = asyncRequestService;
        this.camService = camService;
    }

    public void createEntities(ProcessingContext procCtx,
                               List<EntityXml> entities) {

        ApExternalSystem apExternalSystem = procCtx.getApExternalSystem();
        if (procCtx.getApChange() == null) {
            throw new BusinessException("Change not set", BaseCode.INVALID_STATE);
        }

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
                state = restoreAccessPoint(procCtx, entity, binding, state.getAccessPoint());
            } else {
                state = createAccessPoint(procCtx, entity, binding, srcUuid);
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

    }

    /**
     * Restore access point which was alreay deleted
     * 
     * @param procCtx
     * @param entity
     * @param binding
     * @param accessPoint
     * @return
     */
    private ApState restoreAccessPoint(ProcessingContext procCtx, EntityXml entity, ApBinding binding,
                                       ApAccessPoint accessPoint) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        accessPoint = accessPointService.saveWithLock(accessPoint);
        ApState apState = accessPointService.createAccessPointState(accessPoint, procCtx.getScope(), type, apChange);

        camService.createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, apState, binding);

        return apState;
    }

    public ApState createAccessPoint(final ProcessingContext procCtx,
                                     final EntityXml entity,
                                     ApBinding binding,
                                     String uuid) {
        Validate.notNull(procCtx, "Context cannot be null");
        ApChange apChange = procCtx.getApChange();
        Validate.notNull(apChange, "Change cannot be null");

        StaticDataProvider sdp = procCtx.getStaticDataProvider();

        ApType type = sdp.getApTypeByCode(entity.getEnt().getValue());
        ApState apState = accessPointService.createAccessPoint(procCtx.getScope(), type, apChange, uuid);
        ApAccessPoint accessPoint = apState.getAccessPoint();

        camService.createPartsFromEntityXml(procCtx, entity, accessPoint, apChange, apState, binding);

        return apState;
    }

    public List<ApState> getApStates() {
        return createdEntities;
    };

}
