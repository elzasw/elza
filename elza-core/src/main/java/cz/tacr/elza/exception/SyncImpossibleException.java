package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ExternalCode;

/**
 * Výjimka, kdy synchronizace není možná
 */
public class SyncImpossibleException extends AbstractException {

    public SyncImpossibleException(String message) {
    	super(message, ExternalCode.SYNC_IMPOSSIBLE);
    }
}
