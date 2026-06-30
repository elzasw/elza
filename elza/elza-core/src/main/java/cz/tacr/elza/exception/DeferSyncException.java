package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ExternalCode;

/**
 * Thrown during CAM synchronization when an access point has been replaced in
 * CAM, but the replacing entity has not been downloaded into ELZA yet. Until it
 * is available locally the references cannot be migrated to the replacement and
 * the access point cannot be invalidated, so the queue item is deferred and
 * retried once the replacing entity appears.
 */
public class DeferSyncException extends AbstractException {

    public DeferSyncException(final String message) {
        super(message, ExternalCode.SYNC_DEFERRED);
    }
}
