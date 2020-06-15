package cz.tacr.elza.ws.core.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.service.StructObjService;
import cz.tacr.elza.ws.types.v1.StructuredObject;
import cz.tacr.elza.ws.types.v1.StructuredObjectIdentifiers;

@Component
@javax.jws.WebService(serviceName = "CoreService", portName = "StructuredObjectService", targetNamespace = "http://elza.tacr.cz/ws/core/v1",
        //                      wsdlLocation = "file:elza-core-v1.wsdl",
        endpointInterface = "cz.tacr.elza.ws.core.v1.StructuredObjectService")
public class StructuredObjectServiceImpl implements StructuredObjectService {

    @Autowired
    StructObjService structObjService;

    @Override
    public void deleteStructuredObject(StructuredObjectIdentifiers deleteStructuredObj)
            throws DeleteStructuredObjectFailed {
        // TODO Auto-generated method stub

    }

    @Override
    public StructuredObjectIdentifiers createStructuredObject(StructuredObject createStructuredObject)
            throws CreateStructuredObjectFailed {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateStructuredObject(StructuredObject updateStructuredObject) throws UpdateStructuredObjectFailed {
        // TODO Auto-generated method stub

    }

}
