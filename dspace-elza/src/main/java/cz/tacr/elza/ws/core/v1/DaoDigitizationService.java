package cz.tacr.elza.ws.core.v1;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Digitization service has two methods for notification about result of digitization request. For each request exactly one of these methods have to be called.
 *
 * This class was generated by Apache CXF 3.1.11
 * 2019-06-21T09:42:06.747+02:00
 * Generated source version: 3.1.11
 * 
 */
@WebService(targetNamespace = "http://elza.tacr.cz/ws/core/v1", name = "DaoDigitizationService")
@XmlSeeAlso({cz.tacr.elza.ws.types.v1.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface DaoDigitizationService {

    /**
     * Function for importing result of digitization request.
     */
    @WebMethod(operationName = "DigitizationRequestFinished", action = "DigitizationRequestFinished")
    public void digitizationRequestFinished(
            @WebParam(partName = "digitizationRequestResult", name = "digitizationRequestResult", targetNamespace = "")
                    cz.tacr.elza.ws.types.v1.DigitizationRequestResult digitizationRequestResult
    ) throws CoreServiceException;

    /**
     * Function to revoke some digitization request.
     */
    @WebMethod(operationName = "DigitizationRequestRevoked", action = "DigitizationRequestRevoked")
    public void digitizationRequestRevoked(
            @WebParam(partName = "requestRevoked", name = "requestRevoked", targetNamespace = "")
                    cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked
    ) throws CoreServiceException;
}
