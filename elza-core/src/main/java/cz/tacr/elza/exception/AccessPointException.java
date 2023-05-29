package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.RegistryCode;

/**
 * AccessPoint related exception
 *
 */
public class AccessPointException extends AbstractException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AccessPointException(String message, RegistryCode errorCode) {
        super(message, errorCode);
    }

    public RegistryCode getErrorCode() {
        return (RegistryCode) errorCode;
    }

}
