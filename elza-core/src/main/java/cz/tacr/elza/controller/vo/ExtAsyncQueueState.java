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


}
