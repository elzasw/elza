package cz.tacr.elza.controller.vo;


public enum ExtAsyncQueueState {

    EXPORT_NEW("Nový v ELZA"),

    IMPORT_NEW("Nový v CAM"),

    UPDATE("Aktualizace"),

    ERROR("Chyba"),

    OK("Odesláno");

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
