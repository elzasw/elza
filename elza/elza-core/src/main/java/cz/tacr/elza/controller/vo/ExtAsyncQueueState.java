package cz.tacr.elza.controller.vo;


public enum ExtAsyncQueueState {

    NEW("Nový"),

    RUNNING("Zpracovávaný"),

    OK("Zpracovaný OK"),

    ERROR("Zpracovaný chyba");

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
