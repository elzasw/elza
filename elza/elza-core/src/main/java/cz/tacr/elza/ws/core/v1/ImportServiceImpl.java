package cz.tacr.elza.ws.core.v1;

import java.io.IOException;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ExternalSystemService;
import cz.tacr.elza.ws.types.v1.EntityConflictResolution;
import cz.tacr.elza.ws.types.v1.ImportRequest;
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
    private AccessPointService accessPointService;

    @Autowired
    private ExternalSystemService externalSystemService;

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
        String scopeCode = request.getDisposition().getPrimaryScope();
        ApScope scope = scopeRepository.findByCode(scopeCode);
        if (scope == null) {
            throw WSHelper.prepareException("Scope not found: " + scopeCode, null);
        }
        ApExternalSystem externalSystem = externalSystemService.findApExternalSystemByCode(request.getExternalSystem());
        if (externalSystem == null) {
            throw WSHelper.prepareException("External system not found: " + request.getExternalSystem(), null);
        }

        List<EntityXml> entityList = ents.getList();
        for (EntityXml entityXml : entityList) {
            importCamEntity(scope, externalSystem, entityXml, request.getDisposition().getConflictResolution());
        }

        logger.info("Imported entities in CAM format, count: {}", entityList.size());
    }

    private ApState importCamEntity(ApScope scope, ApExternalSystem externalSystem, EntityXml entityXml,
                                 EntityConflictResolution conflictResolution) {
        String externalEntityId = entityXml.getEuid().getValue();
        // check if entity exists
        ApAccessPoint accessPoint = accessPointRepository.findApAccessPointByUuid(externalEntityId);
        if (accessPoint == null) {
            // check if binding not exists
            boolean refreshReferences = false;
            ApBinding binding = externalSystemService.findByScopeAndValueAndApExternalSystem(scope, externalEntityId,
                                                                                             externalSystem);
            if (binding == null) {
                // prepare new binding
                binding = externalSystemService.createApBinding(scope, externalEntityId, externalSystem);
            } else {
                refreshReferences = true;
            }

            ApState ap = accessPointService.createAccessPoint(scope, entityXml, binding);
            if (refreshReferences) {
                accessPointService.updateDataRefs(ap.getAccessPoint(), binding);
            }
            return ap;
        } else {
            throw new IllegalStateException("Update not implemented");
        }
    }

    @Override
    public RequestStatusInfo getImportStatus(String requestId) {
        return null;
    }
}
