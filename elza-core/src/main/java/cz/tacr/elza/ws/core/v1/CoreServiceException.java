
package cz.tacr.elza.ws.core.v1;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 3.1.8
 * 2016-12-09T13:06:28.017+01:00
 * Generated source version: 3.1.8
 */

@WebFault(name = "errorDescription", targetNamespace = "http://elza.tacr.cz/ws/types/v1")
public class CoreServiceException extends Exception {
    
    private cz.tacr.elza.ws.types.v1.ErrorDescription errorDescription;

    public CoreServiceException() {
        super();
    }
    
    public CoreServiceException(String message) {
        super(message);
    }
    
    public CoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoreServiceException(String message, cz.tacr.elza.ws.types.v1.ErrorDescription errorDescription) {
        super(message);
        this.errorDescription = errorDescription;
    }

    public CoreServiceException(String message, cz.tacr.elza.ws.types.v1.ErrorDescription errorDescription, Throwable cause) {
        super(message, cause);
        this.errorDescription = errorDescription;
    }

    public cz.tacr.elza.ws.types.v1.ErrorDescription getFaultInfo() {
        return this.errorDescription;
    }
}