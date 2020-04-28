package cz.tacr.elza.controller.vo;

public enum RequiredType {

    REQUIRED,

    POSSIBLE;

    public String value() {
        return name();
    }

    public static RequiredType fromValue(String v) {
        return valueOf(v);
    }
}
