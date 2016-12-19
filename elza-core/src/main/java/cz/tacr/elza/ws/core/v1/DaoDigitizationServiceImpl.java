
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package cz.tacr.elza.ws.core.v1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

/**
 * This class was generated by Apache CXF 3.1.8
 * 2016-12-09T13:06:28.052+01:00
 * Generated source version: 3.1.8
 * 
 */

@Component
@javax.jws.WebService(
                      serviceName = "CoreService",
                      portName = "DaoDigitizationService",
                      targetNamespace = "http://elza.tacr.cz/ws/core/v1",
//                      wsdlLocation = "file:elza-core-v1.wsdl",
                      endpointInterface = "cz.tacr.elza.ws.core.v1.DaoDigitizationService")

public class DaoDigitizationServiceImpl implements DaoDigitizationService {

    private Log logger = LogFactory.getLog(this.getClass());

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoDigitizationService#digitizationRequestFinished(cz.tacr.elza.ws.types.v1.DigitizationRequestResult digitizationRequestResult)*
     */
    public void digitizationRequestFinished(cz.tacr.elza.ws.types.v1.DigitizationRequestResult digitizationRequestResult) throws CoreServiceException   { 
        logger.info("Executing operation digitizationRequestFinished");
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        System.out.println(digitizationRequestResult);
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoDigitizationService#digitizationRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    public void digitizationRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked) throws CoreServiceException   { 
        logger.info("Executing operation digitizationRequestRevoked");
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        System.out.println(requestRevoked);
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

}