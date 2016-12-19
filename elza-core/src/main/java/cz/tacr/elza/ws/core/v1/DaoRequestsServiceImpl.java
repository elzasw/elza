
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
 * 2016-12-09T13:06:28.057+01:00
 * Generated source version: 3.1.8
 * 
 */

@Component
@javax.jws.WebService(
                      serviceName = "CoreService",
                      portName = "DaoRequestsService",
                      targetNamespace = "http://elza.tacr.cz/ws/core/v1",
//                      wsdlLocation = "file:elza-core-v1.wsdl",
                      endpointInterface = "cz.tacr.elza.ws.core.v1.DaoRequestsService")
                      
public class DaoRequestsServiceImpl implements DaoRequestsService {

    private Log logger = LogFactory.getLog(this.getClass());

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#destructionRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    public void destructionRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked) throws CoreServiceException   { 
        logger.info("Executing operation destructionRequestRevoked");
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        System.out.println(requestRevoked);
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#transferRequestFinished(java.lang.String requestIdentifier)*
     */
    public void transferRequestFinished(java.lang.String requestIdentifier) throws CoreServiceException   { 
        logger.info("Executing operation transferRequestFinished");
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        System.out.println(requestIdentifier);
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#transferRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked)*
     */
    public void transferRequestRevoked(cz.tacr.elza.ws.types.v1.RequestRevoked requestRevoked) throws CoreServiceException   { 
        logger.info("Executing operation transferRequestRevoked");
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        System.out.println(requestRevoked);
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

    /* (non-Javadoc)
     * @see cz.tacr.elza.ws.core.v1.DaoRequestsService#destructionRequestFinished(java.lang.String requestIdentifier)*
     */
    public void destructionRequestFinished(java.lang.String requestIdentifier) throws CoreServiceException   { 
        logger.info("Executing operation destructionRequestFinished");
        System.out.println(requestIdentifier);
        // TODO ELZA-1300 - Zprovoznit webové služby - dopsat implementaci
        try {
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new CoreServiceException("CoreServiceException...");
    }

}