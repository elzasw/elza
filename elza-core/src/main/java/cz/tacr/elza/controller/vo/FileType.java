package cz.tacr.elza.controller.vo;


public enum FileType {

    KML("KML"),

    GML("GML"),

    WKT("WKT");

    private String value;

    FileType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FileType fromValue(String v) {
        return valueOf(v);
    }
}
