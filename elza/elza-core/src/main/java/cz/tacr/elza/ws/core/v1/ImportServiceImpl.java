package cz.tacr.elza.ws.core.v1;

import java.io.IOException;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.cam.schema.cam.BatchUpdateXml;
import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.ObjectFactory;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.repository.ScopeRepository;
import cz.tacr.elza.ws.types.v1.ImportDisposition;
import cz.tacr.elza.ws.types.v1.ImportRequest;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "ImportService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.ImportService")
public class ImportServiceImpl implements ImportService {

    final private static Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    @Autowired
    private ScopeRepository scopeRepository;

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    final protected static ObjectFactory objectcFactory = CamUtils.getObjectFactory();

    @Override
    @Transactional
    public void importData(ImportRequest request) throws CoreServiceException {
        try {
            switch (request.getRequiredFormat()) {
            case CamUtils.CAM_SCHEMA:
                importCamSchema(request.getDisposition(), request.getBinData());
                break;
            default:
                throw new IllegalStateException("Unrecognized import format");
            }
        } catch (Exception e) {
            logger.error("Failed to import", e);
            throw WSHelper.prepareException("Failed to import data", e);
        }
    }

    private void importCamSchema(ImportDisposition disposition, DataHandler binData) throws JAXBException, IOException {
        String scopeCode = disposition.getPrimaryScope();
        ApScope scope = scopeRepository.findByCode(scopeCode);
        Validate.notNull(scope);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        // read CAM xml
        Object obj = unmarshaller.unmarshal(binData.getInputStream());
        if (obj instanceof BatchUpdateXml) {
            BatchUpdateXml bu = (BatchUpdateXml) obj;
            importCam(disposition, bu);
        } else {
            throw new IllegalStateException("Unrecognized object type: " + obj);
        }
    }

    private void importCam(ImportDisposition disposition, BatchUpdateXml bu) {
        throw new IllegalStateException("Not implemented");
    }
}
