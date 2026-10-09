package cz.tacr.elza.controller.vo;

/**
 * State of a queue item of an external system as shown to the client, which labels the states
 * itself ({@code api/ExtAsyncQueueState.ts}).
 */
public enum ExtAsyncQueueState {
    UPDATE,
    UPDATE_DEFERRED,
    IMPORT_NEW,
    IMPORT_OK,
    EXPORT_NEW,
    EXPORT_NEED_CONFIRM,
    EXPORT_OK,
    EXPORT_CANCELLED,
    ERROR;

    public static ExtAsyncQueueState fromValue(String v) {
        return valueOf(v);
    }

    public static ExtAsyncQueueState fromValue(cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState state) {
        switch (state) {
        case ERROR:
            return ERROR;
        case UPDATE:
            return UPDATE;
        case UPDATE_DEFERRED:
            return UPDATE_DEFERRED;
        case EXPORT_OK:
            return EXPORT_OK;
        case EXPORT_CANCELLED:
            return EXPORT_CANCELLED;
        case EXPORT_NEW:
            return EXPORT_NEW;
        case EXPORT_START:
        case EXPORT_PROCESSING:
            // transient upload states are not propagated to the client
            return EXPORT_NEW;
        case EXPORT_NEED_CONFIRM:
            return EXPORT_NEED_CONFIRM;
        case IMPORT_NEW:
            return IMPORT_NEW;
        case IMPORT_OK:
            return IMPORT_OK;
        }
        return null;
    }
}
