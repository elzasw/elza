package cz.tacr.elza.ws.core.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cz.tacr.elza.service.cam.CamService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.cam.schema.cam.ObjectFactory;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.service.cam.CamHelper;
import cz.tacr.elza.service.cam.ProcessingContext;
import cz.tacr.elza.service.cam.SyncEntityRequest;
import cz.tacr.elza.ws.types.v1.ImportRequest;
import cz.tacr.elza.ws.types.v1.RequestStatus;
import cz.tacr.elza.ws.types.v1.RequestStatusInfo;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "ImportService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.ImportService")
public class ImportServiceImpl implements ImportService {

    final private static Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ExternalSystemService externalSystemService;

    @Autowired
    private CamService camService;

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    final protected static ObjectFactory objectcFactory = CamUtils.getObjectFactory();

    @Override
    @Transactional
    public void importData(ImportRequest request) throws CoreServiceException {
        try {
            logger.info("Received import request, code: {}, requestId: {}",
                        request.getExternalSystem(),
                        request.getRequestId());

            switch (request.getDataFormat()) {
            case CamUtils.CAM_SCHEMA:
                importCamSchema(request);
                break;
            default:
                throw new IllegalStateException("Unrecognized import format");
            }
        } catch (Exception e) {
            logger.error("Failed to import", e);
            if (e instanceof CoreServiceException) {
                throw (CoreServiceException) e;
            }
            throw WSHelper.prepareException("Failed to import data", e);
        }
    }

    private void importCamSchema(ImportRequest request) throws JAXBException, IOException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        DataHandler binData = request.getBinData();
        // read CAM xml
        Object obj = unmarshaller.unmarshal(binData.getInputStream());
        if (obj instanceof EntitiesXml) {
            EntitiesXml ents = (EntitiesXml) obj;
            importCam(request, ents);
        } else {
            throw new IllegalStateException("Unrecognized object type: " + obj);
        }
    }

    private void importCam(ImportRequest request, EntitiesXml ents) {
        if (request.getDisposition() == null) {
            throw WSHelper.prepareException("Request disposition is null", null);
        }
        if (request.getDisposition().getPrimaryScope() == null) {
            throw WSHelper.prepareException("Primary scope in request disposition is null", null);
        }

        String scopeCode = request.getDisposition().getPrimaryScope();
        ApScope scope = scopeRepository.findByCode(scopeCode);
        if (scope == null) {
            throw WSHelper.prepareException("Scope not found: " + scopeCode, null);
        }
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(request.getExternalSystem());

        importCam(request, scope, externalSystem, ents);
    }

    private void importCam(ImportRequest request, ApScope scope, ApExternalSystem externalSystem, EntitiesXml ents) {
        List<EntityXml> entities = ents.getList();

        Map<String, EntityXml> uuids = CamHelper.getEntitiesByUuid(entities);

        // check if some entities exists
        List<ApAccessPoint> existingAps = accessPointRepository.findApAccessPointsByUuids(uuids.keySet());
        List<EntityXml> newEntities;
        List<SyncEntityRequest> updateEntities;
        if (CollectionUtils.isEmpty(existingAps)) {
            newEntities = entities;
            updateEntities = null;
        } else {
            Map<Integer, SyncEntityRequest> updateEntitiesLookup = new HashMap<>();
            List<ApAccessPoint> updateAps = new ArrayList<>();
            updateEntities = new ArrayList<>();

            Map<String, ApAccessPoint> existingApMap = existingAps.stream().collect(Collectors.toMap(ApAccessPoint::getUuid, Function.identity()));
            
            newEntities = new ArrayList<>(entities.size() - existingAps.size());
            for (EntityXml entityXml : entities) {
                String uuid = CamHelper.getEntityUuid(entityXml);
                ApAccessPoint existingAp = existingApMap.get(uuid);
                if (existingAp == null) {
                    newEntities.add(entityXml);
                } else {
                    SyncEntityRequest syncReq = new SyncEntityRequest(existingAp, entityXml);
                    updateEntities.add(syncReq);
                    updateAps.add(existingAp);
                    updateEntitiesLookup.put(existingAp.getAccessPointId(), syncReq);
                }
            }

            // prepare ap states
            List<ApState> apStates = stateRepository.findLastByAccessPoints(updateAps);
            if (apStates.size() != updateAps.size()) {
                throw new IllegalStateException("Missing state for some synchronized access point");
            }
            for (ApState state : apStates) {
                SyncEntityRequest syncRequest = updateEntitiesLookup.get(state.getAccessPointId());
                syncRequest.setState(state);
            }

            // prepare binding
            List<ApBindingState> bindingStates = bindingStateRepository.findByAccessPoints(updateAps);
            if (apStates.size() != updateAps.size()) {
                throw new IllegalStateException("Missing state for some synchronized access point");
            }
            for (ApBindingState bindingState : bindingStates) {
                SyncEntityRequest syncRequest = updateEntitiesLookup.get(bindingState.getAccessPointId());
                syncRequest.setBindingState(bindingState);
            }

        }
        
        ProcessingContext procCtx = new ProcessingContext(scope, externalSystem);
        camService.createAccessPoints(procCtx, newEntities);
        camService.updateAccessPoints(procCtx, updateEntities);

        logger.info("Imported entities in CAM format, count: {}", entities.size());
    }

    @Override
    public RequestStatusInfo getImportStatus(String requestId) {
        RequestStatusInfo rsi = new RequestStatusInfo();
        rsi.setStatus(RequestStatus.NOT_EXISTS);
        return rsi;
    }
}
