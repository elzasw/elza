package cz.tacr.elza.controller.vo;


public enum ExtAsyncQueueState {

    UPDATE("Aktualizováno"),

    IMPORT_NEW("Ke stažení"),

    IMPORT_OK("Staženo"), // předchozí OK

    EXPORT_NEW("K odeslání"),

    EXPORT_OK("Odesláno"),

    ERROR("Chyba");

    private String value;

    ExtAsyncQueueState(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ExtAsyncQueueState fromValue(String v) {
        return valueOf(v);
    }

    public static ExtAsyncQueueState fromValue(cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState state) {
        switch (state) {
        case ERROR:
            return ERROR;
        case UPDATE:
            return UPDATE;
        case EXPORT_OK:
            return EXPORT_OK;
        case EXPORT_NEW:
            return EXPORT_NEW;
        case EXPORT_START:
            // state is not propagated to the client
            return EXPORT_NEW;
        case IMPORT_NEW:
            return IMPORT_NEW;
        case IMPORT_OK:
            return IMPORT_OK;
        }
        return null;
    }

}
